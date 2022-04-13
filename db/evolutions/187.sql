# --- !Ups

ALTER TABLE meal_ticket DROP COLUMN quarter;
ALTER TABLE meal_ticket_history DROP COLUMN quarter;

# --- !Downs

ALTER TABLE meal_ticket ADD COLUMN quarter INTEGER;
ALTER TABLE meal_ticket_history ADD COLUMN quarter INTEGER;