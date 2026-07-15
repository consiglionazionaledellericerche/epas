# --- !Ups

ALTER TABLE general_setting ADD COLUMN disable_check_part_time_working_time BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN disable_check_part_time_working_time BOOLEAN DEFAULT FALSE;


# --- !Downs
# -- Non è necessaria una down