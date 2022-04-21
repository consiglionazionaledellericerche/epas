# --- !Ups

DELETE FROM person_configurations pc 
using person_configurations pc2 
WHERE pc.id > pc2.id 
AND pc.person_id = pc2.person_id AND pc.epas_param = pc2.epas_param;

ALTER TABLE person_configurations 
ADD CONSTRAINT unique_param UNIQUE (person_id, epas_param);

# --- !Downs
