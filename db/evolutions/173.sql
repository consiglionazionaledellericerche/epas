# --- !Ups

UPDATE person_configurations 
  SET begin_date = subquery.begin_date_telework 
  FROM 
    (SELECT pc.person_id, pc2.begin_date AS begin_date_telework FROM person_configurations pc 
     JOIN person_configurations pc2 ON pc.person_id = pc2.person_id 
     WHERE pc.field_value = 'true' AND pc.epas_param = 'TELEWORK_STAMPINGS' AND pc2.epas_param = 'TELEWORK' AND pc.begin_date <> pc2.begin_date) AS subquery 
  WHERE epas_param = 'TELEWORK_STAMPINGS' AND field_value = 'true' AND person_configurations.person_id = subquery.person_id;

# --- !Downs

-- non è necessaria una down

