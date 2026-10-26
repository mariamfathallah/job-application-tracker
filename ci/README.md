# Plateforme CI/CD (Jenkins, SonarQube, Nexus, Prometheus, Grafana)

```bash
cd ci
cp .env.example .env        # définir JENKINS_ADMIN_PASSWORD et GRAFANA_ADMIN_PASSWORD
docker compose up -d --build
```
| Service    | URL                    |
|------------|------------------------|
| Jenkins    | http://localhost:8090  |
| SonarQube  | http://localhost:9000 (admin/admin, à changer) |
| Nexus      | http://localhost:8081  |
| Staging    | http://localhost:8082  |
| Production | http://localhost:8083  |
| Prometheus | http://localhost:9090 (cibles : /targets) |
| Grafana    | http://localhost:3000 (admin / `GRAFANA_ADMIN_PASSWORD`) |

## Première mise en route (une fois)
1. **Nexus** : mot de passe admin initial : `docker exec ci-nexus cat /nexus-data/admin.password`. Le changer dans l'UI, le mettre dans `NEXUS_PASSWORD` (.env), puis `NEXUS_PASSWORD=... sh scripts/bootstrap-nexus.sh`.
2. **SonarQube** : My Account > Security > générer un token, le mettre dans `SONAR_TOKEN` (.env).
3. `docker compose up -d` pour que Jenkins relise les credentials (JCasC).
4. Pousser le `Jenkinsfile` sur `main` : le job Multibranch le détecte (scan toutes les 5 min, ou webhook GitHub vers `<JENKINS_PUBLIC_URL>/github-webhook/`).
5. **Webhook GitHub** : créer un channel sur https://smee.io, mettre l'URL dans `SMEE_URL` (.env), puis sur GitHub (Settings > Webhooks) : Payload URL = l'URL smee, `application/json`, événements *Pushes* et *Pull requests*. Le conteneur `ci-smee` relaie les événements vers Jenkins.

## Observabilité et métriques DORA
- **Prometheus** collecte les métriques de Jenkins (plugin `prometheus`, `/prometheus/`), de l'application en staging et en production (`/actuator/prometheus`) et de l'exporter DORA. Historique conservé 180 jours.
- **Grafana** est provisionné automatiquement (source Prometheus + tableaux de bord de `grafana/dashboards/`, dossier *CI/CD*) :
  - *Job Application Tracker - Vue d'ensemble* : santé des cibles, requêtes, erreurs, mémoire, durée des builds ;
  - *DORA - Job Application Tracker* : les 4 métriques DORA, par période (7 jours, 30 jours, tout le projet).
- **`dora-exporter/`** calcule les métriques DORA depuis Jenkins et Git, sans saisie manuelle :

| Métrique | Calcul |
|---|---|
| Fréquence de déploiement | stages *Deploy production* réussis par jour (branche `main`, plus l'historique de `feature/ci-jenkins` qui déployait avant) |
| Délai de livraison | fin du déploiement réussi - heure de chaque commit livré (changeSets Jenkins, ou API GitHub pour le premier build) |
| Taux d'échec | stages *Deploy production* en échec (dont retour arrière blue/green) / déploiements |
| Temps de rétablissement | premier échec en production -> déploiement réussi suivant |

Une validation refusée n'est pas un déploiement. Jenkins ne garde que 20 builds : l'exporter mémorise chaque build terminé dans le volume `dora_data`, l'historique couvre donc toute la durée du projet. Les vraies valeurs sont celles du Jenkins de référence.

Tests de l'exporter : `python -m unittest discover -s ci/dora-exporter`
