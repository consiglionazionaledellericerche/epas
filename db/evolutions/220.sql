# --- !Ups

ALTER TABLE general_setting ADD COLUMN disable_meal_time_in_working_time_creation BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN disable_meal_time_in_working_time_creation BOOLEAN DEFAULT FALSE;


# --- !Downs
# -- Non è necessaria una down