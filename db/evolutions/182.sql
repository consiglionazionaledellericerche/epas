# --- !Ups

ALTER TABLE absences ADD COLUMN updated_at TIMESTAMP default now();

# --- !Downs

ALTER TABLE absences DROP COLUMN updated_at;
