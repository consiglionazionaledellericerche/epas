# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO initialization_groups_history (id, _revision, _revision_type) 
	  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 2 FROM initialization_groups 
	    WHERE group_absence_type_id = (SELECT id FROM group_absence_types WHERE name = 'G_09');

DELETE FROM initialization_groups 
  WHERE group_absence_type_id = (SELECT id FROM group_absence_types WHERE name = 'G_09');

INSERT INTO configurations_history (id, _revision, _revision_type, field_value) 
     SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, 'true' FROM configurations
       WHERE epas_param = 'ENABLE_MISSIONS_INTEGRATION' AND field_value = 'false';

UPDATE configurations SET field_value = 'true' WHERE epas_param = 'ENABLE_MISSIONS_INTEGRATION' AND field_value = 'false';

-- Non è necessaria una down