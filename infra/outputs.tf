output "web_url" {
  description = "Open this to use the portal."
  value       = local.web_url
}

output "keycloak_admin_console" {
  description = "Keycloak admin console (user: admin, password: terraform output -raw keycloak_admin_password)."
  value       = "${local.iam_url}/admin/"
}

output "keycloak_admin_password" {
  value     = random_password.keycloak_admin.result
  sensitive = true
}

output "postgres_host" {
  value = local.db_host
}

output "postgres_password" {
  value     = random_password.db.result
  sensitive = true
}

output "resource_group" {
  value = azurerm_resource_group.rg.name
}

output "github_variables" {
  description = "Add these as repository VARIABLES in GitHub (Settings > Secrets and variables > Actions > Variables)."
  value = var.enable_github_deploy ? {
    AZURE_CLIENT_ID       = azuread_application.github[0].client_id
    AZURE_TENANT_ID       = data.azuread_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID = var.subscription_id
    AZURE_RESOURCE_GROUP  = azurerm_resource_group.rg.name
  } : null
}
