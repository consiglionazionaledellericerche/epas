# ---!Ups

ALTER TABLE shift_type ADD COLUMN tolerance INT;
ALTER TABLE shift_type ADD COLUMN hour_tolerance INT;

ALTER TABLE shift_type_history ADD COLUMN tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN hour_tolerance INT;

ALTER TABLE person_shift DROP COLUMN jolly;

ALTER TABLE person_shift_shift_type ADD COLUMN jolly BOOLEAN;

UPDATE shift_type SET tolerance = 0, hour_tolerance = 0;
UPDATE person_shift_shift_type SET jolly = false;


# ---!Downs

ALTER TABLE shift_type_history DROP COLUMN hour_tolerance;
ALTER TABLE shift_type_history DROP COLUMN tolerance;

ALTER TABLE shift_type DROP COLUMN hour_tolerance;
ALTER TABLE shift_type DROP COLUMN tolerance;

ALTER TABLE person_shift_shift_type DROP COLUMN jolly;
ALTER TABLE person_shift ADD COLUMN jolly BOOLEAN;

UPDATE person_shift SET jolly = FALSE;