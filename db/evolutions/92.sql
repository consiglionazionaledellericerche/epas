# ---!Ups

ALTER TABLE shift_type ADD COLUMN entrance_tolerance INT;
ALTER TABLE shift_type ADD COLUMN exit_tolerance INT;
ALTER TABLE shift_type ADD COLUMN hour_tolerance INT;
ALTER TABLE shift_type ADD COLUMN break_in_shift_enabled BOOLEAN;
ALTER TABLE shift_type ADD COLUMN break_in_shift INT;

ALTER TABLE shift_type_history ADD COLUMN entrance_tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN exit_tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN hour_tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN break_in_shift_enabled BOOLEAN;
ALTER TABLE shift_type_history ADD COLUMN break_in_shift INT;

ALTER TABLE person_shift DROP COLUMN jolly;

ALTER TABLE person_shift_shift_type ADD COLUMN jolly BOOLEAN;

UPDATE shift_type SET entrance_tolerance = 0, exit_tolerance = 0, hour_tolerance = 0, break_in_shift = 0, break_in_shift_enabled = false;
UPDATE person_shift_shift_type SET jolly = false;


# ---!Downs

ALTER TABLE shift_type_history DROP COLUMN hour_tolerance;
ALTER TABLE shift_type_history DROP COLUMN entrance_tolerance;
ALTER TABLE shift_type_history DROP COLUMN exit_tolerance;
ALTER TABLE shift_type_history DROP COLUMN break_in_shift_enabled;
ALTER TABLE shift_type_history DROP COLUMN break_in_shift;

ALTER TABLE shift_type DROP COLUMN hour_tolerance;
ALTER TABLE shift_type DROP COLUMN entrance_tolerance;
ALTER TABLE shift_type DROP COLUMN exit_tolerance;
ALTER TABLE shift_type DROP COLUMN break_in_shift_enabled;
ALTER TABLE shift_type DROP COLUMN break_in_shift;

ALTER TABLE person_shift_shift_type DROP COLUMN jolly;
ALTER TABLE person_shift ADD COLUMN jolly BOOLEAN;

UPDATE person_shift SET jolly = FALSE;