#!/bin/sh
# Déploiement idempotent : télécharge l'artefact depuis Nexus et (re)crée le conteneur.
# Usage : deploy.sh <staging|production> <version>   (env : NEXUS_URL NEXUS_USER NEXUS_PASSWORD)
set -eu
ENVIRONMENT=$1; VERSION=$2
case "$ENVIRONMENT" in staging) PORT=8082;; production) PORT=8083;; *) echo "env inconnu"; exit 1;; esac
NEXUS_URL=${NEXUS_URL:-http://nexus:8081}
NAME="tracker-$ENVIRONMENT"
WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
curl -fsS -u "$NEXUS_USER:$NEXUS_PASSWORD" -o "$WORK/app.jar" \
  "$NEXUS_URL/repository/artifacts/job-application-tracker/$VERSION/job-application-tracker-$VERSION.jar"
printf 'FROM eclipse-temurin:21-jre\nCOPY app.jar /app.jar\nEXPOSE 8080\nENTRYPOINT ["java","-jar","/app.jar"]\n' > "$WORK/Dockerfile"
docker build -q -t "$NAME:$VERSION" "$WORK"
docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" --restart unless-stopped -p "$PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=dev "$NAME:$VERSION"
for i in $(seq 1 30); do
  if curl -fs "http://host.docker.internal:$PORT/actuator/health" >/dev/null 2>&1; then echo "$ENVIRONMENT OK ($VERSION)"; exit 0; fi
  sleep 3
done
echo "healthcheck KO sur $ENVIRONMENT"; docker logs --tail 50 "$NAME"; exit 1
