# --- !Ups

ALTER TABLE contracts ADD COLUMN source_date_recovery_day DATE;
ALTER TABLE contracts_history ADD COLUMN source_date_recovery_day DATE;

-- Non è necessaria una down