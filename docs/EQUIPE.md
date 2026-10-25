# Guide d'équipe — Projet CI/CD Jenkins

Ce document explique **comment travailler à 4** sur le projet et **qui fait quoi**.
Exigence du sujet (quadrinôme) : partie commune + **5 modules**, dont les **2 de niveau 3**.

---

## 1. État d'avancement (point de départ)

| Élément | État |
|---|---|
| Stack Docker `ci/` (Jenkins, agent, SonarQube, Nexus) | Fait |
| `Jenkinsfile` : tests, Sonar + Quality Gate, artefact Nexus, staging auto, prod après validation | Fait et vérifié de bout en bout |
| Tests d'intégration Docker Compose | Fait (PR #3) |
| Shared Library versionnée par tag | Fait (PR #4, `notifyDiscord` ; étapes communes à migrer) |
| Webhook GitHub + statut du build sur les PR | Fait (source GitHub, smee, check obligatoire sur les PR) |
| 5 modules, rapport technique, soutenance | À faire |

## 2. Architecture en bref

```
git push ─► webhook (smee) ─► Jenkins (controller, 0 executor) ─► agent-1 (JDK 21, Node 22, Docker)
   commit lint (PR) ─► lint + tests (parallèle) ─► SonarQube (Quality Gate bloquante) ─► tests d'intégration
   ─► release (semantic-release : tag vX.Y.Z) ─► jar X.Y.Z-<hash> ─► Nexus (dépôt raw "artifacts")
   ─► staging (auto, :8082) ─► validation manuelle (input, submitter admin) ─► production (:8083)
       (chaque environnement : proxy nginx + blue/green)
```

| Service | URL locale | Rôle |
|---|---|---|
| Jenkins | http://localhost:8090 | orchestration |
| SonarQube | http://localhost:9000 | qualité de code |
| Nexus | http://localhost:8081 | stockage des artefacts |
| Staging / Production | :8082 / :8083 | proxy nginx devant l'appli blue/green (+ PostgreSQL par environnement) |

Fichiers clés :
- `Jenkinsfile` : le pipeline. Variable `DEPLOY_BRANCH` = branche qui déploie.
- `ci/docker-compose.yml`, `ci/jenkins/casc.yaml` (config Jenkins as Code), `ci/jenkins/plugins.txt`
- `ci/scripts/deploy.sh` (déploie une version via Ansible), `infra.sh` (OpenTofu), `bootstrap-nexus.sh`, `bootstrap-sonar.sh`
- `infra/terraform` (environnements décrits avec OpenTofu), `infra/ansible` (déploiement : `bluegreen.yml`, `simple.yml`) — voir `infra/README.md`
- `package.json`, `commitlint.config.cjs`, `.releaserc.json` : outils de release (commitlint, semantic-release)

---

## 3. Mise en route d'un membre (une seule fois)

Prérequis : Docker Desktop démarré, Git.

```bash
git clone https://github.com/mariamfathallah/job-application-tracker.git
cd job-application-tracker
git checkout feature/ci-jenkins
cd ci
cp .env.example .env        # choisir son JENKINS_ADMIN_PASSWORD
docker compose up -d --build
```

Puis, **dans cet ordre** :
1. **Nexus** (http://localhost:8081) : mot de passe initial = `docker exec ci-nexus sh -c "cat /nexus-data/admin.password"`. Se connecter (`admin`), **accepter la licence (EULA)** dans l'assistant, changer le mot de passe, le mettre dans `ci/.env` (`NEXUS_PASSWORD`). Puis : `NEXUS_PASSWORD=<mdp> sh scripts/bootstrap-nexus.sh`
2. **SonarQube** (http://localhost:9000, `admin`/`admin`) : changer le mot de passe (le noter dans `ci/.env` : `SONAR_ADMIN_PASSWORD=`), générer un token (My Account → Security) → `SONAR_TOKEN` dans `ci/.env`. Puis : `SONAR_ADMIN_PASSWORD=<mdp> sh scripts/bootstrap-sonar.sh` (webhook vers Jenkins, indispensable à la Quality Gate).
3. `docker compose up -d` (Jenkins relit les secrets).
4. **GitHub** : créer un token *fine-grained* (**Public repositories (read-only)** suffit) → `GITHUB_USER` et `GITHUB_TOKEN` dans `ci/.env`. Sans token, le scan du dépôt échoue.
      Jenkins de référence uniquement (Mariam) : token limité au dépôt avec *Contents*, *Commit statuses* et *Pull requests* en lecture/écriture, `SMEE_URL` (channel https://smee.io relié au webhook du dépôt) et `RELEASES_ENABLED=true`.
5. `docker compose up -d --build jenkins`
6. Jenkins → job `job-application-tracker` → **Scan Repository Now**. Le build de `feature/ci-jenkins` doit passer.

Les secrets restent dans `ci/.env` (ignoré par Git). **Ne jamais** les committer ni les coller dans un chat ou une PR ; s'échanger en message privé.

> Jenkins/Sonar/Nexus sont **locaux à chaque PC** : l'historique de builds de l'un n'est pas visible par les autres.

---

## 4. Workflow Git

### Branches
```
main                      (ne pas toucher : branche de Mariam)
feature/ci-jenkins        branche commune d'intégration (c'est elle qui déploie)
  ├── module/<nom>        une branche par module / par tâche
```

### Cycle d'une tâche
```bash
git checkout feature/ci-jenkins && git pull
git checkout -b module/<nom>
# ... travail, commits fréquents ...
git push -u origin module/<nom>
```
1. Jenkins construit la branche (tests + Sonar, **sans déployer**).
2. Ouvrir une **Pull Request** vers `feature/ci-jenkins`.
3. **Un autre membre** la relit (voir §7) ; build vert obligatoire.
4. **L'auteur** fusionne en **Squash and merge** (seule option activée), supprime la branche, puis tout le monde fait `git pull`. Ne plus pousser sur une branche déjà fusionnée : repartir d'une nouvelle branche.

### Règles
- **PR petites et fréquentes** (le `Jenkinsfile` et `deploy.sh` sont modifiés par plusieurs modules : les gros écarts créent des conflits).
- **Conventional Commits** : `feat(ci): ...`, `fix(deploy): ...`, `docs: ...`, `chore: ...`. **Vérifié sur chaque PR** (étape `Commit lint`) ; le **titre de la PR** devient le message du commit après le squash, il doit donc aussi respecter le format. semantic-release calcule la version à partir de ces messages (`feat` → mineure, `fix` → correctif).
- Ne jamais pousser directement sur `feature/ci-jenkins` ni forcer (`--force`).
- Avant de modifier le `Jenkinsfile`, vérifier qu'il reste valide : l'ouvrir dans Jenkins (Replay) ou lancer un build sur sa branche.
- Les fichiers `*.sh` et `ci/**` doivent rester en **LF** (déjà imposé par `.gitattributes`).
- Une PR qui modifie `ci/jenkins/*` (`casc.yaml`, `plugins.txt`) ne peut passer que si le Jenkins de référence est reconstruit avec ces changements. Mieux : deux PR (d'abord la configuration, puis son utilisation dans le `Jenkinsfile`).
- `main` est protégée : ne jamais ouvrir de PR vers `main` (c'est le site en ligne).

---

## 5. Répartition des tâches

> Remplacer A/B/C/D par les prénoms. Les modules de niveau 3 sont obligatoires pour un quadrinôme.

| | Personne | Tâches |
|---|---|---|
| **A** | _à compléter_ | Shared Library, tests d'intégration, module **Qualité et retour rapide** |
| **B** | _à compléter_ | Module **Environnements as Code** (Terraform/OpenTofu + Ansible) — niveau 3 |
| **C** | _à compléter_ | Module **Observabilité et DORA** (Prometheus + Grafana) — niveau 3 |
| **D** | _à compléter_ | Accès dépôt + webhook + statut PR, modules **Releases automatisées** et **Blue/green** |

Total modules : Terraform/Ansible (B), DORA (C), Qualité (A), Releases (D), Blue/green (D) = **5**.
Transverse (tous) : rapport technique, README, répétition de soutenance.

### A — Shared Library, tests d'intégration, Qualité
Shared Library (partie commune) :
- [ ] Dépôt dédié `jenkins-shared-library` avec `vars/` (ex. `publishToNexus`, `deployTo`, `notify`)
- [ ] Versionnée par **tag** (`v1.0.0`), appelée dans le Jenkinsfile via `@Library('...@v1.0.0')`
- [ ] Les étapes communes du Jenkinsfile (publication Nexus, déploiement) migrées dans la lib
- [ ] Bonus : tests avec JenkinsPipelineUnit

Tests d'intégration (partie commune) :
- [ ] `docker-compose.test.yml` : l'appli + PostgreSQL, un test qui appelle l'API (register/login/health)
- [ ] Étape dédiée dans le Jenkinsfile, nettoyage des conteneurs en `post { always }`

Module Qualité et retour rapide (niveau 1) :
- [ ] Couverture minimale **imposée** (JaCoCo + seuil qui fait échouer le build ; Sonar coverage)
- [ ] Rapports publiés dans Jenkins (JUnit, couverture)
- [ ] Notification de **chaque build** (Slack, Discord ou e-mail)
- [ ] Pipeline **< 10 min** : parallélisme + cache (cache Maven/npm déjà en volumes, à mesurer)

### B — Environnements as Code (niveau 3) — détails dans `infra/README.md`
- [x] Staging et prod décrits en **OpenTofu** (réseau, volume, PostgreSQL, secrets) — `infra/terraform`
- [x] Configuration par **Ansible** (déploiement de l'appli, healthcheck) — `infra/ansible`
- [x] Plan calculé à chaque build et archivé ; étape de publication sur la PR prête
- [x] Plan **effectivement publié sur une PR** : nécessite la source GitHub + token (rôle D)
- [x] `apply` exécuté par le pipeline **après validation** (fusion pour staging, `input` pour la prod), sur le plan enregistré
- [x] État et secrets hors du dépôt (volume `tf_state` de l'agent, `.gitignore`)
- [x] Interface avec D : `ci/scripts/deploy.sh <env> <version>` inchangé ; le blue/green se fait dans `infra/ansible/bluegreen.yml`

### C — Observabilité et DORA (niveau 3)
- [ ] Prometheus + Grafana ajoutés à la stack (`ci/docker-compose.yml`)
- [ ] Métriques de l'appli (`/actuator/prometheus`) et de Jenkins (plugin Prometheus)
- [ ] Les **4 métriques DORA** calculées automatiquement depuis Jenkins et Git :
  fréquence de déploiement, délai de livraison des changements, taux d'échec des changements, temps de rétablissement
- [ ] Tableau de bord Grafana suivi **sur la durée du projet** → démarrer tôt
- [ ] Section « métriques DORA » du rapport (courbes + analyse)
- Décider **quelle machine sert d'instance de référence** pour accumuler les builds.

### D — Dépôt/webhook/PR, Releases, Blue/green
Partie commune :
- [x] Rôle admin sur le dépôt ; A, B, C invités ; `feature/ci-jenkins` et `main` protégées (PR + 1 approbation + check Jenkins)
- [x] Token GitHub **fine-grained** dans `ci/.env` (référence : lecture/écriture ; autres membres : lecture seule)
- [x] Source du job « GitHub » dans `ci/jenkins/casc.yaml` : builds `PR-<n>`, statut `continuous-integration/jenkins/pr-merge` sur les PR
- [x] **Webhook** GitHub → smee.io → conteneur `ci-smee` → Jenkins (`smee-client@2.0.4` : la v5 supprime le `/` final, Jenkins répond 405) ; scan horaire en secours

Module Releases automatisées (niveau 1) :
- [x] Conventional Commits imposés en CI (`commitlint`, étape `Commit lint` sur les PR)
- [x] semantic-release : calcul de version, `CHANGELOG.md`, **tag Git** `vX.Y.Z` (branche `feature/ci-jenkins`, `main` étant le site en ligne)
- [x] Artefact versionné `X.Y.Z-<hash>`, **notes de version** publiées en GitHub Release ; publication uniquement sur le Jenkins de référence (`RELEASES_ENABLED`)

Module Blue/green (niveau 2) :
- [x] Deux instances (blue/green) derrière un **reverse proxy** nginx, en staging et en production
- [x] Bascule après **vérification de santé automatique**, sans interruption (`nginx -s reload`)
- [x] **Retour arrière automatique** en cas d'échec ; redéployer la même version ne change rien (idempotence)

### Tous
- [ ] Rapport technique (§9) — chacun rédige ses sections
- [ ] README racine : tout relancer en une commande
- [ ] Répétition de soutenance (§8)

---

## 6. Ordre et dépendances

| Phase | Contenu | Remarque |
|---|---|---|
| 0 | Chacun monte sa stack (§3), D obtient l'accès admin | bloquant pour D |
| 1 | A : Shared Library ; C : démarrer DORA/Prometheus | A bloque les autres (la lib est utilisée partout) ; DORA doit accumuler des données |
| 2 | A : tests d'intégration, Qualité ; B : Terraform/Ansible ; D : webhook/PR, Releases | |
| 3 | D : Blue/green (après B pour l'interface de déploiement) | dépend de `deploy.sh` et des environnements de B |
| 4 | Rapport, README, relectures croisées, répétitions | prévoir une marge avant la soutenance |

### Interface B ↔ D (à fixer ensemble)
Fait : `ci/scripts/deploy.sh <env> <version>` reste le point de contact. OpenTofu (B) fournit réseau, base et secrets ; Ansible gère l'appli, les deux couleurs et le proxy nginx (D, `infra/ansible/bluegreen.yml`). OpenTofu n'a pas été modifié : nginx refuse de démarrer si sa cible n'existe pas encore, l'ordre appli → proxy est donc géré dans le playbook.

---

## 7. Relectures croisées

Rotation obligatoire (chacun doit comprendre une partie qu'il n'a pas écrite, exigence de la soutenance) :

| Relecteur | Relit |
|---|---|
| A | B |
| B | C |
| C | D |
| D | A |

Checklist de relecture d'une PR :
- Le build Jenkins de la branche est vert.
- Aucun secret dans le diff (mots de passe, tokens, clés).
- Le `Jenkinsfile` reste valide, les étapes tournent sur l'agent (jamais le controller).
- Le déploiement reste **idempotent** (relancer ne casse rien).
- La doc/README est mise à jour.
- Le relecteur peut **expliquer** ce que fait le changement (il devra le refaire en soutenance).

---

## 8. Soutenance

- **Démo 1** : un commit, puis build, tests, Sonar, Nexus, staging, validation manuelle, prod.
- **Démo 2** : un **échec volontaire bloqué par le pipeline** (par ex. test cassé, Quality Gate rouge, couverture sous le seuil). Préparer 2 ou 3 scénarios prêts.
- **Chacun modifie en direct** une partie qu'il n'a pas écrite → s'entraîner avec la rotation du §7.
- Tout le monde doit pouvoir expliquer **toute** la chaîne.
- Désigner **une machine** pour la démo, stack à jour, secrets en place, **testée la veille**.
- Vérifier la veille : les ports 8090/9000/8081/8082/8083 libres, Docker démarré, `docker compose up -d` OK.

---

## 9. Rapport technique (plan)

1. Architecture (schéma de la chaîne, rôle de chaque composant)
2. Choix techniques et justifications (par module, avec alternatives écartées)
3. Pipeline détaillé (étapes, Shared Library, secrets)
4. Environnements et déploiement (staging, prod, blue/green, Terraform/Ansible)
5. Qualité (couverture, Sonar, durée du pipeline)
6. Métriques DORA (courbes, analyse, limites)
7. Difficultés rencontrées et solutions
8. Répartition du travail

---

## 10. Dépannage (problèmes déjà rencontrés)

| Symptôme | Cause / solution |
|---|---|
| Upload Nexus en **403** (« accept the EULA ») | Accepter la licence dans l'assistant Nexus (UI), une fois |
| Le build attend indéfiniment à « Quality Gate » | Webhook SonarQube → Jenkins absent : `scripts/bootstrap-sonar.sh` |
| « GitHub API rate limit exceeded » | Requêtes anonymes limitées à 60/h : utiliser un token, ou la source Git simple |
| `mvnw: Permission denied`, `esbuild EACCES` | Dossier de travail de l'agent monté `noexec` ; il est en `/opt/jenkins-agent` |
| Modification de `casc.yaml` sans effet | `docker compose up -d --build jenkins` ; la config est dans l'image, pas dans le volume |
| Scripts qui échouent dans les conteneurs (`\r`) | Fins de ligne CRLF : garder LF (`.gitattributes`) |
| Build abandonné « Timeout has been exceeded » | Timeout global 60 min, `input` de prod : 30 min |
| `docker exec ... /nexus-data/...` : chemin Git Bash modifié | Utiliser `docker exec ci-nexus sh -c "cat /nexus-data/admin.password"` |
| App qui ne démarre pas en déploiement | `docker logs tracker-staging-blue` (ou `-green`) ; couleur active : `docker exec tracker-staging-proxy cat /etc/nginx/active_color` |
| Webhook reçu mais `POST /github-webhook - 405` dans `docker logs ci-smee` | URL sans `/` final : garder `smee-client@2.0.4` |
| `npm ci` : « package.json and package-lock.json are not in sync » | Régénérer le lock avec le npm de l'agent (`npm install --package-lock-only` dans `ci-agent`) |
| semantic-release `ENOGHTOKEN` en local | `GITHUB_TOKEN` vide : `export GITHUB_TOKEN=$(grep '^GITHUB_TOKEN=' ci/.env \| cut -d= -f2- \| tr -d '\r')` |
| `docker` répond `500 Internal Server Error`, ou les `localhost:*` ne répondent plus | Docker Desktop bloqué (souvent mémoire) : *Quit Docker Desktop*, `wsl --shutdown`, relancer |
| Workspace de l'agent vide après un redémarrage | Normal (dossier non persistant) : Jenkins reclone au prochain build ; pour un test manuel, cloner la branche dans `/tmp` |
| Build `ABORTED` après « Deploy staging » | Personne n'a validé la production dans les 30 min : cliquer **Proceed** sur le build |

Commandes utiles :
```bash
docker compose -f ci/docker-compose.yml ps          # état de la stack
docker compose -f ci/docker-compose.yml logs -f jenkins
docker logs tracker-staging-blue                     # logs de l'appli déployée (ou -green)
```
