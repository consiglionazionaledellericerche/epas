# --- !Ups

UPDATE absence_types SET external_id = null WHERE external_id not in ('T');

# --- !Downs
# -- Non è necessaria una down