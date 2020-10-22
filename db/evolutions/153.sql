# --- !Ups

INSERT INTO person_configurations (person_id, epas_param, field_value, begin_date, end_date, version) 
SELECT person_id, 'TELEWORK_STAMPINGS', 'true', now(), null, 0 
FROM person_configurations where epas_param = 'TELEWORK' and field_value = 'true';

# --- !Downs
