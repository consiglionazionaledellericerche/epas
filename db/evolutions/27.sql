# --- !Ups

ALTER TABLE working_time_types 
  ADD COLUMN disabled boolean;
    
UPDATE working_time_types SET disabled = false;

ALTER TABLE working_time_types_history 
  ADD COLUMN disabled boolean;

# ---!Downs

ALTER TABLE working_time_types
  DROP COLUMN disabled;
 
ALTER TABLE working_time_types_history 
  DROP COLUMN disabled;  