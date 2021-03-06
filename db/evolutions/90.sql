# ---!Ups

ALTER TABLE shift_type ALTER id SET DEFAULT nextval('seq_shift_type'::regclass);
ALTER TABLE person_shift ALTER id SET DEFAULT nextval('seq_person_shift'::regclass);
ALTER TABLE person_shift_shift_type ALTER id SET DEFAULT nextval('seq_person_shift_shift_type'::regclass);

ALTER TABLE shift_categories_history ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories_history ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD FOREIGN KEY (office_id) REFERENCES office(id);
ALTER TABLE person_shift ADD COLUMN disabled BOOLEAN;

UPDATE person_shift SET disabled = false;
UPDATE shift_categories SET disabled = false;

UPDATE shift_time_table SET end_morning_lunch_time = '23:59:59' where id in (6,7,8,9);
UPDATE shift_time_table SET start_afternoon_lunch_time = '00:00:00' where id in (6,7,8,9);

# ---!Downs

ALTER TABLE shift_categories DROP CONSTRAINT shift_categories_office_id_fkey;
ALTER TABLE shift_categories DROP COLUMN office_id;
ALTER TABLE shift_categories DROP COLUMN disabled;

ALTER TABLE shift_categories_history DROP COLUMN office_id;
ALTER TABLE shift_categories_history DROP COLUMN disabled;

ALTER TABLE person_shift DROP COLUMN disabled;