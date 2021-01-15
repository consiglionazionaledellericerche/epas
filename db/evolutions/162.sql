# --- !Ups

ALTER TABLE contract_stamp_profiles_history ADD end_date DATE;

ALTER TABLE persons_history ADD begin_date DATE;
ALTER TABLE persons_history ADD end_date DATE;


# --- !Downs

ALTER TABLE contract_stamp_profiles_history DROP COLUMN end_date;

ALTER TABLE persons_history DROP COLUMN begin_date;
ALTER TABLE persons_history DROP COLUMN end_date;
