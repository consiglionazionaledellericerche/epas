# --- !Ups

DELETE FROM initialization_groups WHERE group_absence_type_id = (SELECT id FROM group_absence_types WHERE name = 'G_09');

UPDATE configurations SET field_value = 'true' WHERE epas_param = 'ENABLE_MISSIONS_INTEGRATION';

-- Non è necessaria una down