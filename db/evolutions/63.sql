# ---!Ups

ALTER TABLE contracts RENAME COLUMN source_date TO source_date_residual;
ALTER TABLE contracts ADD COLUMN source_date_meal_ticket date;

# ---!Downs

ALTER TABLE contracts RENAME COLUMN source_date_residual TO source_date;
ALTER TABLE contracts DROP COLUMN source_date_meal_ticket;