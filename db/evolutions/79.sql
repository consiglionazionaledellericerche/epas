# ---!Ups

--- Inserimento dei record mancanti nelle tabelle dello storico

INSERT INTO absence_type_groups_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, accumulationbehaviour, accumulation_type, label, limit_in_minute , minutes_excess, replacing_absence_type_id
FROM absence_type_groups WHERE id NOT IN (SELECT id FROM absence_type_groups_history);

INSERT INTO absence_types_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0 ,certification_code, code, considered_week_end, description , internal_use, justified_time_at_work,valid_from,valid_to
FROM absence_types WHERE id NOT IN (SELECT id FROM absence_types_history);

INSERT INTO badges_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0 ,code, person_id, badge_reader_id, badge_system_id
FROM badges WHERE id NOT IN (SELECT id FROM badges_history);

INSERT INTO certificated_data_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0 ,absences_sent, cognome_nome, competences_sent, is_ok, matricola, month, problems, year, person_id, traininghours_sent, meal_ticket_sent
FROM certificated_data WHERE id NOT IN (SELECT id FROM certificated_data_history);

INSERT INTO institutes_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0 ,code, name, cds
FROM institutes WHERE id NOT IN (SELECT id FROM institutes_history);

INSERT INTO person_months_recap_history
SELECT id,(SELECT MAX(rev) FROM revinfo),0, year, month, training_hours, hours_approved, person_id
FROM person_months_recap WHERE id NOT IN (SELECT id FROM person_months_recap_history);

INSERT INTO qualifications_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, description, qualification
FROM qualifications WHERE id NOT IN (SELECT id FROM qualifications_history);

INSERT INTO roles_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, name
FROM roles WHERE id NOT IN (SELECT id FROM roles_history);

INSERT INTO stamp_modification_types_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, code, description
FROM stamp_modification_types WHERE id NOT IN (SELECT id FROM stamp_modification_types_history);

ALTER TABLE vacation_periods_history ALTER COLUMN vacation_codes_id TYPE text;
ALTER TABLE vacation_periods_history RENAME COLUMN vacation_codes_id TO vacation_code;

INSERT INTO vacation_periods_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, begin_date, end_date, contract_id, vacation_code
FROM vacation_periods WHERE id NOT IN (SELECT id FROM vacation_periods_history);

INSERT INTO working_time_type_days_history
SELECT id,(SELECT MIN(rev) FROM revinfo),0, breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time
FROM working_time_type_days WHERE id NOT IN (SELECT id FROM working_time_type_days_history);

--- Eliminazione tabelle inutili

DROP TABLE mealaux;
DROP TABLE permissions_groups_history;
DROP TABLE permissions_groups;
DROP TABLE person_years_history;
DROP TABLE person_years;
DROP TABLE persons_groups_history;
DROP TABLE persons_groups;
DROP TABLE roles_permissions_history;
DROP TABLE roles_permissions;
DROP TABLE permissions_history;
DROP TABLE permissions;

# ---!Downs
