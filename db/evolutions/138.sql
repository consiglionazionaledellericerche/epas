# --- !Ups

ALTER TABLE shift_time_table ADD COLUMN calculation_type TEXT;
UPDATE shift_time_table SET calculation_type = 'standard_CNR';

# --- !Downs
-- non è necessaria una down