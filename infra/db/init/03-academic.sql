CREATE ROLE academic_svc LOGIN PASSWORD 'academic_local_pass';
CREATE SCHEMA academic AUTHORIZATION academic_svc;
ALTER ROLE academic_svc SET search_path = academic;
