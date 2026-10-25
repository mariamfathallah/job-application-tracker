#!/bin/sh
# Tests d'intégration : appli + PostgreSQL via Docker Compose, puis appels réels à l'API.
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

COMPOSE="docker compose -f docker-compose.test.yml -p tracker-it-${EXECUTOR_NUMBER:-0}"
EMAIL="it-$(date +%s)@example.com"
PASSWORD='Secur3!Pass'

JAR="target/Job-application-tracker-0.0.1-SNAPSHOT.jar"
[ -f "$JAR" ] || { echo "ECHEC : $JAR introuvable"; exit 1; }
cp "$JAR" target/app-under-test.jar

$COMPOSE up -d --build
PORT=$($COMPOSE port app-test 8080 | cut -d: -f2)
BASE="http://${IT_HOST:-localhost}:$PORT"
echo "Application sur $BASE"

echo "Attente de /actuator/health..."
i=0
until curl -fs "$BASE/actuator/health" | grep -q '"status":"UP"'; do
  i=$((i+1))
  if [ "$i" -gt 40 ]; then
    echo "ECHEC : l'application ne démarre pas"
    $COMPOSE logs --tail 60 app-test
    exit 1
  fi
  sleep 3
done
echo "OK : health = UP"

echo "Test register..."
CODE=$(curl -s -o /tmp/it-register.json -w '%{http_code}' -X POST "$BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"IT User\"}")
[ "$CODE" = "201" ] || { echo "ECHEC register : code $CODE"; cat /tmp/it-register.json; exit 1; }
echo "OK : register (201)"

echo "Test login..."
CODE=$(curl -s -o /tmp/it-login.json -w '%{http_code}' -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
[ "$CODE" = "200" ] || { echo "ECHEC login : code $CODE"; cat /tmp/it-login.json; exit 1; }
grep -q '"token"' /tmp/it-login.json || { echo "ECHEC login : pas de token"; exit 1; }
echo "OK : login (200 + token)"

echo "Tests d'intégration réussis"