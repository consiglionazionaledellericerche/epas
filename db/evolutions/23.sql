# ---!Ups

ALTER TABLE working_time_types ADD COLUMN meal_ticket_enabled boolean;

ALTER TABLE	working_time_types_history ADD COLUMN meal_ticket_enabled boolean;

UPDATE working_time_types SET meal_ticket_enabled = true;

UPDATE working_time_types_history SET meal_ticket_enabled = true;

# ---!Downs

ALTER TABLE working_time_types_history DROP COLUMN meal_ticket_enabled;

ALTER TABLE working_time_types DROP COLUMN meal_ticket_enabled;