# --- !Ups

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO1', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H1';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO2', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H2';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO3', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H3';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO4', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H4';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO5', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H5';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO6', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H6';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92HO7', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92H7';

UPDATE absence_types SET code = '92HO1' WHERE code = '92H1';
UPDATE absence_types SET code = '92HO2' WHERE code = '92H2';
UPDATE absence_types SET code = '92HO3' WHERE code = '92H3';
UPDATE absence_types SET code = '92HO4' WHERE code = '92H4';
UPDATE absence_types SET code = '92HO5' WHERE code = '92H5';
UPDATE absence_types SET code = '92HO6' WHERE code = '92H6';
UPDATE absence_types SET code = '92HO7' WHERE code = '92H7';


# --- !Downs

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H1', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO1';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H2', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO2';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H3', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO3';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H4', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO4';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H5', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO5';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H6', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO6';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, '92H7', considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation
FROM absence_types WHERE code = '92HO7';

UPDATE absence_types SET code = '92H1' WHERE code = '92HO1';
UPDATE absence_types SET code = '92H2' WHERE code = '92HO2';
UPDATE absence_types SET code = '92H3' WHERE code = '92HO3';
UPDATE absence_types SET code = '92H4' WHERE code = '92HO4';
UPDATE absence_types SET code = '92H5' WHERE code = '92HO5';
UPDATE absence_types SET code = '92H6' WHERE code = '92HO6';
UPDATE absence_types SET code = '92H7' WHERE code = '92HO7';