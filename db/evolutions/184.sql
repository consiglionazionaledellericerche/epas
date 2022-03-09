# --- !Ups

ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_id TEXT;
ALTER TABLE users_history ADD COLUMN IF NOT EXISTS keycloak_id TEXT;

# --- !Downs

ALTER TABLE users DROP COLUMN IF EXISTS keycloak_id;
ALTER TABLE users_history DROP COLUMN IF EXISTS keycloak_id;