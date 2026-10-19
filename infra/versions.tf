terraform {
  required_version = ">= 1.9"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.14"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # State is kept on your PC (infra/terraform.tfstate) to stay free.
  # It contains passwords: never commit it (it is in .gitignore).
}

# Signs in with your Azure CLI login (`az login`).
provider "azurerm" {
  subscription_id = var.subscription_id

  features {
    resource_group {
      # `terraform destroy` removes the group even if something was added by hand.
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azuread" {}
