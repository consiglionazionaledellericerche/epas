# --- !Ups

ALTER TABLE users ADD COLUMN keycloak_id TEXT;
ALTER TABLE users_history ADD COLUMN keycloak_id TEXT;

# --- !Downs

ALTER TABLE users DROP COLUMN keycloak_id;
ALTER TABLE users_history DROP COLUMN keycloak_id;