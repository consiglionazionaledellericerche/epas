# --- !Ups
ALTER TABLE person_days ADD COLUMN out_part_time INTEGER NOT NULL DEFAULT 0;
ALTER TABLE person_days ADD COLUMN approved_out_part_time INTEGER NOT NULL DEFAULT 0;
ALTER TABLE person_days_history ADD COLUMN out_part_time INTEGER;
ALTER TABLE person_days_history ADD COLUMN approved_out_part_time INTEGER;

# --- !Downs
# -- Non è necessaria una down