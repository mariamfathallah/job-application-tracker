FROM jenkins/inbound-agent:latest-jdk21
USER root

# Outils de build : Node 22 (frontend), curl/jq (scripts)
RUN apt-get update && apt-get install -y --no-install-recommends curl jq ca-certificates gnupg unzip \
 && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

# Environnements as Code : Ansible (le paquet Debian inclut la collection community.docker) et OpenTofu
ARG TOFU_VERSION=1.10.6
RUN apt-get update && apt-get install -y --no-install-recommends ansible python3-docker python3-requests \
 && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL -o /tmp/tofu.zip "https://github.com/opentofu/opentofu/releases/download/v${TOFU_VERSION}/tofu_${TOFU_VERSION}_linux_amd64.zip" \
 && unzip -o /tmp/tofu.zip tofu -d /usr/local/bin && rm /tmp/tofu.zip && tofu version
ENV TF_PLUGIN_CACHE_DIR=/root/.terraform.d/plugin-cache

# Docker CLI + Compose : l'agent pilote le démon Docker de l'hôte (socket monté)
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins/docker-compose /usr/local/libexec/docker/cli-plugins/docker-compose
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && mkdir -p /root/.terraform.d/plugin-cache /tfstate
ENTRYPOINT ["/entrypoint.sh"]
