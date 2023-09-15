# --- !Ups

ALTER TABLE general_setting ADD COLUMN max_months_in_past_for_absences INTEGER DEFAULT 12;
ALTER TABLE general_setting_history ADD COLUMN max_months_in_past_for_absences INTEGER;


# --- !Downs

ALTER TABLE general_setting DROP COLUMN max_months_in_past_for_absences;
ALTER TABLE general_setting_history DROP COLUMN max_months_in_past_for_absences;