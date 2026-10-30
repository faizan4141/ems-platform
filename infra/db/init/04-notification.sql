CREATE ROLE notification_svc LOGIN PASSWORD 'notification_local_pass';
CREATE SCHEMA notification AUTHORIZATION notification_svc;
ALTER ROLE notification_svc SET search_path = notification;
