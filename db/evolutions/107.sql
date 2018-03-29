# ---!Ups

INSERT INTO stampings_history (id, _revision, _revision_type, date, marked_by_admin, note, way, personday_id,
stamp_modification_type_id, marked_by_employee, stamp_type, stamping_zone)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, date, marked_by_admin, note, way, personday_id,
stamp_modification_type_id, marked_by_employee, 'LAVORO_FUORI_SEDE', stamping_zone
FROM stampings WHERE stamp_type = 'MOTIVI_DI_SERVIZIO_FUORI_SEDE';

UPDATE stampings
SET stamp_type = 'LAVORO_FUORI_SEDE'
WHERE stamp_type = 'MOTIVI_DI_SERVIZIO_FUORI_SEDE';

# ---!Downs