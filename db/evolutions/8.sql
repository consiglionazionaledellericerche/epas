# ---!Ups

ALTER TABLE absences RENAME COLUMN absencerequest TO absence_file;
ALTER TABLE absences_history RENAME COLUMN absencerequest TO absence_file;

ALTER TABLE "person_children_history" ALTER COLUMN "borndate" TYPE date using ("borndate"::text::date);

# -- !Downs

ALTER TABLE absences RENAME COLUMN absence_file TO absencerequest;
ALTER TABLE absences_history RENAME COLUMN absence_file TO absencerequest;
