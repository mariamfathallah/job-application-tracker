"""Exporter Prometheus des 4 métriques DORA, calculées depuis Jenkins et Git.

Source des données
  - Jenkins : la liste des builds des branches qui déploient (job Multibranch),
    le statut et l'heure de fin du stage de production (API wfapi du plugin
    pipeline-stage-view) et les commits livrés par chaque build (changeSets).
  - Git (API GitHub) : l'heure du commit déployé quand Jenkins ne fournit pas
    de changeSet (premier build d'une branche).

Plusieurs branches peuvent être suivies (DEPLOY_BRANCHES) : main déploie
aujourd'hui, feature/ci-jenkins déployait avant, son historique reste compté.

Les builds terminés sont mémorisés dans un fichier JSON (volume Docker) :
Jenkins ne garde que les 20 derniers builds, l'historique DORA est conservé
sur toute la durée du projet.

Définitions retenues
  - Déploiement      : le stage de production d'un build d'une branche qui déploie
                       se termine en SUCCESS (réussi) ou FAILED/UNSTABLE (échoué).
                       Validation refusée ou stage non exécuté : pas un déploiement.
  - Fréquence        : déploiements réussis par jour.
  - Délai de livraison : fin du déploiement réussi - heure de chaque commit livré.
  - Taux d'échec     : déploiements échoués / déploiements (le retour arrière
                       automatique du blue/green fait échouer le stage, il compte donc).
  - Temps de rétablissement : premier déploiement échoué d'une série -> déploiement
                       réussi suivant.

Uniquement la bibliothèque standard Python : pas de dépendance à installer.
"""

import base64
import json
import logging
import os
import statistics
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

LOG = logging.getLogger("dora-exporter")

DAY = 86400
WINDOWS = {"7d": 7 * DAY, "30d": 30 * DAY, "all": None}


# --------------------------------------------------------------------------- config

class Config:
    def __init__(self, env=os.environ):
        self.jenkins_url = env.get("JENKINS_URL", "http://jenkins:8080").rstrip("/")
        self.jenkins_user = env.get("JENKINS_USER", "admin")
        self.jenkins_password = env.get("JENKINS_PASSWORD", "")
        self.job = env.get("JENKINS_JOB", "job-application-tracker")
        # branches qui déploient en production, séparées par des virgules
        self.branches = [b.strip() for b in env.get("DEPLOY_BRANCHES", "main").split(",") if b.strip()]
        self.prod_stage = env.get("PROD_STAGE", "Deploy production")
        self.github_repo = env.get("GITHUB_REPO", "mariamfathallah/job-application-tracker")
        self.github_token = env.get("GITHUB_TOKEN", "")
        self.github_api = env.get("GITHUB_API", "https://api.github.com").rstrip("/")
        self.state_file = env.get("STATE_FILE", "/data/dora-state.json")
        self.poll_seconds = int(env.get("POLL_SECONDS", "60"))
        self.port = int(env.get("PORT", "9101"))

    def branch_url(self, branch):
        # Multibranch : le nom du job de branche est encodé une fois (feature%2Fci-jenkins),
        # puis encodé à nouveau dans l'URL (feature%252Fci-jenkins).
        job = urllib.parse.quote(self.job, safe="")
        branch = urllib.parse.quote(urllib.parse.quote(branch, safe=""), safe="")
        return f"{self.jenkins_url}/job/{job}/job/{branch}"


# --------------------------------------------------------------------------- HTTP

def get_json(url, headers=None, timeout=20):
    req = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


class Jenkins:
    def __init__(self, cfg):
        self.cfg = cfg
        token = base64.b64encode(f"{cfg.jenkins_user}:{cfg.jenkins_password}".encode()).decode()
        self.headers = {"Authorization": f"Basic {token}"}

    def build_numbers(self, branch):
        """Numéros des builds d'une branche ; liste vide si Jenkins n'a pas (ou plus) ce job."""
        try:
            data = get_json(f"{self.cfg.branch_url(branch)}/api/json?tree=builds[number]", self.headers)
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                return []
            raise
        return sorted(b["number"] for b in data.get("builds", []))

    def build(self, branch, number):
        """Renvoie le résumé d'un build terminé, ou None s'il tourne encore."""
        base = f"{self.cfg.branch_url(branch)}/{number}"
        tree = ("number,result,building,timestamp,"
                "changeSets[items[commitId,timestamp]],"
                "actions[lastBuiltRevision[SHA1]]")
        info = get_json(f"{base}/api/json?tree={urllib.parse.quote(tree, safe='[],')}", self.headers)
        if info.get("building"):
            return None
        stages = get_json(f"{base}/wfapi/describe", self.headers).get("stages", [])
        summary = summarize_build(info, stages, self.cfg.prod_stage)
        summary["branch"] = branch
        return summary


class GitHub:
    def __init__(self, cfg):
        self.cfg = cfg
        self.headers = {"Accept": "application/vnd.github+json"}
        if cfg.github_token:
            self.headers["Authorization"] = f"Bearer {cfg.github_token}"

    def commit_time(self, sha):
        url = f"{self.cfg.github_api}/repos/{self.cfg.github_repo}/commits/{sha}"
        data = get_json(url, self.headers)
        return parse_iso(data["commit"]["committer"]["date"])


def parse_iso(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()


# --------------------------------------------------------------------------- calculs

def summarize_build(info, stages, prod_stage):
    """Réduit les réponses Jenkins à ce dont DORA a besoin."""
    deploy = None
    for stage in stages:
        if stage.get("name") != prod_stage:
            continue
        status = stage.get("status")
        end = (stage.get("startTimeMillis", 0) + stage.get("durationMillis", 0)) / 1000
        if status == "SUCCESS":
            deploy = {"ok": True, "time": end}
        elif status in ("FAILED", "UNSTABLE"):
            deploy = {"ok": False, "time": end}
        # ABORTED (validation refusée), NOT_EXECUTED, SKIPPED : pas un déploiement
        break

    commits = []
    for change_set in info.get("changeSets") or []:
        for item in change_set.get("items") or []:
            if item.get("commitId") and item.get("timestamp"):
                commits.append({"id": item["commitId"], "ts": item["timestamp"] / 1000})

    revision = None
    for action in info.get("actions") or []:
        rev = (action or {}).get("lastBuiltRevision")
        if rev and rev.get("SHA1"):
            revision = rev["SHA1"]
            break

    return {
        "number": info["number"],
        "result": info.get("result"),
        "start": info.get("timestamp", 0) / 1000,
        "deploy": deploy,
        "commits": commits,
        "revision": revision,
    }


def deployments(builds, commit_time):
    """Liste chronologique des déploiements de production.

    builds      : résumés de builds (summarize_build), d'une ou plusieurs branches
    commit_time : fonction sha -> timestamp (ou None), pour les builds sans changeSet
    Chaque déploiement réussi porte le délai de livraison de chacun de ses commits.
    Les numéros de build sont propres à chaque branche : on les traite branche par branche.
    """
    by_branch = {}
    for build in builds:
        by_branch.setdefault(build.get("branch", ""), []).append(build)
    events = []
    for branch_builds in by_branch.values():
        events.extend(_branch_deployments(branch_builds, commit_time))
    return sorted(events, key=lambda e: e["time"])


def _branch_deployments(builds, commit_time):
    events = []
    pending = {}            # commits construits mais pas encore livrés en production
    last_revision = None
    for build in sorted(builds, key=lambda b: b["number"]):
        for commit in build["commits"]:
            pending.setdefault(commit["id"], commit["ts"])
        deploy = build["deploy"]
        if not deploy:
            continue
        event = {"build": build["number"], "time": deploy["time"], "ok": deploy["ok"], "lead_times": []}
        if deploy["ok"]:
            if pending:
                event["lead_times"] = [max(0.0, deploy["time"] - ts) for ts in pending.values()]
            elif build["revision"] and build["revision"] != last_revision:
                ts = commit_time(build["revision"])
                if ts is not None:
                    event["lead_times"] = [max(0.0, deploy["time"] - ts)]
            pending = {}
            last_revision = build["revision"]
        events.append(event)
    return events


def restorations(events):
    """(heure du rétablissement, durée) pour chaque série d'échecs suivie d'un succès."""
    result, failing_since = [], None
    for event in events:
        if not event["ok"]:
            failing_since = failing_since if failing_since is not None else event["time"]
        elif failing_since is not None:
            result.append((event["time"], event["time"] - failing_since))
            failing_since = None
    return result, failing_since


def compute(events, now):
    """Métriques DORA par fenêtre de temps. Une valeur absente = pas de donnée."""
    restores, failing_since = restorations(events)
    first = events[0]["time"] if events else None
    metrics = {}
    for name, length in WINDOWS.items():
        since = now - length if length else (first if first is not None else now)
        days = max((now - since) / DAY, 1.0)
        window = [e for e in events if e["time"] >= since]
        ok = [e for e in window if e["ok"]]
        failed = [e for e in window if not e["ok"]]
        leads = [lt for e in ok for lt in e["lead_times"]]
        mttr = [d for (t, d) in restores if t >= since]

        values = {"deployments_success": len(ok), "deployments_failure": len(failed),
                  "deployment_frequency_per_day": len(ok) / days}
        if leads:
            values["lead_time_median_seconds"] = statistics.median(leads)
            values["lead_time_mean_seconds"] = statistics.fmean(leads)
        if window:
            values["change_failure_rate"] = len(failed) / len(window)
        if mttr:
            values["time_to_restore_median_seconds"] = statistics.median(mttr)
            values["time_to_restore_mean_seconds"] = statistics.fmean(mttr)
        metrics[name] = values

    last_ok = next((e for e in reversed(events) if e["ok"]), None)
    metrics["current"] = {
        "production_failing": 1 if failing_since is not None else 0,
        "last_deployment_timestamp_seconds": events[-1]["time"] if events else None,
        "last_successful_deployment_timestamp_seconds": last_ok["time"] if last_ok else None,
        "last_lead_time_seconds": statistics.median(last_ok["lead_times"]) if last_ok and last_ok["lead_times"] else None,
        "failing_since_timestamp_seconds": failing_since,
    }
    return metrics


# --------------------------------------------------------------------------- rendu

HELP = {
    "deployments_total": ("counter", "Déploiements de production depuis le début du suivi, par résultat."),
    "deployments": ("gauge", "Déploiements de production dans la fenêtre, par résultat."),
    "deployment_frequency_per_day": ("gauge", "Fréquence de déploiement : déploiements réussis par jour."),
    "lead_time_median_seconds": ("gauge", "Délai de livraison médian (commit -> production)."),
    "lead_time_mean_seconds": ("gauge", "Délai de livraison moyen (commit -> production)."),
    "change_failure_rate": ("gauge", "Taux d'échec des changements (0 à 1)."),
    "time_to_restore_median_seconds": ("gauge", "Temps de rétablissement médian après un échec en production."),
    "time_to_restore_mean_seconds": ("gauge", "Temps de rétablissement moyen après un échec en production."),
    "production_failing": ("gauge", "1 si le dernier déploiement de production a échoué."),
    "last_deployment_timestamp_seconds": ("gauge", "Heure du dernier déploiement de production."),
    "last_successful_deployment_timestamp_seconds": ("gauge", "Heure du dernier déploiement réussi."),
    "last_lead_time_seconds": ("gauge", "Délai de livraison (médian) du dernier déploiement réussi."),
    "failing_since_timestamp_seconds": ("gauge", "Début de l'échec en cours en production."),
    "exporter_up": ("gauge", "1 si la dernière lecture de Jenkins a réussi."),
    "exporter_last_poll_timestamp_seconds": ("gauge", "Heure de la dernière lecture réussie de Jenkins."),
    "builds_tracked": ("gauge", "Builds Jenkins mémorisés par l'exporter."),
}


def fmt(value):
    """Valeur au format Prometheus, sans perte de précision (horodatages compris)."""
    value = float(value)
    return str(int(value)) if value.is_integer() else repr(value)


def render(metrics, events, status):
    lines = []

    def emit(name, samples):
        samples = [(labels, value) for labels, value in samples if value is not None]
        if not samples:
            return
        kind, text = HELP[name]
        lines.append(f"# HELP dora_{name} {text}")
        lines.append(f"# TYPE dora_{name} {kind}")
        for labels, value in samples:
            label_text = ",".join(f'{k}="{v}"' for k, v in labels.items())
            lines.append(f"dora_{name}{{{label_text}}} {fmt(value)}" if label_text
                         else f"dora_{name} {fmt(value)}")

    total_ok = sum(1 for e in events if e["ok"])
    emit("deployments_total", [({"result": "success"}, total_ok),
                               ({"result": "failure"}, len(events) - total_ok)])
    emit("deployments", [({"window": w, "result": r}, metrics[w][f"deployments_{r}"])
                         for w in WINDOWS for r in ("success", "failure")])
    for name in ("deployment_frequency_per_day", "lead_time_median_seconds", "lead_time_mean_seconds",
                 "change_failure_rate", "time_to_restore_median_seconds", "time_to_restore_mean_seconds"):
        emit(name, [({"window": w}, metrics[w].get(name)) for w in WINDOWS])
    for name, value in metrics["current"].items():
        emit(name, [({}, value)])
    emit("exporter_up", [({}, status["up"])])
    emit("exporter_last_poll_timestamp_seconds", [({}, status["last_poll"])])
    emit("builds_tracked", [({}, status["builds"])])
    return "\n".join(lines) + "\n"


# --------------------------------------------------------------------------- état

class State:
    """Builds terminés et heures de commits, conservés entre deux redémarrages."""

    def __init__(self, path):
        self.path = path
        self.builds, self.commit_times = {}, {}   # builds : "branche#numéro" -> résumé
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
            # un résumé sans branche vient d'une ancienne version : Jenkins le redonnera
            self.builds = {k: v for k, v in data.get("builds", {}).items() if "branch" in v}
            self.commit_times = data.get("commit_times", {})
        except FileNotFoundError:
            pass
        except (OSError, ValueError) as exc:
            LOG.warning("état illisible (%s), on repart de zéro", exc)

    def save(self):
        directory = os.path.dirname(self.path)
        if directory:
            os.makedirs(directory, exist_ok=True)
        tmp = f"{self.path}.tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump({"builds": self.builds, "commit_times": self.commit_times}, f)
        os.replace(tmp, self.path)


class Exporter:
    def __init__(self, cfg, jenkins=None, github=None):
        self.cfg = cfg
        self.jenkins = jenkins or Jenkins(cfg)
        self.github = github or GitHub(cfg)
        self.state = State(cfg.state_file)
        self.status = {"up": 0, "last_poll": None, "builds": len(self.state.builds)}
        self.lock = threading.Lock()
        self.text = render(compute([], time.time()), [], self.status)

    def commit_time(self, sha):
        if sha not in self.state.commit_times:
            try:
                self.state.commit_times[sha] = self.github.commit_time(sha)
            except (urllib.error.URLError, KeyError, ValueError, OSError) as exc:
                LOG.warning("heure du commit %s indisponible : %s", sha[:8], exc)
                return None
        return self.state.commit_times[sha]

    def poll(self):
        try:
            changed = False
            for branch in self.cfg.branches:
                for number in self.jenkins.build_numbers(branch):
                    key = f"{branch}#{number}"
                    if key in self.state.builds:
                        continue
                    summary = self.jenkins.build(branch, number)
                    if summary is not None:
                        self.state.builds[key] = summary
                        changed = True
            events = deployments(self.state.builds.values(), self.commit_time)
            if changed:
                self.state.save()
            self.status.update(up=1, last_poll=time.time(), builds=len(self.state.builds))
        except (urllib.error.URLError, ValueError, KeyError, OSError) as exc:
            LOG.error("lecture de Jenkins impossible : %s", exc)
            self.status["up"] = 0
            events = deployments(self.state.builds.values(), lambda sha: self.state.commit_times.get(sha))
        text = render(compute(events, time.time()), events, self.status)
        with self.lock:
            self.text = text

    def metrics(self):
        with self.lock:
            return self.text


def serve(exporter, port):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path.split("?")[0] == "/metrics":
                body, ctype = exporter.metrics().encode(), "text/plain; version=0.0.4; charset=utf-8"
            else:
                body, ctype = b"DORA exporter - voir /metrics\n", "text/plain; charset=utf-8"
            self.send_response(200)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, *args):
            pass

    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    cfg = Config()
    exporter = Exporter(cfg)
    serve(exporter, cfg.port)
    LOG.info("exporter DORA sur :%s, branches %s, stage '%s'", cfg.port, ", ".join(cfg.branches), cfg.prod_stage)
    while True:
        exporter.poll()
        time.sleep(cfg.poll_seconds)


if __name__ == "__main__":
    main()
