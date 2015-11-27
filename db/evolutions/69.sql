# ---!Ups

ALTER TABLE absence_types ADD CONSTRAINT absence_code_unique UNIQUE(code);

ALTER TABLE institutes ADD COLUMN cds text;
ALTER TABLE office ALTER COLUMN code SET DATA TYPE text USING code::text;
ALTER TABLE office ALTER COLUMN code SET NOT NULL;
ALTER TABLE office RENAME COLUMN code TO code_id;
ALTER TABLE office ADD COLUMN code text;

ALTER TABLE institutes_history ADD COLUMN cds text;
ALTER TABLE office_history  ALTER COLUMN code SET DATA TYPE text USING code::text;
ALTER TABLE office_history  RENAME COLUMN code TO code_id;
ALTER TABLE office_history  ADD COLUMN code text;

ALTER TABLE contract_stamp_profiles RENAME COLUMN start_from TO begin_date;
ALTER TABLE contract_stamp_profiles RENAME COLUMN end_to TO end_date;

ALTER TABLE contracts ADD COLUMN begin_date DATE;
ALTER TABLE contracts ADD COLUMN end_date DATE;

# ---!Downs

ALTER TABLE absence_types DROP CONSTRAINT absence_code_unique;

ALTER TABLE institutes DROP COLUMN cds;
ALTER TABLE office DROP COLUMN code;
ALTER TABLE office RENAME COLUMN code_id TO code;
ALTER TABLE office ALTER COLUMN code DROP NOT NULL;
ALTER TABLE office ALTER COLUMN code SET DATA TYPE integer USING code::integer;

ALTER TABLE institutes_history DROP COLUMN cds;
ALTER TABLE office_history DROP COLUMN code;
ALTER TABLE office_history RENAME COLUMN code_id TO code;
ALTER TABLE office_history ALTER COLUMN code SET DATA TYPE integer USING code::integer;

ALTER TABLE contract_stamp_profiles RENAME COLUMN begin_date TO start_from;
ALTER TABLE contract_stamp_profiles RENAME COLUMN end_date TO end_to;

ALTER TABLE contracts DROP COLUMN begin_date;
ALTER TABLE contracts DROP COLUMN end_date;