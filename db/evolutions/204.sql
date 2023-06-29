# --- !Ups

ALTER TABLE person_days ADD COLUMN note TEXT; 
ALTER TABLE person_days_history ADD COLUMN note TEXT;

# --- !Downs

ALTER TABLE person_days DROP COLUMN note; 
ALTER TABLE person_days_history DROP COLUMN note;