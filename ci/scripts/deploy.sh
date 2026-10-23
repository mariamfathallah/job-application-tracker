#!/bin/sh
# Déploiement idempotent : télécharge l'artefact depuis Nexus, (re)crée l'appli et sa base PostgreSQL.
# Usage : deploy.sh <staging|production> <version>   (env : NEXUS_URL NEXUS_USER NEXUS_PASSWORD)
# Relancé avec la même version, le résultat est identique ; la base et les secrets générés sont conservés.
set -eu
ENVIRONMENT=$1; VERSION=$2
case "$ENVIRONMENT" in staging) PORT=8082;; production) PORT=8083;; *) echo "env inconnu"; exit 1;; esac
NEXUS_URL=${NEXUS_URL:-http://nexus:8081}
APP="tracker-$ENVIRONMENT"; DB="tracker-db-$ENVIRONMENT"; NET="tracker-net-$ENVIRONMENT"

rand() { head -c 64 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 40; }
# lit une variable d'environnement d'un conteneur existant (pour conserver les secrets d'un déploiement à l'autre)
existing() { docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$1" 2>/dev/null | sed -n "s/^$2=//p" | head -n1 || true; }

WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
curl -fsS -u "$NEXUS_USER:$NEXUS_PASSWORD" -o "$WORK/app.jar" \
  "$NEXUS_URL/repository/artifacts/job-application-tracker/$VERSION/job-application-tracker-$VERSION.jar"
printf 'FROM eclipse-temurin:21-jre\nCOPY app.jar /app.jar\nEXPOSE 8080\nENTRYPOINT ["java","-jar","/app.jar"]\n' > "$WORK/Dockerfile"
docker build -q -t "$APP:$VERSION" "$WORK" >/dev/null

docker network inspect "$NET" >/dev/null 2>&1 || docker network create "$NET" >/dev/null

DB_PASSWORD=$(existing "$DB" POSTGRES_PASSWORD); [ -n "$DB_PASSWORD" ] || DB_PASSWORD=$(rand)
if ! docker inspect "$DB" >/dev/null 2>&1; then
  docker run -d --name "$DB" --network "$NET" --restart unless-stopped \
    -e POSTGRES_DB=jobtracker -e POSTGRES_USER=jobtracker -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -v "$DB-data:/var/lib/postgresql/data" postgres:16 >/dev/null
fi
for i in $(seq 1 30); do docker exec "$DB" pg_isready -U jobtracker >/dev/null 2>&1 && break; sleep 2; done

JWT_SECRET=$(existing "$APP" JWT_SECRET); [ "${#JWT_SECRET}" -ge 32 ] || JWT_SECRET=$(rand)
docker rm -f "$APP" >/dev/null 2>&1 || true
docker run -d --name "$APP" --network "$NET" --restart unless-stopped -p "$PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$DB:5432/jobtracker" \
  -e SPRING_DATASOURCE_USERNAME=jobtracker -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" "$APP:$VERSION" >/dev/null

for i in $(seq 1 40); do
  if curl -fs "http://host.docker.internal:$PORT/actuator/health" >/dev/null 2>&1; then echo "$ENVIRONMENT OK ($VERSION)"; exit 0; fi
  sleep 3
done
echo "healthcheck KO sur $ENVIRONMENT"; docker logs --tail 40 "$APP"; exit 1
