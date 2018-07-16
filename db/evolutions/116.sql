# ---!Ups

ALTER TABLE absence_types ADD COLUMN minimum_time INT;
ALTER TABLE absence_types ADD COLUMN percentage_time INT;

ALTER TABLE absence_types_history ADD COLUMN minimum_time INT;
ALTER TABLE absence_types_history ADD COLUMN percentage_time INT;

UPDATE absence_types SET code = '661MO' where code = '661M';
UPDATE absence_types_history SET code = '661MO' where code = '661M';

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN minimum_time;
ALTER TABLE absence_types_history DROP COLUMN percentage_time;

ALTER TABLE absence_types DROP COLUMN minimum_time;
ALTER TABLE absence_types DROP COLUMN percentage_time;

UPDATE absence_types SET code = '661M' where code = '661MO';
UPDATE absence_types_history SET code = '661M' where code = '661MO';
