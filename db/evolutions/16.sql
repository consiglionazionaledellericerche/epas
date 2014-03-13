# ---!Ups

ALTER TABLE persons
  DROP COLUMN working_time_type_id;
  
  
# ---!Downs
  
ALTER TABLE persons
  ADD COLUMN working_time_type_id bigint,
  ADD CONSTRAINT fkd78fcfbe35555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types (id);