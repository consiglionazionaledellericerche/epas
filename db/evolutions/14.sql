# --- !Ups

ALTER TABLE person_days
  DROP COLUMN is_time_at_work_auto_certificated,
  DROP COLUMN modification_type;
  
ALTER TABLE person_days_history
  DROP COLUMN is_time_at_work_auto_certificated,
  DROP COLUMN modification_type;
  
# ---!Downs

ALTER TABLE person_days
  ADD COLUMN is_time_at_work_auto_certificated boolean,
  ADD COLUMN Modification_type  character varying(255);
  
ALTER TABLE person_days_history
  ADD COLUMN is_time_at_work_auto_certificated boolean,
  ADD COLUMN Modification_type  character varying(255);
