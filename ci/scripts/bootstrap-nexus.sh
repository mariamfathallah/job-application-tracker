#!/bin/sh
# Crée le dépôt raw "artifacts" dans Nexus (idempotent).
# Usage : NEXUS_URL=http://localhost:8081 NEXUS_PASSWORD=... ./bootstrap-nexus.sh
NEXUS_URL=${NEXUS_URL:-http://localhost:8081}
code=$(curl -s -o /dev/null -w '%{http_code}' -u "admin:$NEXUS_PASSWORD" "$NEXUS_URL/service/rest/v1/repositories/artifacts")
if [ "$code" = "200" ]; then echo "dépôt 'artifacts' déjà présent"; exit 0; fi
set -e
curl -fsS -u "admin:$NEXUS_PASSWORD" -X POST -H 'Content-Type: application/json' \
  "$NEXUS_URL/service/rest/v1/repositories/raw/hosted" \
  -d '{"name":"artifacts","online":true,"storage":{"blobStoreName":"default","strictContentTypeValidation":false,"writePolicy":"allow"}}'
echo "dépôt 'artifacts' créé"
