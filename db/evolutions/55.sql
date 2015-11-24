# ---!Ups

ALTER TABLE absences ADD COLUMN justified_minutes INTEGER;
ALTER TABLE absences_history ADD COLUMN justified_minutes INTEGER;

ALTER TABLE working_time_type_days ADD COLUMN ticket_afternoon_threshold INTEGER;
ALTER TABLE working_time_type_days_history ADD COLUMN ticket_afternoon_threshold INTEGER;
ALTER TABLE working_time_type_days ADD COLUMN ticket_afternoon_working_time INTEGER;
ALTER TABLE working_time_type_days_history ADD COLUMN ticket_afternoon_working_time INTEGER;



# ---!Downs

ALTER TABLE absences DROP COLUMN justified_minutes;
ALTER TABLE absences_history DROP COLUMN justified_minutes;

ALTER TABLE working_time_type_days DROP COLUMN ticket_afternoon_threshold;
ALTER TABLE working_time_type_days_history DROP COLUMN ticket_afternoon_threshold;
ALTER TABLE working_time_type_days DROP COLUMN ticket_afternoon_working_time;
ALTER TABLE working_time_type_days_history DROP COLUMN ticket_afternoon_working_time;