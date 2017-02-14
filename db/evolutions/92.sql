# ---!Ups

ALTER TABLE shift_type ADD COLUMN tolerance INT;
ALTER TABLE shift_type_history ADD COLUMN tolerance INT;

# ---!Downs

ALTER TABLE shift_type DROP COLUMN tolerance;
ALTER TABLE shift_type_history DROP COLUMN tolerance;