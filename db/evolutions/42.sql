# --- !Ups

SELECT setval('seq_conf_year',COALESCE ((SELECT MAX(id) FROM conf_year),1));
SELECT setval('seq_conf_general',COALESCE ((SELECT MAX(id) FROM conf_general),1));

# ---!Downs