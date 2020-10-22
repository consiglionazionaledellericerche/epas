# --- !Ups

ALTER TABLE organization_shift_time_table ADD COLUMN consider_every_slot BOOLEAN default true;
ALTER TABLE organization_shift_time_table_history ADD COLUMN consider_every_slot BOOLEAN;


# --- !Downs
-- non è necessaria una down
