# ---!Ups

ALTER TABLE shift_type_history
  ADD COLUMN shift_time_table_id bigint;

# ---!Downs

ALTER TABLE shift_type_history DROP COLUMN shift_time_table_id;