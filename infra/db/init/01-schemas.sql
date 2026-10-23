-- One login role and one schema per service (local-dev passwords only)
CREATE ROLE tenant_svc LOGIN PASSWORD 'tenant_local_pass';
CREATE ROLE user_svc   LOGIN PASSWORD 'user_local_pass';

CREATE SCHEMA tenant AUTHORIZATION tenant_svc;
CREATE SCHEMA users  AUTHORIZATION user_svc;

ALTER ROLE tenant_svc SET search_path = tenant;
ALTER ROLE user_svc   SET search_path = users;