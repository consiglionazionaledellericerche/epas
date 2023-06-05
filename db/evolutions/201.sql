# --- !Ups

ALTER TABLE general_setting ADD COLUMN warning_insert_person BOOLEAN default true;
ALTER TABLE general_setting_history ADD COLUMN warning_insert_person BOOLEAN;

# --- !Downs

ALTER TABLE general_setting_history DROP COLUMN warning_insert_person;
ALTER TABLE general_setting DROP COLUMN warning_insert_person;