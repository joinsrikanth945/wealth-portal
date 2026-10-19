# Keycloak for the cloud: production mode, PostgreSQL, realm baked in.
# (Locally docker-compose.yml still runs the plain image in dev mode.)
#
# The realm file uses ${WEB_URL}; Keycloak fills it from the environment
# variable when it imports the realm, so the cloud web address is allowed
# as a sign-in redirect.
FROM quay.io/keycloak/keycloak:26.8 AS builder
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
# Pre-build the server so each cold start skips this step.
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY wealth-realm.json /opt/keycloak/data/import/wealth-realm.json
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
