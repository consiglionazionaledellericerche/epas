# ---!Ups

ALTER TABLE absences RENAME COLUMN absencerequest TO absence_file;
ALTER TABLE absences_history RENAME COLUMN absencerequest TO absence_file;

ALTER TABLE "person_children_history" ALTER COLUMN "borndate" TYPE date using ("borndate"::text::date);

DELETE FROM persons_permissions where permissions_id = 14;

DELETE FROM permissions where id = 14 and description = 'insertAndUpdateOffices';

INSERT INTO permissions (description) values ('insertAndUpdateOffices');

# -- !Downs

ALTER TABLE absences RENAME COLUMN absence_file TO absencerequest;
ALTER TABLE absences_history RENAME COLUMN absence_file TO absencerequest;

DELETE FROM permissions where description = 'insertAndUpdateOffices';
INSERT INTO permissions (id, description) values (14, 'insertAndUpdateOffices');