#!/bin/sh
# Déploie une version de l'application dans un environnement (interface stable pour le Jenkinsfile).
# Usage : deploy.sh <staging|production> <version>   (env : NEXUS_URL NEXUS_USER NEXUS_PASSWORD)
# 1. lit les informations de l'environnement dans les outputs OpenTofu (réseau, base, secrets)
# 2. lance le playbook Ansible qui installe et vérifie l'application (idempotent)
set -eu
ENVIRONMENT=$1; VERSION=$2
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

VARS=$(mktemp); trap 'rm -f "$VARS"' EXIT
chmod 600 "$VARS"
sh "$ROOT/ci/scripts/infra.sh" output "$ENVIRONMENT" \
  | jq --arg v "$VERSION" --arg s "${DEPLOY_STRATEGY:-}" '{
      env_name: .environment.value, app_version: $v, app_name: .app_name.value,
      app_port: .app_port.value, network_name: .network_name.value, db_host: .db_host.value,
      db_password: .db_password.value, jwt_secret: .jwt_secret.value, deploy_strategy: $s }' > "$VARS"

cd "$ROOT/infra/ansible"
ANSIBLE_CONFIG="$ROOT/infra/ansible/ansible.cfg" ansible-playbook deploy.yml -e "@$VARS"
