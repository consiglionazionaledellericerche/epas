# ---!Ups

DELETE FROM configurations WHERE epas_param = 'NEW_ATTESTATI';
DELETE FROM configurations_history WHERE epas_param = 'NEW_ATTESTATI';

# ---!Downs

