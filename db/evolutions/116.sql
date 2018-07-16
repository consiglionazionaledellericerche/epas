# ---!Ups

ALTER TABLE absences ADD COLUMN minimum_time INT;
ALTER TABLE absences ADD COLUMN percentage_time INT;

ALTER TABLE absences_history ADD COLUMN minimum_time DATE;
ALTER TABLE absences_history ADD COLUMN percentage_time INT;

UPDATE absence_types SET code = '661MO' where code = '661M';
UPDATE absence_types_history SET code = '661MO' where code = '661M';

# ---!Downs

ALTER TABLE absences_history DROP COLUMN minimum_time;
ALTER TABLE absences_history DROP COLUMN percentage_time;

ALTER TABLE absences DROP COLUMN minimum_time;
ALTER TABLE absences DROP COLUMN percentage_time;

UPDATE absence_types SET code = '661M' where code = '661MO';
UPDATE absence_types_history SET code = '661M' where code = '661MO';
