# ---!Ups

ALTER TABLE person_months_recap ADD COLUMN fromDate date;
ALTER TABLE	person_months_recap ADD COLUMN toDate date;

# ---!Downs

ALTER TABLE person_months_recap DROP COLUMN fromDate;
ALTER TABLE person_months_recap DROP COLUMN toDate;
