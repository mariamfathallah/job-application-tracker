# Infrastructure d'un environnement : réseau isolé, base PostgreSQL persistante, secrets.
# L'application elle-même est déployée par Ansible (infra/ansible), car sa version change à chaque livraison.

locals {
  prefix = "tracker-${var.environment}"
  labels = {
    "project"     = "job-application-tracker"
    "environment" = var.environment
    "managed-by"  = "opentofu"
  }
}

resource "random_password" "db" {
  length  = 32
  special = false
}

# >= 256 bits exigés par la librairie JWT de l'application
resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "docker_network" "env" {
  name = "${local.prefix}-net"

  dynamic "labels" {
    for_each = local.labels
    content {
      label = labels.key
      value = labels.value
    }
  }
}

resource "docker_volume" "db_data" {
  name = "${local.prefix}-db-data"

  dynamic "labels" {
    for_each = local.labels
    content {
      label = labels.key
      value = labels.value
    }
  }

  lifecycle {
    prevent_destroy = true # les données ne doivent pas disparaître sur une erreur de plan
  }
}

resource "docker_image" "postgres" {
  name         = var.postgres_image
  keep_locally = true
}

resource "docker_container" "db" {
  name    = "${local.prefix}-db"
  image   = docker_image.postgres.image_id
  restart = "unless-stopped"

  env = [
    "POSTGRES_DB=jobtracker",
    "POSTGRES_USER=jobtracker",
    "POSTGRES_PASSWORD=${random_password.db.result}",
  ]

  networks_advanced {
    name = docker_network.env.id
  }

  volumes {
    volume_name    = docker_volume.db_data.name
    container_path = "/var/lib/postgresql/data"
  }

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U jobtracker -d jobtracker"]
    interval = "5s"
    timeout  = "3s"
    retries  = 10
  }

  dynamic "labels" {
    for_each = local.labels
    content {
      label = labels.key
      value = labels.value
    }
  }
}
