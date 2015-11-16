# ---!Ups

ALTER TABLE contracts ADD COLUMN source_by_admin boolean;
UPDATE contracts set source_by_admin = true;

DROP TABLE stamp_profiles;
DROP TABLE contract_year_recap;
DROP TABLE initialization_absences;
DROP TABLE initialization_absences_history;
DROP TABLE initialization_times;
DROP TABLE initialization_times_history;

# ---!Downs

ALTER TABLE contracts DROP COLUMN source_by_admin;
