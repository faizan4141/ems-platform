CREATE ROLE keycloak_svc LOGIN PASSWORD 'keycloak_local_pass';
CREATE SCHEMA keycloak AUTHORIZATION keycloak_svc;
ALTER ROLE keycloak_svc SET search_path = keycloak;
