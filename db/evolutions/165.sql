# --- !Ups

ALTER TABLE person_children ADD COLUMN external_id TEXT;
ALTER TABLE person_children ADD COLUMN updated_at TIMESTAMP default now();

ALTER TABLE person_children_history ADD COLUMN external_id TEXT;

# --- !Downs

ALTER TABLE person_children DROP COLUMN external_id;
ALTER TABLE person_children_history DROP COLUMN external_id;
ALTER TABLE person_children DROP COLUMN updated_at;
