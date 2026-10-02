# Plateforme CI/CD (Jenkins, SonarQube, Nexus)

```bash
cd ci
cp .env.example .env        # définir JENKINS_ADMIN_PASSWORD
docker compose up -d --build
```
| Service    | URL                    |
|------------|------------------------|
| Jenkins    | http://localhost:8090  |
| SonarQube  | http://localhost:9000 (admin/admin, à changer) |
| Nexus      | http://localhost:8081  |
| Staging    | http://localhost:8082  |
| Production | http://localhost:8083  |

## Première mise en route (une fois)
1. **Nexus** : mot de passe admin initial : `docker exec ci-nexus cat /nexus-data/admin.password`. Le changer dans l'UI, le mettre dans `NEXUS_PASSWORD` (.env), puis `NEXUS_PASSWORD=... sh scripts/bootstrap-nexus.sh`.
2. **SonarQube** : My Account > Security > générer un token, le mettre dans `SONAR_TOKEN` (.env).
3. `docker compose up -d` pour que Jenkins relise les credentials (JCasC).
4. Pousser le `Jenkinsfile` sur `main` : le job Multibranch le détecte (scan toutes les 5 min, ou webhook GitHub vers `<JENKINS_PUBLIC_URL>/github-webhook/`).
