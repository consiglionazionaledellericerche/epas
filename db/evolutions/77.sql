# ---!Ups

ALTER TABLE vacation_periods RENAME COLUMN begin_from TO begin_date;
ALTER TABLE vacation_periods RENAME COLUMN end_to TO end_date;

ALTER TABLE vacation_periods_history RENAME COLUMN begin_from TO begin_date;
ALTER TABLE vacation_periods_history RENAME COLUMN end_to TO end_date;

ALTER TABLE vacation_periods ALTER COLUMN begin_date SET NOT NULL;

# ---!Downs

ALTER TABLE vacation_periods RENAME COLUMN begin_date TO begin_from;
ALTER TABLE vacation_periods RENAME COLUMN end_date TO end_to;

ALTER TABLE vacation_periods_history RENAME COLUMN begin_date TO begin_from;
ALTER TABLE vacation_periods_history RENAME COLUMN end_date TO end_to;


