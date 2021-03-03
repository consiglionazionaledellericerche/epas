# --- !Ups
ALTER TABLE certificated_data_history ADD COLUMN meal_ticket_sent text;

ALTER TABLE certificated_data_history DROP COLUMN mealticket_sent;

ALTER TABLE certificated_data ADD COLUMN meal_ticket_sent text;

ALTER TABLE certificated_data DROP COLUMN mealticket_sent;

# ---!Downs

ALTER TABLE certificated_data_history ADD COLUMN mealticket_sent integer;

ALTER TABLE certificated_data_history DROP COLUMN meal_ticket_sent;

ALTER TABLE certificated_data ADD COLUMN mealticket_sent integer;

ALTER TABLE certificated_data DROP COLUMN meal_ticket_sent;
