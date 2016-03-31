# ---!Ups

--- Aggiunta nella tabella dello storico degli absence_types mancanti
INSERT INTO absence_types_history (id,_revision,_revision_type,certification_code, code, considered_week_end, description , internal_use, justified_time_at_work,valid_from, valid_to)
SELECT id,(SELECT MIN(rev) FROM revinfo) AS _revision,0 AS _revision_type,certification_code, code, considered_week_end, description , internal_use, justified_time_at_work,valid_from,valid_to
FROM absence_types WHERE id NOT IN (SELECT id FROM absence_types_history);

# ---!Downs
