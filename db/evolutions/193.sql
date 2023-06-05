# ---!Ups

UPDATE absence_types SET code = '25O' where code = '25';
UPDATE absence_types SET code = '252O' where code = '252';
UPDATE absence_types SET code = '253O' where code = '253';
UPDATE absence_types SET code = '254O' where code = '254';

UPDATE absence_types SET code = '25MO' where code = '25M';
UPDATE absence_types SET code = '252MO' where code = '252M';
UPDATE absence_types SET code = '253MO' where code = '253M';
UPDATE absence_types SET code = '254MO' where code = '254M';

UPDATE absence_types SET code = '25OH7' where code = '25H7';
UPDATE absence_types SET code = '252OH7' where code = '252H7';
UPDATE absence_types SET code = '253OH7' where code = '253H7';
UPDATE absence_types SET code = '254OH7' where code = '254H7';

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '25';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '252';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '253';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '254';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '25M';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '252M';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '253M';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '254M';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '25H7';
    
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '252H7';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '253H7';
    
INSERT INTO absence_types_history (
    id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
    justified_time_at_work, valid_from, valid_to, justified_time, replacing_time, replacing_type_id,
    documentation, reperibility_compatible, is_real_absence, to_update, meal_ticket_behaviour
    FROM absence_types WHERE code = '254H7';
    

# ---!Downs

-- non è necessaria una down

