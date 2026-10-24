#!/bin/sh
# Pilote OpenTofu pour un environnement.
# Usage : infra.sh <plan|apply|output> <staging|production>
#   plan   : calcule le plan, l'enregistre (<env>.tfplan) et l'écrit en texte (plan-<env>.txt)
#   apply  : applique EXACTEMENT le plan enregistré par l'étape plan du même build
#   output : affiche les outputs en JSON (secrets compris : réservé aux scripts)
# L'état est conservé hors du dépôt, dans $TF_STATE_DIR (volume Docker de l'agent).
set -eu
ACTION=$1; ENVIRONMENT=$2
case "$ENVIRONMENT" in staging|production) ;; *) echo "env inconnu : $ENVIRONMENT" >&2; exit 1;; esac

TF_DIR="$(cd "$(dirname "$0")/../../infra/terraform" && pwd)"
TF_STATE_DIR=${TF_STATE_DIR:-/tfstate}
export TF_DATA_DIR="$TF_DIR/.terraform-$ENVIRONMENT"   # un dossier d'init par environnement
export TF_IN_AUTOMATION=1
cd "$TF_DIR"

tofu init -input=false -reconfigure -backend-config="path=$TF_STATE_DIR/$ENVIRONMENT.tfstate" >/dev/null

case "$ACTION" in
  plan)
    tofu plan -input=false -no-color -var-file="environments/$ENVIRONMENT.tfvars" -out="$ENVIRONMENT.tfplan" \
      | tee "plan-$ENVIRONMENT.txt"
    ;;
  apply)
    [ -f "$ENVIRONMENT.tfplan" ] || { echo "aucun plan enregistré pour $ENVIRONMENT : lancer 'plan' d'abord" >&2; exit 1; }
    tofu apply -input=false -no-color "$ENVIRONMENT.tfplan"
    ;;
  output)
    tofu output -json
    ;;
  *) echo "action inconnue : $ACTION" >&2; exit 1;;
esac
