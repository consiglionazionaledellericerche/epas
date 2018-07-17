# ---!Ups

ALTER TABLE absence_types ADD COLUMN minimum_time INT;
ALTER TABLE absence_types ADD COLUMN maximum_time INT;
ALTER TABLE absence_types ADD COLUMN percentage_time INT;
ALTER TABLE absence_types ADD COLUMN no_overtime BOOLEAN DEFAULT FALSE;

ALTER TABLE absence_types_history ADD COLUMN minimum_time INT;
ALTER TABLE absence_types_history ADD COLUMN maximum_time INT;
ALTER TABLE absence_types_history ADD COLUMN percentage_time INT;
ALTER TABLE absence_types_history ADD COLUMN no_overtime BOOLEAN DEFAULT FALSE;

UPDATE absence_types SET code = '661MO' where code = '661M';
UPDATE absence_types_history SET code = '661MO' where code = '661M';

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN no_overtime;
ALTER TABLE absence_types_history DROP COLUMN minimum_time;
ALTER TABLE absence_types_history DROP COLUMN maximum_time;
ALTER TABLE absence_types_history DROP COLUMN percentage_time;

ALTER TABLE absence_types DROP COLUMN no_overtime;
ALTER TABLE absence_types DROP COLUMN minimum_time;
ALTER TABLE absence_types DROP COLUMN maximum_time;
ALTER TABLE absence_types DROP COLUMN percentage_time;

UPDATE absence_types SET code = '661M' where code = '661MO';
UPDATE absence_types_history SET code = '661M' where code = '661MO';
