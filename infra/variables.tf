variable "subscription_id" {
  description = "Azure subscription to deploy into (az account show --query id -o tsv)."
  type        = string
}

variable "location" {
  description = "Azure region. If PostgreSQL is refused there on a free account, try westeurope, northeurope or swedencentral."
  type        = string
  default     = "germanywestcentral"
}

variable "prefix" {
  description = "Short name used in every resource name."
  type        = string
  default     = "wealth"
}

variable "github_repo" {
  description = "owner/repo of the GitHub repository; its images and deploy workflow are used."
  type        = string
  default     = "joinsrikanth945/wealth-portal"
}

variable "image_tag" {
  description = "Image tag for the first deployment. Later deployments are done by the GitHub workflow."
  type        = string
  default     = "latest"
}

variable "ghcr_username" {
  description = "Only needed if the GHCR packages are private: your GitHub username."
  type        = string
  default     = ""
}

variable "ghcr_token" {
  description = "Only needed if the GHCR packages are private: a GitHub token with read:packages."
  type        = string
  default     = ""
  sensitive   = true
}

variable "load_demo_data" {
  description = "Load the demo clients, portfolios and orders into the cloud database."
  type        = bool
  default     = true
}

variable "postgres_version" {
  description = "PostgreSQL major version on Azure."
  type        = string
  default     = "16"
}

variable "my_ip" {
  description = "Optional: your public IP, to connect to the cloud database from your PC (e.g. with psql). Empty = no access from outside Azure."
  type        = string
  default     = ""
}

variable "enable_github_deploy" {
  description = "Create the Microsoft Entra app that lets GitHub Actions deploy without a stored password (OIDC)."
  type        = bool
  default     = true
}
