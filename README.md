# Wealth Portal

Wealth banking portal for clients and relationship managers.
Angular 22 · Spring Boot 4.1 (Java 25) · PostgreSQL 17 · Keycloak 26 · Docker.

**Phase 1 delivers:** Keycloak sign-in with two-factor (authenticator app),
role-based areas for Client, RM and Admin, the app shell and menus, a
"last sign-in" notice, user preferences, and an append-only audit trail.

---

## 1. Install once

| Tool | Version | Check with |
|---|---|---|
| Git | any recent | `git --version` |
| JDK (Eclipse Temurin) | **25** | `java -version` |
| Maven | **3.9+** | `mvn -v` |
| Node.js | **24 LTS** (Angular 22 needs 22.22.3+ or 24.15+) | `node -v` |
| Docker Desktop | recent | `docker compose version` |
| VS Code | recent | — |

Open the `wealth-portal` folder in VS Code and accept the recommended
extensions (Angular, Java, Spring Boot, Docker).

You also need an **authenticator app** on your phone (Google Authenticator,
Microsoft Authenticator, FreeOTP…) for the two-factor step.

## 2. Run locally (everyday development)

Three terminals, from the `wealth-portal` folder:

```bash
# Terminal 1 — database and Keycloak
docker compose up -d

# Terminal 2 — backend on http://localhost:8090
cd backend
mvn spring-boot:run

# Terminal 3 — frontend on http://localhost:4200
cd frontend
npm install
npm start
```

Open **http://localhost:4200**.

Keycloak takes 20–40 seconds to start the first time. If the login page
can't reach it, wait and refresh.

### Test users

| User | Password | Role | Lands on |
|---|---|---|---|
| `client1` | `Client#2026` | Client | `/client/home` |
| `rm1` | `Manager#2026` | Relationship manager | `/rm/home` |
| `admin1` | `Admin#2026x` | Administrator | `/admin/home` |

Two-factor is switched off for these demo users so demos are quick
(see `keycloak/README.txt` to switch it back on).

Try this: choose **Relationship manager** on the login page but sign in as
`client1` — you get a "this account isn't registered as a relationship
manager" page.

### Useful addresses

| What | Address |
|---|---|
| App (dev server) | http://localhost:4200 |
| API health | http://localhost:8090/actuator/health |
| Swagger UI (try every API) | http://localhost:8090/swagger-ui.html |
| Keycloak admin console | http://localhost:8180 (admin / admin) |
| PostgreSQL | localhost:5432, database `wealth`, user `wealth`, password `wealth` |

## 3. Run everything in containers

Closest to how it runs on Azure (no local Java or Node needed):

```bash
docker compose --profile app up --build -d
```

Open **http://localhost:8081**.

Stop everything with `docker compose --profile app down`.
Add `-v` to also wipe the database and Keycloak data.

## 4. Test the APIs

**In the browser:** open http://localhost:8090/swagger-ui.html, click
**Authorize**, then **Authorize** again in the popup and sign in as `rm1` or
`client1`. Every endpoint now has **Try it out**.

**In VS Code:** open `backend/requests.http` (needs the REST Client
extension). Click **Send Request** on the two sign-in requests at the top,
then on any other request — tokens are reused automatically.

### Demo data

Loaded by Flyway from `backend/src/main/resources/db/demo/R__demo_data.sql`:
21 instruments (shares, ETFs, funds, bonds, options, a future), 8 clients
(7 for `rm1`, 1 for another RM) with 9 portfolios, 12 months of performance
history and MiFID profiles in every state (valid, expired, missing, awaiting
the client, in progress). `client1` is Clara Dupont.

Editing that file reloads the demo data on the next backend start and
**resets** clients, portfolios and MiFID profiles. Production runs with
`FLYWAY_LOCATIONS=classpath:db/migration`, which skips it.

## 5. Automated tests

```bash
cd backend && mvn verify          # unit and web-layer tests
cd frontend && npm run test:ci    # Vitest
```

## Project layout

```
wealth-portal/
├── backend/              Spring Boot 4 API
│   └── src/main/java/com/yourbank/wealth/
│       ├── identity/     sign-in, current user, login history
│       ├── audit/        append-only audit trail
│       └── shared/       security (Keycloak roles), clock
├── frontend/             Angular 22 single-page app
│   └── src/app/
│       ├── core/         auth (Keycloak, guards, interceptors), config, API
│       ├── layout/       shell, menu, last sign-in dialog
│       └── features/     login, client, rm, admin, settings
├── keycloak/             realm: roles, test users, 2FA policy
├── .github/              CI, CodeQL, Dependabot
└── docker-compose.yml
```

## How sign-in works

1. Our login page asks *Client* or *Relationship manager*, then hands over to
   Keycloak's secure page. Angular never sees passwords.
2. Keycloak checks the password and the authenticator code, then returns the
   user to the app with a short-lived access token (5 minutes, refreshed
   automatically).
3. The Angular `authInterceptor` sends the token with every `/api` call.
4. Spring Boot checks the token's signature, issuer, audience (`wealth-api`)
   and expiry, and turns Keycloak roles into `ROLE_CLIENT`, `ROLE_RM`, etc.
5. URL rules: `/api/rm/**` needs RM or ADMIN, `/api/admin/**` needs ADMIN,
   `/api/client/**` needs CLIENT.

## GitHub setup

1. Create a repository called `wealth-portal` and push this folder.
2. **Settings → Branches → Add rule** for `main`: require a pull request and
   require the status checks *Backend*, *Frontend* and *Docker images build*.
3. **Settings → Code security**: turn on Dependabot alerts and secret scanning.

CI runs on every pull request; CodeQL scans weekly and on every change.
Deployment to Azure is added in phase 10.

## Troubleshooting

| Problem | Fix |
|---|---|
| Port 5432 already in use | You have a local PostgreSQL. Stop it, or change the left side of `"5432:5432"` and `DB_URL` |
| "Invalid redirect uri" from Keycloak | Use exactly http://localhost:4200 (not 127.0.0.1) |
| Backend 401 on every call | Keycloak was restarted with a new realm: sign out and in again |
| Changed `keycloak/wealth-realm.json` but nothing happens | The realm is imported only once: `docker compose down -v` then `up` again |
