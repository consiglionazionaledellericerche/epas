# ---!Ups

ALTER TABLE shift_type ADD COLUMN tolerance INT;
ALTER TABLE shift_type ADD COLUMN hour_tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN hour_tolerance INT;
UPDATE shift_type SET tolerance = 0, hour_tolerance = 0;



# ---!Downs

ALTER TABLE shift_type_history DROP COLUMN hour_tolerance;
ALTER TABLE shift_type DROP COLUMN hour_tolerance;
ALTER TABLE shift_type DROP COLUMN tolerance;
ALTER TABLE shift_type_history DROP COLUMN tolerance;