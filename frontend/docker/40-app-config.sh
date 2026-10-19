#!/bin/sh
# Writes /app-config.json from environment variables when the container starts,
# so one image works locally and on Azure. Without KEYCLOAK_URL the file baked
# into the image (localhost settings) is kept.
set -eu

if [ -n "${KEYCLOAK_URL:-}" ]; then
  cat > /usr/share/nginx/html/app-config.json <<EOF
{
  "apiBaseUrl": "/api",
  "keycloak": {
    "url": "${KEYCLOAK_URL}",
    "realm": "${KEYCLOAK_REALM:-wealth}",
    "clientId": "${KEYCLOAK_CLIENT_ID:-wealth-portal}"
  }
}
EOF
  echo "app-config.json written for ${KEYCLOAK_URL}"
fi
