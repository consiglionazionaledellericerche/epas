# ---!Ups

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO configurations_history (id, _revision, _revision_type, office_id, epas_param, field_value, begin_date, end_date)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 2, office_id, epas_param, field_value, begin_date, end_date
FROM configurations WHERE epas_param = 'RECOMPUTATION_LIMIT';

DELETE FROM configurations WHERE epas_param = 'RECOMPUTATION_LIMIT';
# ---!Downs
--non è necessaria una down