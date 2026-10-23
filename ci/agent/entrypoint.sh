#!/bin/sh
# Récupère le secret de l'agent auprès du controller puis lance l'agent (WebSocket).
set -e
until curl -fs -o /dev/null "$JENKINS_URL/login"; do echo "attente de Jenkins..."; sleep 5; done
until SECRET=$(curl -fs -u "$JENKINS_ADMIN_USER:$JENKINS_ADMIN_PASSWORD" \
    "$JENKINS_URL/computer/$JENKINS_AGENT_NAME/jenkins-agent.jnlp" | sed -n 's/.*<argument>\([0-9a-f]\{64\}\)<\/argument>.*/\1/p' | head -n1) && [ -n "$SECRET" ]; do
  echo "attente du noeud $JENKINS_AGENT_NAME..."; sleep 5
done
export JENKINS_SECRET="$SECRET" JENKINS_WEB_SOCKET=true JENKINS_AGENT_WORKDIR=/opt/jenkins-agent
exec jenkins-agent
