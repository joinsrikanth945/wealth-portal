# Deploy to Azure (free tier) with Terraform

Everything in this folder creates the cloud version of the portal in your
Azure subscription. One `terraform apply` builds it, one `terraform destroy`
removes it.

## What gets created

| Azure resource | Name | Purpose | Cost |
|---|---|---|---|
| Resource group | `rg-wealth-portal` | Holds everything below | Free |
| Container Apps environment | `wealth-env` | Shared runtime, Consumption plan, no log workspace | Free |
| Container App | `wealth-web` | Angular + Nginx, **public HTTPS** address | Monthly free grant |
| Container App | `wealth-api` | Spring Boot, **internal only** | Monthly free grant |
| Container App | `wealth-iam` | Keycloak sign-in, public HTTPS | Monthly free grant |
| PostgreSQL Flexible Server | `wealth-pg-xxxxx` | B1ms, 32 GB, databases `wealth` + `keycloak` | Free 12 months (free account) |
| Entra app registration + role | `wealth-github-deploy` | Lets GitHub Actions deploy via OIDC | Free |

Images live in **GitHub Container Registry** (free), not Azure Container Registry.

**Free grant for Container Apps:** 180,000 vCPU-seconds, 360,000 GiB-seconds
and 2 million requests per month. All three apps together use 1.25 vCPU and
2.5 GiB while awake, so that is **about 40 hours of active use per month** at
no cost. Apps scale to zero about 5 minutes after the last request and cost
nothing while asleep. Close the browser tab when you finish a demo: an open
tab polls for unread messages and keeps the apps awake.

**Cold start:** after a quiet period the first visit takes about 30–90 seconds
(Keycloak and Spring Boot start up).

## One-time setup

Prerequisites: Terraform, Azure CLI, `az login` done, and the code pushed to
GitHub so the **Release** workflow has published the images.

### 1. Make the three images public on GitHub

GitHub > your profile > **Packages** > for each of `wealth-backend`,
`wealth-frontend`, `wealth-keycloak`: **Package settings** > **Change
visibility** > **Public**. (Or keep them private and set `ghcr_username` /
`ghcr_token` in `terraform.tfvars`.)

### 2. Create the infrastructure

```powershell
cd D:\Projects\wealth-portal\infra
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform plan -out tfplan
terraform apply tfplan
```

Takes about 10–15 minutes (PostgreSQL is the slow part). At the end:

```powershell
terraform output web_url
terraform output github_variables
terraform output -raw keycloak_admin_password
```

### 3. Let GitHub deploy

GitHub repo > **Settings** > **Secrets and variables** > **Actions** >
**Variables** tab > add the four values from `github_variables`:
`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`,
`AZURE_RESOURCE_GROUP`. From now on every green CI run on `main` deploys.

## Everyday commands

```powershell
# Pause the database (free tier hours are not used while stopped; it auto-starts after 7 days)
az postgres flexible-server stop  -g rg-wealth-portal -n (terraform output -raw postgres_host).Split('.')[0]
az postgres flexible-server start -g rg-wealth-portal -n (terraform output -raw postgres_host).Split('.')[0]

# Live logs of an app
az containerapp logs show -g rg-wealth-portal -n wealth-api --follow

# Remove everything
terraform destroy
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `LocationIsOfferRestricted` / PostgreSQL not available in the region | Set `location = "westeurope"` (or `northeurope`, `swedencentral`) in `terraform.tfvars` and apply again |
| Container app shows `ImagePullBackOff` / image not found | Release workflow hasn't pushed yet, or the package is private (step 1) |
| Sign-in page says *Invalid redirect URI* | The realm was imported before `WEB_URL` was correct. In the Keycloak admin console, add `https://wealth-web…/*` to the `wealth-portal` client's redirect URIs |
| Page shows 502 for a minute | The API is waking up; refresh after 30–60 seconds |

## Before sharing the link

The demo users (`rm1`, `client1`, `admin1`) have published passwords and no
two-factor sign-in. Before sharing the URL with anyone, change their passwords
in the Keycloak admin console (Users > user > Credentials) or re-enable OTP
(Authentication > Required actions > Configure OTP), and delete the
`wealth-api-tests` client.
