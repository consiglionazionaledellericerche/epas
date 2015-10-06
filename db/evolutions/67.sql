# ---!Ups

ALTER TABLE working_time_types ADD COLUMN horizontal BOOLEAN;
ALTER TABLE working_time_types_history ADD COLUMN horizontal BOOLEAN;

update working_time_type_days set ticket_afternoon_threshold = 0 where ticket_afternoon_threshold is null;
update working_time_type_days set ticket_afternoon_working_time = 0 where ticket_afternoon_working_time is null;
update working_time_type_days_history set ticket_afternoon_threshold = 0 where ticket_afternoon_threshold is null;
update working_time_type_days_history set ticket_afternoon_working_time = 0 where ticket_afternoon_working_time is null;



# ---!Downs

ALTER TABLE working_time_types DROP COLUMN horizontal;
ALTER TABLE working_time_types_history DROP COLUMN horizontal;
