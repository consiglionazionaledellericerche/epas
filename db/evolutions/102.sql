# ---!Ups

ALTER TABLE absences ADD COLUMN external_identifier BIGINT;
ALTER TABLE absences_history ADD COLUMN external_identifier BIGINT;


# ---!Downs

ALTER TABLE absences_history DROP COLUMN external_identifier;
ALTER TABLE absences DROP COLUMN external_identifier;

