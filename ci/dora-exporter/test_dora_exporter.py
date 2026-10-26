"""Tests de l'exporter DORA : python -m unittest discover -s ci/dora-exporter"""

import json
import os
import tempfile
import threading
import unittest
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import dora_exporter as d

H = 3600
T0 = 1_760_000_000  # instant de référence arbitraire


def build(number, deploy=None, commits=(), revision=None):
    return {"number": number, "result": "SUCCESS", "start": T0, "deploy": deploy,
            "commits": [{"id": c, "ts": ts} for c, ts in commits], "revision": revision}


class SummarizeTest(unittest.TestCase):
    def stage(self, status, start=T0 * 1000, duration=60_000):
        return [{"name": "Build", "status": "SUCCESS"},
                {"name": "Deploy production", "status": status,
                 "startTimeMillis": start, "durationMillis": duration}]

    def info(self, **extra):
        base = {"number": 7, "result": "SUCCESS", "timestamp": T0 * 1000,
                "changeSets": [{"items": [{"commitId": "abc", "timestamp": (T0 - H) * 1000}]}],
                "actions": [{}, {"lastBuiltRevision": {"SHA1": "abc"}}, None]}
        base.update(extra)
        return base

    def test_success_uses_stage_end(self):
        s = d.summarize_build(self.info(), self.stage("SUCCESS"), "Deploy production")
        self.assertEqual(s["deploy"], {"ok": True, "time": T0 + 60})
        self.assertEqual(s["commits"], [{"id": "abc", "ts": T0 - H}])
        self.assertEqual(s["revision"], "abc")

    def test_failed_stage_is_a_failed_deployment(self):
        s = d.summarize_build(self.info(), self.stage("FAILED"), "Deploy production")
        self.assertFalse(s["deploy"]["ok"])

    def test_refused_approval_is_not_a_deployment(self):
        for status in ("ABORTED", "NOT_EXECUTED"):
            s = d.summarize_build(self.info(), self.stage(status), "Deploy production")
            self.assertIsNone(s["deploy"], status)

    def test_branch_without_production_stage(self):
        s = d.summarize_build(self.info(changeSets=[]), [{"name": "Build", "status": "SUCCESS"}], "Deploy production")
        self.assertIsNone(s["deploy"])
        self.assertEqual(s["commits"], [])


class DeploymentsTest(unittest.TestCase):
    def test_commits_wait_for_next_successful_deployment(self):
        builds = [
            build(1, commits=[("a", T0)]),                                    # validation refusée
            build(2, {"ok": False, "time": T0 + 2 * H}, [("b", T0 + H)]),     # échec en prod
            build(3, {"ok": True, "time": T0 + 4 * H}, [("c", T0 + 3 * H)]),  # livre a, b, c
        ]
        events = d.deployments(builds, lambda sha: None)
        self.assertEqual([e["ok"] for e in events], [False, True])
        self.assertEqual(sorted(events[1]["lead_times"]), [H, 3 * H, 4 * H])

    def test_first_build_falls_back_on_git_commit_time(self):
        events = d.deployments([build(1, {"ok": True, "time": T0 + H}, revision="sha1")],
                               lambda sha: T0 if sha == "sha1" else None)
        self.assertEqual(events[0]["lead_times"], [H])

    def test_same_build_numbers_on_two_branches(self):
        old = dict(build(1, {"ok": True, "time": T0 + H}, [("a", T0)]), branch="feature/ci-jenkins")
        new = dict(build(1, {"ok": True, "time": T0 + 3 * H}, [("b", T0 + 2 * H)]), branch="main")
        events = d.deployments([new, old], lambda sha: None)
        self.assertEqual([e["time"] for e in events], [T0 + H, T0 + 3 * H])
        self.assertEqual([e["lead_times"] for e in events], [[H], [H]])

    def test_redeploying_same_commit_adds_no_lead_time(self):
        builds = [build(1, {"ok": True, "time": T0 + H}, [("a", T0)], "sha1"),
                  build(2, {"ok": True, "time": T0 + 5 * H}, revision="sha1")]
        events = d.deployments(builds, lambda sha: T0)
        self.assertEqual(events[1]["lead_times"], [])


class ComputeTest(unittest.TestCase):
    def events(self):
        return [
            {"time": T0, "ok": True, "lead_times": [2 * H]},
            {"time": T0 + 1 * H, "ok": False, "lead_times": []},
            {"time": T0 + 2 * H, "ok": False, "lead_times": []},
            {"time": T0 + 4 * H, "ok": True, "lead_times": [H, 3 * H]},
        ]

    def test_four_metrics(self):
        m = d.compute(self.events(), now=T0 + 5 * H)
        w = m["7d"]
        self.assertAlmostEqual(w["deployment_frequency_per_day"], 2 / 7)
        self.assertEqual(w["lead_time_median_seconds"], 2 * H)
        self.assertEqual(w["change_failure_rate"], 0.5)
        self.assertEqual(w["time_to_restore_median_seconds"], 3 * H)  # depuis le 1er échec de la série
        self.assertEqual(m["current"]["production_failing"], 0)

    def test_old_events_leave_the_window(self):
        m = d.compute(self.events(), now=T0 + 10 * d.DAY)
        self.assertEqual(m["7d"]["deployments_success"], 0)
        self.assertNotIn("change_failure_rate", m["7d"])
        self.assertEqual(m["all"]["deployments_success"], 2)

    def test_ongoing_failure(self):
        m = d.compute(self.events()[:2], now=T0 + 2 * H)
        self.assertEqual(m["current"]["production_failing"], 1)
        self.assertEqual(m["current"]["failing_since_timestamp_seconds"], T0 + H)

    def test_timestamps_keep_full_precision(self):
        text = d.render(d.compute([{"time": T0 + 0.5, "ok": True, "lead_times": [60]}], T0 + H), [],
                        {"up": 1, "last_poll": T0 + 1, "builds": 1})
        self.assertIn(f"dora_last_deployment_timestamp_seconds {T0}.5", text)
        self.assertIn(f"dora_exporter_last_poll_timestamp_seconds {T0 + 1}", text)

    def test_no_data_renders_without_crashing(self):
        text = d.render(d.compute([], T0), [], {"up": 0, "last_poll": None, "builds": 0})
        self.assertIn('dora_deployments_total{result="success"} 0', text)
        self.assertNotIn("lead_time", text)


def fake_build(n, status, end, commits=(), sha=None, building=False, result="SUCCESS"):
    info = {"number": n, "result": result, "building": building, "timestamp": (end - H) * 1000 if end else 0,
            "changeSets": [{"items": [{"commitId": c, "timestamp": ts * 1000} for c, ts in commits]}] if commits else [],
            "actions": [{"lastBuiltRevision": {"SHA1": sha}}] if sha else []}
    return info, status, end * 1000 if end else None


class FakeJenkins(BaseHTTPRequestHandler):
    """Répond comme Jenkins : feature/ci-jenkins (ancienne branche qui déployait) puis main.
    Les deux branches ont des builds numérotés 1 et 2 : l'exporter ne doit pas les confondre."""
    branches = {
        "/job/job-application-tracker/job/feature%252Fci-jenkins": {
            1: fake_build(1, "SUCCESS", T0 + H, sha="sha1"),                                   # 1er build : heure via Git
            2: fake_build(2, "FAILED", T0 + 3 * H, [("sha2", T0 + 2 * H)], result="FAILURE"),
            3: fake_build(3, "SUCCESS", T0 + 5 * H, [("sha3", T0 + 4 * H)]),                  # livre sha2 et sha3
            4: fake_build(4, None, None, building=True),                                      # en cours
        },
        "/job/job-application-tracker/job/main": {
            1: fake_build(1, "SUCCESS", T0 + 7 * H, [("sha4", T0 + 6 * H)]),
            2: fake_build(2, "SUCCESS", T0 + 9 * H, [("sha5", T0 + 8 * H)]),
        },
    }

    def do_GET(self):
        path = self.path.split("?")[0]
        if path.startswith("/job/") and self.headers.get("Authorization") != "Basic YWRtaW46c2VjcmV0":
            return self.reply(401, {})  # Jenkins exige admin:secret
        for prefix, builds in self.branches.items():
            if path == f"{prefix}/api/json":
                return self.reply(200, {"builds": [{"number": n} for n in builds]})
            for n, (info, status, end) in builds.items():
                if path == f"{prefix}/{n}/api/json":
                    return self.reply(200, info)
                if path == f"{prefix}/{n}/wfapi/describe":
                    return self.reply(200, {"stages": [{"name": "Deploy production", "status": status,
                                                        "startTimeMillis": end - 60_000, "durationMillis": 60_000}]})
        if path == "/repos/mariamfathallah/job-application-tracker/commits/sha1":
            return self.reply(200, {"commit": {"committer": {"date": "2025-10-09T08:53:20Z"}}})
        return self.reply(404, {})

    def reply(self, code, body):
        data = json.dumps(body).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


class EndToEndTest(unittest.TestCase):
    def test_poll_against_fake_jenkins(self):
        server = ThreadingHTTPServer(("127.0.0.1", 0), FakeJenkins)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        url = f"http://127.0.0.1:{server.server_port}"
        with tempfile.TemporaryDirectory() as tmp:
            env = {"JENKINS_URL": url, "JENKINS_USER": "admin", "JENKINS_PASSWORD": "secret",
                   "GITHUB_API": url, "STATE_FILE": os.path.join(tmp, "state.json"), "PORT": "0",
                   "DEPLOY_BRANCHES": "main, feature/ci-jenkins, branche-supprimee"}
            exporter = d.Exporter(d.Config(env))
            exporter.poll()
            text = exporter.metrics()
            self.assertIn("dora_exporter_up 1", text)
            # 4 réussis (2 par branche), 1 échec ; une branche absente de Jenkins (404) est ignorée
            self.assertIn('dora_deployments_total{result="success"} 4', text)
            self.assertIn('dora_deployments_total{result="failure"} 1', text)
            self.assertIn('dora_change_failure_rate{window="all"} 0.2', text)
            self.assertIn('dora_time_to_restore_median_seconds{window="all"} 7200', text)
            # délais : 1 h (via Git), 3 h et 1 h (build 3), 1 h et 1 h (main) -> médiane 1 h
            self.assertIn('dora_lead_time_median_seconds{window="all"} 3600', text)
            self.assertEqual(exporter.status["builds"], 5)           # le build 4 tourne encore

            # l'historique survit à un redémarrage, même si Jenkins est injoignable
            env["JENKINS_URL"] = "http://127.0.0.1:1"
            restarted = d.Exporter(d.Config(env))
            restarted.poll()
            self.assertIn("dora_exporter_up 0", restarted.metrics())
            self.assertIn('dora_deployments_total{result="success"} 4', restarted.metrics())

            # le serveur HTTP sert bien /metrics
            http = d.serve(exporter, 0)
            body = urllib.request.urlopen(f"http://127.0.0.1:{http.server_port}/metrics").read().decode()
            self.assertIn("# TYPE dora_deployments_total counter", body)
            http.shutdown()
            http.server_close()
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    unittest.main()
