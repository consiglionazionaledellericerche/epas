# ---!Ups

ALTER TABLE shift_type ADD COLUMN enable_holidays boolean;
ALTER TABLE shift_type_history ADD COLUMN enable_holidays boolean;

UPDATE shift_type SET enable_holidays = false;

# ---!Downs

ALTER TABLE shift_type_history DROP COLUMN enable_holidays;
ALTER TABLE shift_type DROP COLUMN enable_holidays;