#!/bin/sh
# Ajoute le webhook SonarQube -> Jenkins (nécessaire à waitForQualityGate). Idempotent.
# Usage : SONAR_ADMIN_PASSWORD=... sh bootstrap-sonar.sh
SONAR_URL=${SONAR_URL:-http://localhost:9000}
if curl -fs -u "admin:$SONAR_ADMIN_PASSWORD" "$SONAR_URL/api/webhooks/list" | grep -q 'sonarqube-webhook'; then echo "webhook déjà présent"; exit 0; fi
curl -fsS -u "admin:$SONAR_ADMIN_PASSWORD" -X POST "$SONAR_URL/api/webhooks/create" \
  -d name=jenkins --data-urlencode "url=http://jenkins:8080/sonarqube-webhook/" && echo "webhook créé"
