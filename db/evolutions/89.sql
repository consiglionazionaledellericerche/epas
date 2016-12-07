# ---!Ups

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO configurations_history (id, _revision, _revision_type) SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 2  FROM configurations WHERE epas_param = 'NEW_ATTESTATI';

DELETE FROM configurations WHERE epas_param = 'NEW_ATTESTATI';

# ---!Downs

