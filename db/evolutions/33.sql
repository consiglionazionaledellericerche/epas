# ---!Ups

ALTER TABLE total_overtime 
  ADD COLUMN office_id bigint,
  ADD CONSTRAINT totalovertime_office_key FOREIGN KEY (office_id) REFERENCES office (id);
    
UPDATE total_overtime SET office_id = null;

# ---!Downs

ALTER TABLE total_overtime
  DROP CONSTRAINT totalovertime_office_key,
  DROP COLUMN office_id;

