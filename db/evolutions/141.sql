# --- !Ups

ALTER TABLE person_days_history ALTER COLUMN out_opening DROP NOT NULL;
ALTER TABLE person_days_history ALTER COLUMN approved_out_opening DROP NOT NULL;
ALTER TABLE person_days_history ALTER COLUMN on_holiday DROP NOT NULL;
ALTER TABLE person_days_history ALTER COLUMN approved_on_holiday DROP NOT NULL;

ALTER TABLE competences_history ALTER COLUMN person_id DROP NOT NULL;
ALTER TABLE competences_history ALTER COLUMN competence_code_id DROP NOT NULL;

ALTER TABLE person_month_recap_history ALTER COLUMN person_id DROP NOT NULL;

ALTER TABLE zones_history ALTER COLUMN name DROP NOT NULL;
ALTER TABLE zones_history ALTER COLUMN badge_reader_id DROP NOT NULL;


# --- !Downs
-- non è necessaria una down