# --- !Ups

ALTER TABLE contracts ADD COLUMN contract_type TEXT;
ALTER TABLE contracts_history ADD COLUMN contract_type TEXT;

UPDATE contracts SET contract_type = 'structured_public_administration' WHERE on_certificate = true;
UPDATE contracts SET contract_type = 'unstructured' WHERE on_certificate = false;

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO contracts_history (id, _revision, _revision_type, begin_date, end_date, end_contract, person_id, source_date_residual, source_permission_used,
source_recovery_day_used, source_remaining_minutes_current_year,  source_remaining_minutes_last_year, source_vacation_current_year_used, source_vacation_last_year_used,
source_remaining_meal_ticket, source_by_admin, source_date_meal_ticket, perseo_id, is_temporary, source_date_vacation, source_date_recovery_day, previous_contract_id,
external_id, contract_type)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, begin_date, end_date, end_contract, person_id, source_date_residual, source_permission_used,
source_recovery_day_used, source_remaining_minutes_current_year,  source_remaining_minutes_last_year, source_vacation_current_year_used, source_vacation_last_year_used,
source_remaining_meal_ticket, source_by_admin, source_date_meal_ticket, perseo_id, is_temporary, source_date_vacation, source_date_recovery_day, previous_contract_id,
external_id, contract_type FROM contracts;

ALTER TABLE contracts DROP COLUMN on_certificate;
ALTER TABLE contracts_history DROP COLUMN on_certificate;


# --- !Downs
# -- Non è necessaria una down