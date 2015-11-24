# ---!Ups

ALTER TABLE contracts
  ADD COLUMN source_date date,
  ADD COLUMN source_permission_used integer,
  ADD COLUMN source_recovery_day_used integer,
  ADD COLUMN source_remaining_minutes_current_year integer,
  ADD COLUMN source_remaining_minutes_last_year integer,
  ADD COLUMN source_vacation_current_year_used integer,
  ADD COLUMN source_vacation_last_year_used integer;

ALTER TABLE initialization_times
  ADD COLUMN permissionused integer,
  ADD COLUMN recoverydayused integer,
  ADD COLUMN vacationcurrentyearused integer,
  ADD COLUMN vacationlastyearused integer;
 
ALTER TABLE initialization_times_history
  ADD COLUMN permissionused integer,
  ADD COLUMN recoverydayused integer,
  ADD COLUMN vacationcurrentyearused integer,
  ADD COLUMN vacationlastyearused integer;

CREATE SEQUENCE seq_contract_year_recap
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE contract_year_recap
(
  id bigint NOT NULL DEFAULT nextval('seq_contract_year_recap'::regclass),
  has_source boolean,
  permission_used integer,
  recovery_day_used integer,
  remaining_minutes_current_year integer,
  remaining_minutes_last_year integer,
  vacation_current_year_used integer,
  vacation_last_year_used integer,
  year integer,
  contract_id bigint NOT NULL,
  CONSTRAINT contract_year_recap_pkey PRIMARY KEY (id),
  CONSTRAINT fkbf232f8afb1f039e FOREIGN KEY (contract_id)
      REFERENCES contracts (id) 
);


# ---!Downs

DROP TABLE contract_year_recap;
DROP SEQUENCE seq_contract_year_recap;

ALTER TABLE contracts
  DROP COLUMN source_date,
  DROP COLUMN source_permission_used,
  DROP COLUMN source_recovery_day_used,
  DROP COLUMN source_remaining_minutes_current_year,
  DROP COLUMN source_remaining_minutes_last_year,
  DROP COLUMN source_vacation_current_year_used,
  DROP COLUMN source_vacation_last_year_used;

ALTER TABLE initialization_times
  DROP COLUMN permissionused,
  DROP COLUMN recoverydayused,
  DROP COLUMN vacationcurrentyearused,
  DROP COLUMN vacationlastyearused;
 
ALTER TABLE initialization_times_history
  DROP COLUMN permissionused,
  DROP COLUMN recoverydayused,
  DROP COLUMN vacationcurrentyearused,
  DROP COLUMN vacationlastyearused;
