# ---!Ups

ALTER TABLE absences ADD COLUMN note TEXT;

ALTER TABLE absences_history ADD COLUMN note TEXT;

# ---!Downs

ALTER TABLE absences_history DROP COLUMN note;

ALTER TABLE absences DROP COLUMN note;
