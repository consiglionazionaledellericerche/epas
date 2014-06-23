# --- !Ups

ALTER TABLE working_time_types 
  ADD COLUMN office_id bigint,
  ADD CONSTRAINT wtt_office_key FOREIGN KEY (office_id) REFERENCES office (id);
    
UPDATE working_time_types SET office_id = null;

ALTER TABLE working_time_types_history 
  ADD COLUMN office_id bigint;

# ---!Downs

ALTER TABLE working_time_types
  DROP CONSTRAINT wtt_office_key,
  DROP COLUMN office_id;
 
ALTER TABLE working_time_types_history 
  DROP COLUMN office_id;  