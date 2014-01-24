# ---!Ups

ALTER TABLE absences RENAME COLUMN absencerequest TO absence_file;


# -- !Downs

ALTER TABLE  absences RENAME COLUMN absence_file TO absencerequest;
