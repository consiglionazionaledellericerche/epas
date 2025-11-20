# --- !Ups

ALTER TABLE absence_requests ADD COLUMN absence_code TEXT DEFAULT NULL;

ALTER TABLE absence_requests_history ADD COLUMN absence_code TEXT DEFAULT NULL;

# --- !Downs
# -- Non è necessaria una down