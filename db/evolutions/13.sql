# --- !Ups

ALTER TABLE person_days 
  ADD COLUMN stamp_modification_type_id bigint,
  ADD CONSTRAINT fke69adb0135175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types (id);
    
UPDATE person_days SET stamp_modification_type_id = (select id from stamp_modification_types where code = modification_type);

ALTER TABLE person_days_history
  ADD COLUMN stamp_modification_type_id bigint;

# ---!Downs

ALTER TABLE person_days
  DROP CONSTRAINT fke69adb0135175bce,
  DROP COLUMN stamp_modification_type_id;
 
ALTER TABLE person_days_history
  DROP COLUMN stamp_modification_type_id;  