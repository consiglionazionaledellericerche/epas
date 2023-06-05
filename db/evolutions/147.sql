# --- !Ups

CREATE INDEX stampings_history_id ON stampings_history(id);
CREATE INDEX stampings_history_revision ON stampings_history(_revision);
CREATE INDEX stampings_history_revision_type ON stampings_history(_revision_type);
CREATE INDEX stampings_history_person_day_id ON stampings_history(person_day_id);

CREATE INDEX absences_history_id ON absences_history(id);
CREATE INDEX absences_history_revision ON absences_history(_revision);
CREATE INDEX absences_history_revision_type ON absences_history(_revision_type);
CREATE INDEX absences_history_person_day_id ON absences_history(person_day_id);
CREATE INDEX absences_history_absence_type_id ON absences_history(absence_type_id);

CREATE INDEX person_reperibility_days_history_id ON person_reperibility_days_history(id);
CREATE INDEX person_reperibility_days_history_revision ON person_reperibility_days_history(_revision);
CREATE INDEX person_reperibility_days_history_revision_type ON person_reperibility_days_history(_revision_type);

CREATE INDEX person_shift_days_history_id ON person_shift_days_history(id);
CREATE INDEX person_shift_days_history_revision ON person_shift_days_history(_revision);
CREATE INDEX person_shift_days_history_revision_type ON person_shift_days_history(_revision_type);

# --- !Downs

DROP INDEX stampings_history_id;
DROP INDEX stampings_history_revision;
DROP INDEX stampings_history_revision_type;
DROP INDEX stampings_history_person_day_id;

DROP INDEX absences_history_id;
DROP INDEX absences_history_revision;
DROP INDEX absences_history_revision_type;
DROP INDEX absences_history_person_day_id;
DROP INDEX absences_history_absence_type_id;

DROP INDEX person_reperibility_days_history_id;
DROP INDEX person_reperibility_days_history_revision;
DROP INDEX person_reperibility_days_history_revision_type;

DROP INDEX person_shift_days_history_id;
DROP INDEX person_shift_days_history_revision;
DROP INDEX person_shift_days_history_revision_type;
