# --- !Ups
ALTER TABLE meal_ticket ADD COLUMN expire_date date;

ALTER TABLE meal_ticket_history ADD COLUMN expire_date date;

UPDATE meal_ticket SET expire_date = '2014-12-31';
UPDATE meal_ticket_history SET expire_date = '2014-12-31';

# ---!Downs
ALTER TABLE meal_ticket DROP COLUMN expire_date;

ALTER TABLE meal_ticket_history DROP COLUMN expire_date;
