# --- !Ups

ALTER TABLE meal_ticket ADD COLUMN block_type TEXT;
ALTER TABLE meal_ticket_history ADD COLUMN block_type TEXT;

UPDATE meal_ticket SET block_type = 'papery';

# --- !Downs

ALTER TABLE meal_ticket_history DROP COLUMN block_type;
ALTER TABLE meal_ticket DROP COLUMN block_type;