# --- !Ups

UPDATE groups SET external_id = NULL WHERE external_id = '';

# --- !Downs

# Non e' necessaria una down
