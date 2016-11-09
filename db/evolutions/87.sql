# ---!Ups

ALTER TABLE shift_categories_history ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories_history ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD COLUMN office_id BIGINT;
ALTER TABLE shift_categories ADD COLUMN disabled BOOLEAN;
ALTER TABLE shift_categories ADD FOREIGN KEY (office_id) REFERENCES office(id);

UPDATE shift_categories SET disabled = false;

# ---!Downs

ALTER TABLE shift_categories DROP CONSTRAINT shift_categories_office_id_fkey;
ALTER TABLE shift_categories DROP COLUMN office_id;
ALTER TABLE shift_categories DROP COLUMN disabled;


ALTER TABLE shift_categories_history DROP COLUMN office_id;
ALTER TABLE shift_categories_history DROP COLUMN disabled;
