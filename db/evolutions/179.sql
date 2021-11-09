# --- !Ups

UPDATE person_configurations SET field_value = false WHERE epas_param = 'COVID_19';

INSERT INTO person_configurations_history (id, _revision, _revision_type, person_id, epas_param, 
field_value, begin_date, end_date)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, person_id, epas_param, 
field_value, begin_date, end_date 
FROM person_configurations WHERE epas_param = 'COVID_19';

# --- !Downs