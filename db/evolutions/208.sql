# --- !Ups

ALTER TABLE person_days ADD COLUMN ignore_short_leave BOOLEAN DEFAULT FALSE;
ALTER TABLE person_days_history ADD COLUMN ignore_short_leave BOOLEAN;


# --- !Downs

ALTER TABLE person_days DROP COLUMN ignore_short_leave;
ALTER TABLE person_days_history DROP COLUMN ignore_short_leave;