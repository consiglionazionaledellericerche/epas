# ---!Ups

ALTER TABLE absences ADD COLUMN justified_minutes INTEGER;
ALTER TABLE absences_history ADD COLUMN justified_minutes INTEGER;

# ---!Downs

ALTER TABLE absences DROP COLUMN justified_minutes;
ALTER TABLE absences_history DROP COLUMN justified_minutes;
