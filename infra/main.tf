# Wealth portal on Azure, sized for the free tier.
#
#   Resource group rg-wealth-portal
#   ├─ Container Apps environment (Consumption, no log workspace)
#   │   ├─ wealth-web  Angular + Nginx      public HTTPS   0.25 vCPU / 0.5 GiB
#   │   ├─ wealth-api  Spring Boot           internal only  0.5 vCPU  / 1 GiB
#   │   └─ wealth-iam  Keycloak              public HTTPS   0.5 vCPU  / 1 GiB
#   │       every app scales to zero when idle (no cost while asleep)
#   └─ PostgreSQL Flexible Server B1ms (free 12 months) with databases wealth + keycloak
#   + Microsoft Entra app for GitHub Actions (OIDC, no stored password)

locals {
  rg_name      = "rg-${var.prefix}-portal"
  image_prefix = "ghcr.io/${lower(split("/", var.github_repo)[0])}"
  use_ghcr_auth = var.ghcr_token != ""

  domain  = azurerm_container_app_environment.env.default_domain
  web_url = "https://${var.prefix}-web.${local.domain}"
  iam_url = "https://${var.prefix}-iam.${local.domain}"
  issuer  = "${local.iam_url}/realms/wealth"

  db_host = azurerm_postgresql_flexible_server.db.fqdn

  tags = {
    app        = "wealth-portal"
    managed_by = "terraform"
  }
}

resource "random_string" "suffix" {
  length  = 5
  upper   = false
  special = false
}

resource "random_password" "db" {
  length           = 24
  special          = true
  override_special = "_-!"
}

resource "random_password" "keycloak_admin" {
  length           = 20
  special          = true
  override_special = "_-!"
}

# ---------------------------------------------------------------- resource group

resource "azurerm_resource_group" "rg" {
  name     = local.rg_name
  location = var.location
  tags     = local.tags
}

# ---------------------------------------------------------------- PostgreSQL

resource "azurerm_postgresql_flexible_server" "db" {
  name                = "${var.prefix}-pg-${random_string.suffix.result}"
  resource_group_name = azurerm_resource_group.rg.name
  # May differ from the resource group's region when PostgreSQL is not offered there.
  location            = coalesce(var.db_location, azurerm_resource_group.rg.location)
  version             = var.postgres_version

  # Free-account offer: Burstable B1ms, 32 GB storage.
  sku_name                     = "B_Standard_B1ms"
  storage_mb                   = 32768
  storage_tier                 = "P4"
  auto_grow_enabled            = false
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false

  public_network_access_enabled = true
  administrator_login           = "wealthadmin"
  administrator_password        = random_password.db.result

  authentication {
    password_auth_enabled         = true
    active_directory_auth_enabled = false
  }

  tags = local.tags

  lifecycle {
    # Azure picks the availability zone; don't fight it.
    ignore_changes = [zone, high_availability]
  }
}

# 0.0.0.0 is Azure's special rule for "services inside Azure" (our container apps).
resource "azurerm_postgresql_flexible_server_firewall_rule" "azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.db.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "my_pc" {
  count            = var.my_ip == "" ? 0 : 1
  name             = "MyPc"
  server_id        = azurerm_postgresql_flexible_server.db.id
  start_ip_address = var.my_ip
  end_ip_address   = var.my_ip
}

resource "azurerm_postgresql_flexible_server_database" "wealth" {
  name      = "wealth"
  server_id = azurerm_postgresql_flexible_server.db.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

resource "azurerm_postgresql_flexible_server_database" "keycloak" {
  name      = "keycloak"
  server_id = azurerm_postgresql_flexible_server.db.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# ---------------------------------------------------------------- Container Apps

# No Log Analytics workspace: it would cost money. Live logs still work with
# `az containerapp logs show`.
resource "azurerm_container_app_environment" "env" {
  name                = "${var.prefix}-env"
  # May differ from the resource group's region when that region has no Container Apps capacity.
  location            = coalesce(var.apps_location, azurerm_resource_group.rg.location)
  resource_group_name = azurerm_resource_group.rg.name
  tags                = local.tags
}

# --- Keycloak (sign-in) ------------------------------------------------------

resource "azurerm_container_app" "iam" {
  name                         = "${var.prefix}-iam"
  container_app_environment_id = azurerm_container_app_environment.env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"
  tags                         = local.tags

  secret {
    name  = "db-password"
    value = random_password.db.result
  }
  secret {
    name  = "admin-password"
    value = random_password.keycloak_admin.result
  }
  dynamic "secret" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      name  = "ghcr-token"
      value = var.ghcr_token
    }
  }
  dynamic "registry" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      server               = "ghcr.io"
      username             = var.ghcr_username
      password_secret_name = "ghcr-token"
    }
  }

  template {
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "keycloak"
      image  = "${local.image_prefix}/wealth-keycloak:${var.image_tag}"
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "KC_DB_URL"
        value = "jdbc:postgresql://${local.db_host}:5432/keycloak?sslmode=require"
      }
      env {
        name  = "KC_DB_USERNAME"
        value = azurerm_postgresql_flexible_server.db.administrator_login
      }
      env {
        name        = "KC_DB_PASSWORD"
        secret_name = "db-password"
      }
      env {
        name  = "KC_DB_POOL_INITIAL_SIZE"
        value = "1"
      }
      env {
        name  = "KC_DB_POOL_MAX_SIZE"
        value = "10"
      }
      env {
        name  = "KC_HOSTNAME"
        value = local.iam_url
      }
      env {
        # TLS ends at the Container Apps ingress; Keycloak itself speaks HTTP.
        name  = "KC_HTTP_ENABLED"
        value = "true"
      }
      env {
        name  = "KC_PROXY_HEADERS"
        value = "xforwarded"
      }
      env {
        name  = "KC_BOOTSTRAP_ADMIN_USERNAME"
        value = "admin"
      }
      env {
        name        = "KC_BOOTSTRAP_ADMIN_PASSWORD"
        secret_name = "admin-password"
      }
      env {
        # Fills ${WEB_URL} in the realm file: the cloud app may use this sign-in.
        name  = "WEB_URL"
        value = local.web_url
      }
    }
  }

  ingress {
    external_enabled = true
    target_port      = 8080
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  lifecycle {
    # The GitHub workflow rolls out new image tags.
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [
    azurerm_postgresql_flexible_server_database.keycloak,
    azurerm_postgresql_flexible_server_firewall_rule.azure_services,
  ]
}

# --- Spring Boot API ---------------------------------------------------------

resource "azurerm_container_app" "api" {
  name                         = "${var.prefix}-api"
  container_app_environment_id = azurerm_container_app_environment.env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"
  tags                         = local.tags

  secret {
    name  = "db-password"
    value = random_password.db.result
  }
  dynamic "secret" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      name  = "ghcr-token"
      value = var.ghcr_token
    }
  }
  dynamic "registry" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      server               = "ghcr.io"
      username             = var.ghcr_username
      password_secret_name = "ghcr-token"
    }
  }

  template {
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "api"
      image  = "${local.image_prefix}/wealth-backend:${var.image_tag}"
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${local.db_host}:5432/wealth?sslmode=require"
      }
      env {
        name  = "DB_USER"
        value = azurerm_postgresql_flexible_server.db.administrator_login
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      env {
        name  = "DB_POOL_SIZE"
        value = "5"
      }
      env {
        name  = "KEYCLOAK_ISSUER"
        value = local.issuer
      }
      env {
        name  = "KEYCLOAK_JWKS"
        value = "${local.issuer}/protocol/openid-connect/certs"
      }
      env {
        name  = "FLYWAY_LOCATIONS"
        value = var.load_demo_data ? "classpath:db/migration,classpath:db/demo" : "classpath:db/migration"
      }
    }
  }

  # Internal only: reachable from wealth-web as http://wealth-api, not from the internet.
  # Plain HTTP is allowed because the call never leaves the environment.
  ingress {
    external_enabled           = false
    allow_insecure_connections = true
    target_port                = 8080
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  lifecycle {
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [
    azurerm_postgresql_flexible_server_database.wealth,
    azurerm_postgresql_flexible_server_firewall_rule.azure_services,
  ]
}

# --- Angular + Nginx (the address people open) --------------------------------

resource "azurerm_container_app" "web" {
  name                         = "${var.prefix}-web"
  container_app_environment_id = azurerm_container_app_environment.env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"
  tags                         = local.tags

  dynamic "secret" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      name  = "ghcr-token"
      value = var.ghcr_token
    }
  }
  dynamic "registry" {
    for_each = local.use_ghcr_auth ? [1] : []
    content {
      server               = "ghcr.io"
      username             = var.ghcr_username
      password_secret_name = "ghcr-token"
    }
  }

  template {
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "web"
      image  = "${local.image_prefix}/wealth-frontend:${var.image_tag}"
      cpu    = 0.25
      memory = "0.5Gi"

      env {
        name  = "API_UPSTREAM"
        value = "http://${azurerm_container_app.api.name}"
      }
      env {
        name  = "KEYCLOAK_URL"
        value = local.iam_url
      }
    }
  }

  ingress {
    external_enabled = true
    target_port      = 80
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  lifecycle {
    ignore_changes = [template[0].container[0].image]
  }
}

# ---------------------------------------------------------------- GitHub OIDC

data "azuread_client_config" "current" {}

resource "azuread_application" "github" {
  count        = var.enable_github_deploy ? 1 : 0
  display_name = "${var.prefix}-github-deploy"
  owners       = [data.azuread_client_config.current.object_id]
}

resource "azuread_service_principal" "github" {
  count     = var.enable_github_deploy ? 1 : 0
  client_id = azuread_application.github[0].client_id
  owners    = [data.azuread_client_config.current.object_id]
}

# Trusts tokens GitHub issues to workflows running on the main branch of this repo.
resource "azuread_application_federated_identity_credential" "github_main" {
  count          = var.enable_github_deploy ? 1 : 0
  application_id = azuread_application.github[0].id
  display_name   = "github-main"
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = "https://token.actions.githubusercontent.com"
  subject        = "repo:${var.github_repo}:ref:refs/heads/main"
}

# The workflow may change resources in this resource group only.
resource "azurerm_role_assignment" "github" {
  count                = var.enable_github_deploy ? 1 : 0
  scope                = azurerm_resource_group.rg.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.github[0].object_id
}
