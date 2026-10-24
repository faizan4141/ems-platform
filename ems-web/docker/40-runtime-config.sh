#!/bin/sh
set -eu

: "${API_BASE_URL:?API_BASE_URL must be set}"
: "${KEYCLOAK_URL:?KEYCLOAK_URL must be set}"
: "${KEYCLOAK_REALM:?KEYCLOAK_REALM must be set}"
: "${KEYCLOAK_CLIENT_ID:?KEYCLOAK_CLIENT_ID must be set}"

cat > /usr/share/nginx/html/config.js <<EOF
window.__EMS_CONFIG__ = {
  API_BASE_URL: "${API_BASE_URL}",
  KEYCLOAK_URL: "${KEYCLOAK_URL}",
  KEYCLOAK_REALM: "${KEYCLOAK_REALM}",
  KEYCLOAK_CLIENT_ID: "${KEYCLOAK_CLIENT_ID}"
};
EOF
echo "Wrote runtime config.js"