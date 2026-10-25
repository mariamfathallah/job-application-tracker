# Environnements as Code (module niveau 3)

Staging et production sont **décrits avec OpenTofu** (Terraform open source) et **configurés avec Ansible**.
Le plan est calculé à chaque build, publié sur la pull request, et appliqué par le pipeline après validation.

## Qui fait quoi

| Outil | Gère                                                                                                                                                             | Pourquoi lui |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| **OpenTofu** (`infra/terraform`) | réseau Docker isolé, volume de données, base PostgreSQL, secrets (mot de passe BDD, clé JWT)                                                                     | infrastructure durable, qui change rarement : on décrit l'état voulu, OpenTofu calcule l'écart (plan) |
| **Ansible** (`infra/ansible`) | l'application : téléchargement de l'artefact Nexus, image, conteneurs blue/green, proxy nginx, bascule et vérification de santé | change à chaque livraison (nouvelle version) : c'est de la configuration, pas de l'infrastructure |

Les deux environnements ont **le même code** ; seules les valeurs changent (`environments/staging.tfvars`, `environments/production.tfvars`).

## Ce qui est créé par environnement

```
tracker-<env>-net        réseau Docker isolé                (OpenTofu)
tracker-<env>-db-data    volume des données PostgreSQL      (OpenTofu, protégé contre la destruction)
tracker-<env>-db         conteneur PostgreSQL 16 + healthcheck (OpenTofu)
tracker-<env>-proxy      proxy nginx, seul port publié      (Ansible)  → :8082 staging, :8083 production
tracker-<env>-blue       une version de l'application       (Ansible, sans port publié)
tracker-<env>-green      l'autre version                    (Ansible, sans port publié)
```

Chaque ressource porte les labels `project`, `environment` et `managed-by` (`opentofu` ou `ansible`) :
`docker ps --format '{{.Names}} {{.Labels}}'` montre qui gère quoi.

## Déploiement blue/green (Ansible)

Chaque environnement a deux conteneurs applicatifs (`blue`, `green`) derrière un proxy nginx (voir ci-dessus) :

1. La nouvelle version est déployée sur la couleur **inactive** (le trafic n'est pas touché).
2. Vérification de santé directement sur cette couleur.
3. OK : le proxy bascule (`nginx -s reload`, sans interruption), puis vérification à travers le proxy.
4. KO : la couleur défaillante est supprimée, le proxy reste (ou revient) sur l'ancienne : retour arrière automatique.
5. L'ancienne couleur reste démarrée pour un retour arrière instantané.

Redéployer la version déjà active ne change rien (idempotence).
Stratégie choisie par `DEPLOY_STRATEGY` (`bluegreen` par défaut, `simple` = un seul conteneur).
Couleur active : `docker exec tracker-<env>-proxy cat /etc/nginx/active_color`

## Dans le pipeline

```
Infra plan          (toutes les branches) plan staging + production → artefacts plan-*.txt
Publish plan on PR  (builds de PR)        plans publiés en commentaire de la PR
Deploy staging      (branche de déploiement) tofu apply staging → Ansible déploie la version
Deploy production   validation manuelle (input) → tofu apply production → Ansible
```

- `apply` applique **exactement le plan enregistré** plus tôt dans le même build (`<env>.tfplan`) : ce qui a été relu est ce qui est appliqué. Si l'infrastructure a changé entre-temps, OpenTofu refuse (plan périmé).
- Le commentaire de PR nécessite la source GitHub du job et le credential `github-token` (voir rôle D). Avec la source Git simple, l'étape est sautée et les plans restent disponibles en artefacts du build.

## Scripts

| Script | Rôle |
|---|---|
| `ci/scripts/infra.sh <plan\|apply\|output> <env>` | pilote OpenTofu pour un environnement |
| `ci/scripts/deploy.sh <env> <version>` | lit les outputs OpenTofu puis lance le playbook Ansible |
| `ci/scripts/comment-plan.sh` | publie les plans sur la PR (API GitHub) |

## État (state) et secrets

- L'état OpenTofu contient les mots de passe générés : il n'est **jamais dans Git** (`.gitignore`).
- Il vit dans le volume Docker `tf_state` monté sur l'agent (`/tfstate/<env>.tfstate`) et survit aux redémarrages.
- Les secrets passent d'OpenTofu à Ansible par un fichier temporaire (droits 600, supprimé à la fin) ; les tâches sensibles sont en `no_log`.
- `.terraform.lock.hcl` est versionné : tout le monde utilise exactement les mêmes versions de providers.

## Idempotence (vérifiée)

- Relancer `infra.sh plan` sans changement : `No changes. Your infrastructure matches the configuration.`
- Relancer `deploy.sh` avec la même version : `changed=0` côté Ansible, rien n'est recréé.
- Chaque version est téléchargée dans son propre dossier (`/tmp/build-tracker-<env>/<version>`) : l'image contient toujours le jar de la version annoncée (vérifiable avec `docker exec tracker-<env>-<couleur> sha1sum /app.jar`). Les dossiers des versions précédentes sont supprimés.

## Commandes utiles (depuis l'agent)

```bash
docker exec -it ci-agent bash
cd /opt/jenkins-agent/workspace/<dossier du build>
sh ci/scripts/infra.sh plan staging        # voir l'écart entre le code et l'existant
sh ci/scripts/infra.sh output staging      # outputs (contient des secrets)
ls /tfstate                                # fichiers d'état
# si le workspace est vide (redémarrage de Docker) : cloner la branche dans /tmp
git clone --branch <branche> --depth 1 https://github.com/mariamfathallah/job-application-tracker.git /tmp/bg && cd /tmp/bg
```

## Exemples de modifications (pour la soutenance)

- **Changer le port de staging** : `app_port` dans `environments/staging.tfvars` → le plan montre le nouvel output, Ansible republie le port.
- **Changer la version de PostgreSQL** : `postgres_image` → le plan montre le remplacement du conteneur de base (les données restent dans le volume).
- **Ajouter une variable d'environnement à l'application** : bloc `env` de la tâche « Conteneur » dans `infra/ansible/bluegreen.yml` (et `simple.yml` pour la stratégie simple).
- **Montrer la protection des données** : supprimer la ressource `docker_volume.db_data` du code → le plan échoue grâce à `prevent_destroy`.

## Limites connues

- Les environnements sont des conteneurs sur la machine Docker de l'agent (pas des VM) : c'est le provider `kreuzwerker/docker`. Passer à des VM cloud reviendrait à changer de provider dans `main.tf`, la structure restant la même.
- L'état est local au volume de l'agent : chaque membre de l'équipe a ses propres environnements (cohérent avec une stack par personne).
- Blue/green : l'ancienne couleur reste démarrée pour un retour arrière instantané, ce qui coûte un conteneur Java de plus (~400 Mo) par environnement.