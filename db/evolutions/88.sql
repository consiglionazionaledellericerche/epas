# ---!Ups

ALTER TABLE initialization_groups RENAME COLUMN initialization_date TO date;
ALTER TABLE initialization_groups ADD COLUMN units_input INT;
ALTER TABLE initialization_groups ADD COLUMN hours_input INT;
ALTER TABLE initialization_groups ADD COLUMN minutes_input INT;
ALTER TABLE initialization_groups ADD COLUMN average_week_time INT;

ALTER TABLE initialization_groups_history RENAME COLUMN initialization_date TO date;
ALTER TABLE initialization_groups_history ADD COLUMN units_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN hours_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN minutes_input INT;
ALTER TABLE initialization_groups_history ADD COLUMN average_week_time INT;

# ---!Downs

ALTER TABLE initialization_groups RENAME COLUMN date TO initialization_date;
ALTER TABLE initialization_groups DROP COLUMN units_input;
ALTER TABLE initialization_groups DROP COLUMN hours_input;
ALTER TABLE initialization_groups DROP COLUMN minutes_input;
ALTER TABLE initialization_groups DROP COLUMN average_week_time;

ALTER TABLE initialization_groups_history RENAME COLUMN date TO initialization_date;
ALTER TABLE initialization_groups_history DROP COLUMN units_input;
ALTER TABLE initialization_groups_history DROP COLUMN hours_input;
ALTER TABLE initialization_groups_history DROP COLUMN minutes_input;
ALTER TABLE initialization_groups_history DROP COLUMN average_week_time; 