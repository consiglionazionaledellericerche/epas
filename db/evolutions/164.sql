# --- !Ups

SELECT SETVAL('seq_absence_types', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM absence_types;
SELECT SETVAL('seq_qualifications', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL ) FROM qualifications;

# --- !Downs
