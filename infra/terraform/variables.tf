variable "environment" {
  description = "Nom de l'environnement (staging ou production)"
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment doit valoir staging ou production."
  }
}

variable "app_port" {
  description = "Port de l'hôte sur lequel l'application est exposée"
  type        = number
}

variable "postgres_image" {
  description = "Image de la base de données"
  type        = string
  default     = "postgres:16"
}

variable "docker_host" {
  description = "Démon Docker qui héberge les environnements"
  type        = string
  default     = "unix:///var/run/docker.sock"
}
