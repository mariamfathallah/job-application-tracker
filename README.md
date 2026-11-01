# Plateforme CI/CD (Jenkins, SonarQube, Nexus, Prometheus, Grafana)

Projet du cours **CI/CD avec Jenkins** (ESGI) : une chaîne d'intégration et de livraison continues complète autour d'une application existante (API Spring Boot + frontend React + PostgreSQL).

> L'application elle-même (fonctionnalités, API, lancement en local) est décrite dans [APP_README.md](APP_README.md).

**Équipe :** Basma MORABIT · Bastien LIEVRE · Siham AIDROUS · Mariam FATHALLAH

**Rendu :** [Rapport technique](<lien>) · [Vidéo de démonstration](<lien>)

---
## Tout relancer

### Prérequis

- **Docker Desktop** démarré, avec **au moins 8 Go de mémoire** pour Docker (Windows/WSL2 : `memory=8GB` dans `%UserProfile%\.wslconfig`). La plateforme seule utilise environ 7 Go.
- **Git** et un shell POSIX (Git Bash sous Windows). Sous Git Bash, faire `export MSYS_NO_PATHCONV=1` avant les commandes `docker exec … /chemin`.
- **Ports libres** : 8090, 9000, 8081, 9090, 3000, 8082, 8083.
- Un **compte GitHub** (pour le token de lecture du dépôt).

### Variables (`ci/.env`)

`ci/.env` est créé à partir de `ci/.env.example` et **n'est jamais commité** (ignoré par Git).

| Variable | Obligatoire | Rôle / où l'obtenir |
|---|---|---|
| `JENKINS_ADMIN_USER` | oui | laisser `admin` : c'est le seul utilisateur autorisé à valider la production |
| `JENKINS_ADMIN_PASSWORD` | oui | au choix |
| `GITHUB_USER`, `GITHUB_TOKEN` | oui | GitHub > Settings > Developer settings > *Fine-grained token*, accès **Public repositories (read-only)**. Sans token, le scan du dépôt échoue |
| `NEXUS_USER`, `NEXUS_PASSWORD` | oui | `admin` + le mot de passe choisi à l'étape 2 |
| `SONAR_TOKEN` | oui | généré à l'étape 3 |
| `SONAR_ADMIN_PASSWORD` | oui | mot de passe admin choisi à l'étape 3 |
| `GRAFANA_ADMIN_PASSWORD` | oui | au choix (le conteneur Grafana refuse de démarrer sans) |
| `DISCORD_WEBHOOK_URL` | non | notifications de fin de build ; vide = pas de notification |
| `SMEE_URL` | non | relais du webhook GitHub (étape 6) ; sans lui, Jenkins scanne le dépôt toutes les heures |
| `RELEASES_ENABLED` | non | laisser `false` : seul le Jenkins de référence publie les tags et releases GitHub |

### Première mise en route (une fois)

```bash
git clone https://github.com/mariamfathallah/job-application-tracker.git
cd job-application-tracker/ci
```
Toutes les commandes suivantes se lancent **depuis le dossier `ci/`**, sauf mention contraire.

1. **Démarrer la plateforme**
   ```bash
   cp .env.example .env     # remplir JENKINS_ADMIN_PASSWORD, GITHUB_USER, GITHUB_TOKEN, GRAFANA_ADMIN_PASSWORD
   docker compose up -d --build
   docker compose ps        # tous les services doivent être "running"
   ```
   Nexus et SonarQube mettent 2 à 3 minutes à démarrer (page vide ou `ERR_EMPTY_RESPONSE` au début : normal).
2. **Nexus** (http://localhost:8081) : mot de passe initial : `docker exec ci-nexus cat /nexus-data/admin.password`. Se connecter (`admin`), accepter la licence dans l'assistant, changer le mot de passe et le mettre dans `NEXUS_PASSWORD` (.env). Créer le dépôt des artefacts :
   ```bash
   NEXUS_PASSWORD='<mot de passe>' sh scripts/bootstrap-nexus.sh
   ```
3. **SonarQube** (http://localhost:9000, `admin`/`admin`) : changer le mot de passe (`SONAR_ADMIN_PASSWORD` dans .env), puis My Account > Security > générer un token (`SONAR_TOKEN` dans .env). Créer le webhook SonarQube → Jenkins, indispensable à la Quality Gate :
   ```bash
   SONAR_ADMIN_PASSWORD='<mot de passe>' sh scripts/bootstrap-sonar.sh
   ```
4. **Recharger les secrets** : `docker compose up -d`. Jenkins relit `.env` (JCasC) ; un simple `docker restart` ne suffit pas.
5. **Premier build** : Jenkins (http://localhost:8090) > *Manage Jenkins* > *Nodes* : `agent-1` doit être en ligne. Ouvrir le job `job-application-tracker` > **Scan Repository Now** : les branches et les PR sont découvertes, le build de `main` démarre. Il va jusqu'à *Deploy production*, qui attend une validation (**Proceed**, en tant qu'`admin`).
6. **Webhook GitHub** (optionnel, nécessite d'être administrateur du dépôt) : créer un channel sur https://smee.io, mettre l'URL dans `SMEE_URL` (.env), `docker compose up -d`, puis sur GitHub (Settings > Webhooks) : Payload URL = l'URL smee, `application/json`, événements *Pushes* et *Pull requests*. Le conteneur `ci-smee` relaie les événements vers Jenkins : un push lance le build immédiatement. Sans webhook, le dépôt est scanné toutes les heures.

### Vérifications

| Quoi | Comment | Attendu |
|---|---|---|
| Build | Jenkins > `job-application-tracker` > `main` | toutes les étapes vertes |
| Artefact | Nexus > Browse > `artifacts` | `job-application-tracker/<version>-<hash>/…jar` |
| Staging / production | http://localhost:8082/actuator/health, http://localhost:8083/actuator/health | `{"status":"UP"}` |
| Couleur active (blue/green) | `docker exec tracker-production-proxy cat /etc/nginx/active_color` | `blue` ou `green` |
| Métriques | http://localhost:9090/targets | toutes les cibles `UP` (staging et production après leur premier déploiement) |
| Tableaux de bord | Grafana > Dashboards > *CI/CD* | vue d'ensemble et DORA |

---

## Architecture

```
git push / PR ─► GitHub ─► webhook (smee) ─► Jenkins (controller, 0 executor)
                                                │
                                                ▼
                     agent-1 (JDK 21, Node 22, Docker, OpenTofu, Ansible)
   commit lint (PR) ─► lint + tests (parallèle) ─► SonarQube (Quality Gate bloquante) ∥ tests d'intégration
   ─► release (semantic-release : tag vX.Y.Z) ─► jar X.Y.Z-<hash> ─► Nexus
   ─► plan OpenTofu ─► staging (auto) ─► validation manuelle (admin) ─► production
       (chaque environnement : PostgreSQL + proxy nginx + application blue/green)

Prometheus ◄── Jenkins, staging, production, exporter DORA ──► Grafana (tableaux de bord)
```

Toute la plateforme tient dans un seul fichier : [`ci/docker-compose.yml`](ci/docker-compose.yml).
Jenkins est configuré entièrement par code ([`ci/jenkins/casc.yaml`](ci/jenkins/casc.yaml)) : job Multibranch, agent, credentials, Shared Library.

| Service    | URL                    |
|------------|------------------------|
| Jenkins    | http://localhost:8090 (`admin` / `JENKINS_ADMIN_PASSWORD`) |
| SonarQube  | http://localhost:9000 (admin/admin, à changer) |
| Nexus      | http://localhost:8081  |
| Staging    | http://localhost:8082  |
| Production | http://localhost:8083  |
| Prometheus | http://localhost:9090 (cibles : /targets) |
| Grafana    | http://localhost:3000 (admin / `GRAFANA_ADMIN_PASSWORD`) |

## Le pipeline ([`Jenkinsfile`](Jenkinsfile))

| Étape | Quand | Rôle |
|---|---|---|
| Commit lint | pull request | messages au format Conventional Commits (commitlint) |
| Lint & tests | toujours | backend `mvnw verify` (JUnit, couverture JaCoCo) ∥ frontend (ESLint, Vitest, build) |
| Analyse & intégration | toujours | SonarQube + Quality Gate bloquante ∥ tests d'intégration avec Docker Compose |
| Release | `main` | semantic-release : version, tag `vX.Y.Z`, CHANGELOG, release GitHub |
| Package & publish | toujours | jar `X.Y.Z-<hash du commit>` publié dans Nexus |
| Infra plan | toujours | plan OpenTofu de staging et production |
| Publish plan on PR | pull request | plans publiés en commentaire de la PR |
| Deploy staging | `main` | `tofu apply` puis déploiement blue/green (Ansible) |
| Deploy production | `main` | validation manuelle (`input`, submitter `admin`), puis même déploiement |

Les étapes communes (publication Nexus, déploiement, notification Discord) viennent de la **Shared Library** [`jenkins-shared-library`](https://github.com/l3miage-morabitb/jenkins-shared-library), chargée par tag (`@Library('jenkins-shared-library@v1.2.0')`).
Les secrets sont des credentials Jenkins créés depuis `ci/.env` (jamais dans Git).

## Modules réalisés

| Module | Niveau | Documentation |
|---|--------|---|
| Qualité et retour rapide | 1      | [`Jenkinsfile`](Jenkinsfile) |
| Releases automatisées (semantic-release) | 1      | [`.releaserc.json`](.releaserc.json), [`commitlint.config.cjs`](commitlint.config.cjs) |
| Blue/green | 2      | [`infra/README.md`](infra/README.md#déploiement-bluegreen-ansible) |
| Environnements as Code (OpenTofu + Ansible) | 3      | [`infra/README.md`](infra/README.md) |
| Observabilité & DORA (Prometheus, Grafana) | 3      | [section DORA](#observabilité-et-métriques-dora), [`ci/dora-exporter/`](ci/dora-exporter/) |

## Branches

| Branche | Rôle |
|---|---|
| `main` | branche du projet : chaque merge est publié (release), déployé en staging puis en production après validation |
| `module/<nom>`, `fix/…`, `docs/…` | travail en cours → pull request vers `main` (build Jenkins vert + 1 approbation, squash merge) |
| `live` | site public de l'application (Render + Vercel), indépendant de la chaîne Jenkins |

---

## Problèmes fréquents

| Symptôme | Cause | Solution |
|---|---|---|
| Le scan du dépôt échoue | `GITHUB_TOKEN` absent ou invalide | corriger `.env`, puis `docker compose up -d` |
| Une modification de `.env` n'est pas prise en compte | le conteneur garde ses anciennes variables | `docker compose up -d` (pas `docker restart`) |
| `waitForQualityGate` attend indéfiniment | webhook SonarQube absent | relancer `scripts/bootstrap-sonar.sh` |
| `agent-1` hors ligne | moins de 1 Go de disque libre, ou agent juste reconstruit | supprimer les anciens workspaces (`docker exec ci-agent sh -c 'rm -rf /opt/jenkins-agent/workspace/*'`), attendre une minute |
| Docker ne répond plus, tests qui expirent | mémoire insuffisante | fermer les applications lourdes, augmenter `memory` dans `.wslconfig`, `wsl --shutdown` puis relancer Docker Desktop |
| `jobtracker-postgres` démarre au lieu de la plateforme | `docker compose` lancé à la racine du dépôt | `docker stop jobtracker-postgres`, relancer depuis `ci/` |

## Commandes utiles

```bash
docker compose ps                    # état des services
docker compose logs -f jenkins       # journaux d'un service
docker compose up -d --build jenkins # après une modification de casc.yaml ou plugins.txt
docker compose down                  # arrêt (les volumes, donc les données, sont conservés)
```

## Observabilité et métriques DORA
- **Prometheus** collecte les métriques de Jenkins (plugin `prometheus`, `/prometheus/`), de l'application en staging et en production (`/actuator/prometheus`) et de l'exporter DORA. Historique conservé 180 jours.
- **Grafana** est provisionné automatiquement (source Prometheus + tableaux de bord de `ci/grafana/dashboards/`, dossier *CI/CD*) :
  - *Job Application Tracker - Vue d'ensemble* : santé des cibles, requêtes, erreurs, mémoire, durée des builds ;
  - *DORA - Job Application Tracker* : les 4 métriques DORA, par période (7 jours, 30 jours, tout le projet).
- **`ci/dora-exporter/`** calcule les métriques DORA depuis Jenkins et Git, sans saisie manuelle :

| Métrique | Calcul |
|---|---|
| Fréquence de déploiement | stages *Deploy production* réussis par jour (branche `main`, plus l'historique de `feature/ci-jenkins` qui déployait avant) |
| Délai de livraison | fin du déploiement réussi - heure de chaque commit livré (changeSets Jenkins, ou API GitHub pour le premier build) |
| Taux d'échec | stages *Deploy production* en échec (dont retour arrière blue/green) / déploiements |
| Temps de rétablissement | premier échec en production -> déploiement réussi suivant |

Une validation refusée n'est pas un déploiement. Jenkins ne garde que 20 builds : l'exporter mémorise chaque build terminé dans le volume `dora_data`, l'historique couvre donc toute la durée du projet. Les vraies valeurs sont celles du Jenkins de référence.

Tests de l'exporter (depuis la racine du dépôt) : `python -m unittest discover -s ci/dora-exporter`

---

## Documentation

- [`infra/README.md`](infra/README.md) : environnements as code, blue/green
- [`docs/EQUIPE.md`](docs/EQUIPE.md) : organisation de l'équipe, workflow Git
- [`APP_README.md`](APP_README.md) : l'application