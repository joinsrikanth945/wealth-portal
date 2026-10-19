# Azure deployment (free tier) with Terraform

This folder creates the cloud version of the portal in Azure. One
`terraform apply` builds it, one `terraform destroy` removes it.

**Current deployment:** Container Apps and PostgreSQL in **North Europe**,
resource group in Germany West Central (the group's region only stores
metadata). Deploys run automatically from GitHub Actions.

## What gets created

| Azure resource | Name | Purpose | Cost |
|---|---|---|---|
| Resource group | `rg-wealth-portal` | Holds everything below | Free |
| Container Apps environment | `wealth-env` | Shared runtime, Consumption plan, no log workspace | Free |
| Container App | `wealth-web` | Angular + Nginx, **public HTTPS** address | Monthly free grant |
| Container App | `wealth-api` | Spring Boot, **internal only** (reached as `http://wealth-api`) | Monthly free grant |
| Container App | `wealth-iam` | Keycloak sign-in, public HTTPS | Monthly free grant |
| PostgreSQL Flexible Server | `wealth-pg-xxxxx` | B1ms, 32 GB, v16, databases `wealth` + `keycloak` | Free 12 months (free account) |
| Entra app, service principal, federated credential, role | `wealth-github-deploy` | Lets GitHub Actions deploy via OIDC (Contributor on the resource group only) | Free |

Images live in **GitHub Container Registry** (free), not Azure Container Registry.

**Free grant for Container Apps** (per subscription, shared with any other
container apps you have): 180,000 vCPU-seconds, 360,000 GiB-seconds and
2 million requests per month. The three apps use 1.25 vCPU and 2.5 GiB while
awake, so about **40 hours of active use per month** cost nothing. Apps scale to
zero about 5 minutes after the last request. Close the browser tab after a
demo: an open tab polls for unread messages every 30 seconds and keeps the
apps awake.

**Free PostgreSQL** (12 months on a free account): 750 hours of Burstable
B1ms, 32 GB storage and 32 GB backup per month, enough for **one** server
running all month. Check usage in the portal: Subscriptions > your
subscription > *View all free services*.

**Cold start:** the first visit after a quiet period takes 30–90 seconds while
Keycloak and Spring Boot start.

## Settings (`terraform.tfvars`)

| Variable | Our value | Meaning |
|---|---|---|
| `subscription_id` | your subscription | Where to deploy |
| `location` | `germanywestcentral` | Resource group region |
| `apps_location` | `northeurope` | Container Apps region (empty = `location`) |
| `db_location` | `northeurope` | PostgreSQL region (empty = `location`) |
| `github_repo` | `joinsrikanth945/wealth-portal` | Images and deploy workflow |
| `load_demo_data` | `true` | Load demo clients, portfolios and orders |
| `ghcr_username` / `ghcr_token` | not set | Only for private GHCR packages |
| `my_ip` | not set | Your IP, to reach the database from your PC |

`terraform.tfvars` and `terraform.tfstate` contain passwords and are never
committed; `.terraform.lock.hcl` is committed.

## First-time setup

Prerequisites: Terraform, Azure CLI, `az login`, the providers registered
(`Microsoft.App`, `Microsoft.DBforPostgreSQL`, `Microsoft.OperationalInsights`,
`Microsoft.ManagedIdentity`), and the code pushed to GitHub so the **Release**
workflow has published the images.

1. **Make the three images public** on GitHub: profile > Packages >
   `wealth-backend`, `wealth-frontend`, `wealth-keycloak` > Package settings >
   Change visibility > Public.
2. **Create the infrastructure** (10–15 minutes):
   ```powershell
   cd D:\Projects\wealth-portal\infra
   Copy-Item terraform.tfvars.example terraform.tfvars
   terraform init
   terraform plan -out tfplan
   terraform apply tfplan
   terraform output web_url
   ```
3. **Connect GitHub**: add the four values from `terraform output github_variables`
   as repository **variables** (not secrets): Settings > Secrets and variables >
   Actions > Variables > New repository variable.
4. Push to `main`. CI → Release → *Deploy to Azure Container Apps*; the job
   summary shows the live address.

## Everyday commands

```powershell
# Which commit is live (the image tag is the commit SHA)
az containerapp show -g rg-wealth-portal -n wealth-web --query "properties.template.containers[0].image" -o tsv

# Live logs
az containerapp logs show -g rg-wealth-portal -n wealth-api --follow
az containerapp logs show -g rg-wealth-portal -n wealth-iam --tail 100

# Pause / resume the database (no compute hours used while stopped; Azure restarts it after 7 days)
az postgres flexible-server stop  -g rg-wealth-portal -n (terraform output -raw postgres_host).Split('.')[0]
az postgres flexible-server start -g rg-wealth-portal -n (terraform output -raw postgres_host).Split('.')[0]

# Keycloak admin console
terraform output keycloak_admin_console
terraform output -raw keycloak_admin_password

# Remove everything
terraform destroy
```

## Problems we hit, and the fixes

| Error | Cause | Fix |
|---|---|---|
| `AKSCapacityHeavyUsage` / *Creating a new cluster is unavailable at this time in region …* when creating `wealth-env` | Azure has no Container Apps capacity in that region at the moment | Set `apps_location = "northeurope"` (or `swedencentral`, `francecentral`). Delete the failed environment first: `az containerapp env delete -n wealth-env -g rg-wealth-portal --yes` |
| `ParameterOutOfRange: The value of the 'Version' should be in:[]` when creating PostgreSQL | PostgreSQL isn't offered in that region to this subscription (common for free accounts) | Check: `az postgres flexible-server list-skus --location northeurope --query "[].supportedServerVersions[].name" -o tsv`, then set `db_location` to a region that lists versions |
| `… already exists - to be managed via Terraform this resource needs to be imported` | A previous run failed after Azure had started creating it | Delete the failed resource, or `terraform import <address> <azure id>`, then plan and apply again |
| Deploy job: `AADSTS700213: No matching federated identity record found for presented assertion subject 'repo:owner@123/repo@456:ref:refs/heads/main'` | GitHub sends the OIDC subject with numeric owner/repo IDs | Put that exact subject in the `subject` of `azuread_application_federated_identity_credential.github_main` in `main.tf`, then plan and apply |
| Container app `ImagePullBackOff` | Images not pushed yet, or GHCR package private | Wait for Release; make the packages public |
| Sign-in page: *Invalid parameter: redirect_uri* | Realm imported before `WEB_URL` was right | Keycloak admin > realm `wealth` > Clients > `wealth-portal`: add `https://wealth-web…/*` to redirect URIs and the address to web origins |
| 502 or blank page for a minute | An app is waking up from zero | Refresh after 30–60 seconds |

## Before sharing the link

The demo users (`rm1`, `client1`, `admin1`) have passwords published in the
README and no two-factor sign-in. Before sharing the URL:

1. Open the Keycloak admin console (`terraform output keycloak_admin_console`),
   user `admin`, password from `terraform output -raw keycloak_admin_password`.
2. Switch to the `wealth` realm > Users > each user > Credentials > Reset
   password (Temporary: off).
3. Clients > delete `wealth-api-tests` (it allows password-only sign-in).
4. Optional: Authentication > Required actions > enable *Configure OTP*.
