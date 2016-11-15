# ---!Ups

ALTER TABLE shift_categories_history ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories_history ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD FOREIGN KEY (office_id) REFERENCES office(id);

UPDATE shift_categories SET disabled = false;

ALTER TABLE shift_time_table ALTER COLUMN start_morning TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN end_morning TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN start_afternoon TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN end_afternoon TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN start_morning_lunch_time TYPE time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN end_morning_lunch_time TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN start_afternoon_lunch_time TYPE  time without time zone;
ALTER TABLE shift_time_table ALTER COLUMN end_afternoon_lunch_time TYPE  time without time zone;

# ---!Downs

ALTER TABLE shift_categories DROP CONSTRAINT shift_categories_office_id_fkey;
ALTER TABLE shift_categories DROP COLUMN office_id;
ALTER TABLE shift_categories DROP COLUMN disabled;


ALTER TABLE shift_categories_history DROP COLUMN office_id;
ALTER TABLE shift_categories_history DROP COLUMN disabled;
