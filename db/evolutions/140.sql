# --- !Ups

ALTER TABLE working_time_types ADD COLUMN enable_adjustment_for_quantity BOOLEAN DEFAULT true;
ALTER TABLE working_time_types_history ADD COLUMN enable_adjustment_for_quantity BOOLEAN;

# --- !Downs
-- non è necessaria una down