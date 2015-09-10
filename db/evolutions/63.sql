# ---!Ups

ALTER TABLE contracts RENAME COLUMN source_date TO source_date_residual;
ALTER TABLE contracts ADD COLUMN source_date_meal_ticket date;

UPDATE contracts SET source_date_meal_ticket = source_date_residual WHERE source_date_residual IS NOT null;

# ---!Downs

ALTER TABLE contracts RENAME COLUMN source_date_residual TO source_date;
ALTER TABLE contracts DROP COLUMN source_date_meal_ticket;
