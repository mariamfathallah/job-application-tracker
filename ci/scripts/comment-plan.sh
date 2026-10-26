#!/bin/sh
# Publie les plans OpenTofu en commentaire de la pull request (build de PR uniquement).
# Env : CHANGE_ID (fourni par Jenkins), GITHUB_REPO (owner/repo), GH_TOKEN (credential github-token)
set -eu
TF_DIR="$(cd "$(dirname "$0")/../../infra/terraform" && pwd)"
BODY=$(mktemp); trap 'rm -f "$BODY"' EXIT
{
  echo "### Plan OpenTofu (build #${BUILD_NUMBER:-?})"
  for env in staging production; do
    echo; echo "<details><summary>$env</summary>"; echo; echo '```'
    head -c 28000 "$TF_DIR/plan-$env.txt"
    echo '```'; echo "</details>"
  done
  echo; echo "Appliqué par le pipeline après fusion (staging) puis validation manuelle (production)."
} > "$BODY"
jq -Rs '{body: .}' < "$BODY" | curl -fsS -X POST \
  -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$GITHUB_REPO/issues/$CHANGE_ID/comments" -d @- >/dev/null
echo "plan publié sur la PR #$CHANGE_ID"
