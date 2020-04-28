# --- !Ups

ALTER TABLE person_children ADD COLUMN tax_code TEXT;
ALTER TABLE person_children_history ADD COLUMN tax_code TEXT;

# --- !Downs