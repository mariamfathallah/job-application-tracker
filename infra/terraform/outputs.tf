# Lus par Ansible (ci/scripts/deploy.sh) pour configurer l'application.

output "environment" {
  value = var.environment
}

output "app_name" {
  value = local.prefix
}

output "app_port" {
  value = var.app_port
}

output "network_name" {
  value = docker_network.env.name
}

output "db_host" {
  value = docker_container.db.name
}

output "db_password" {
  value     = random_password.db.result
  sensitive = true
}

output "jwt_secret" {
  value     = random_password.jwt.result
  sensitive = true
}
