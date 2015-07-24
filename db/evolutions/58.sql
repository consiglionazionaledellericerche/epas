# ---!Ups

ALTER TABLE contracts ADD COLUMN source_by_admin boolean;
UPDATE contracts set source_by_admin = true;

# ---!Downs

ALTER TABLE contracts DROP COLUMN source_by_admin;
