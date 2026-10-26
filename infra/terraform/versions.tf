terraform {
  required_version = ">= 1.6"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.6"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # L'état (state) contient des secrets : il n'est jamais dans Git.
  # Son chemin est fourni à l'init : -backend-config="path=/tfstate/<env>.tfstate"
  backend "local" {}
}

provider "docker" {
  host = var.docker_host
}
