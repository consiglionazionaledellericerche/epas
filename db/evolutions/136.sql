# --- !Ups

ALTER TABLE general_setting ADD COLUMN only_meal_ticket BOOLEAN;
ALTER TABLE general_setting_history ADD COLUMN only_meal_ticket BOOLEAN;

UPDATE general_setting SET only_meal_ticket = false;

# --- !Downs

ALTER TABLE general_setting_history DROP COLUMN only_meal_ticket;
ALTER TABLE general_setting DROP COLUMN only_meal_ticket;