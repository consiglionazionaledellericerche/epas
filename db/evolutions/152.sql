# --- !Ups

ALTER TABLE absence_requests ADD COLUMN hours INTEGER;
ALTER TABLE absence_requests ADD COLUMN minutes INTEGER;

ALTER TABLE absence_requests_history ADD COLUMN hours INTEGER;
ALTER TABLE absence_requests_history ADD COLUMN minutes INTEGER;

# --- !Downs

ALTER TABLE absence_requests_history DROP COLUMN hours;
ALTER TABLE absence_requests DROP COLUMN hours;
ALTER TABLE absence_requests_history DROP COLUMN minutes;
ALTER TABLE absence_requests DROP COLUMN minutes;