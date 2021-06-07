# --- !Ups

ALTER TABLE general_setting ADD COLUMN max_days_in_past_for_rest_stampings INTEGER default 90;
ALTER TABLE general_setting_history ADD COLUMN max_days_in_past_for_rest_stampings INTEGER;

# --- !Downs

ALTER TABLE general_setting DROP COLUMN max_days_in_past_for_rest_stampings;
ALTER TABLE general_setting_history DROP COLUMN max_days_in_past_for_rest_stampings;
