# --- !Ups
DELETE FROM configurations WHERE epas_param in ('TR_COMPENSATORY', 'TR_VACATIONS');

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);
INSERT INTO configurations_history (id, _revision, _revision_type)
SELECT id, (SELECT MAX (rev) AS rev FROM revinfo), 2 FROM configurations WHERE epas_param ='TR_COMPENSATORY';

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);
INSERT INTO configurations_history (id, _revision, _revision_type)
SELECT id, (SELECT MAX (rev) AS rev FROM revinfo), 2 FROM configurations WHERE epas_param ='TR_VACATIONS';

# --- !Downs

-- non è necessaria una down