# --- !Ups

ALTER TABLE person_children ADD COLUMN externalId TEXT;
ALTER TABLE person_children ADD COLUMN updated_at TIMESTAMP default now();

# --- !Downs

ALTER TABLE person_children DROP COLUMN externalId;
ALTER TABLE person_children DROP COLUMN updated_at;
