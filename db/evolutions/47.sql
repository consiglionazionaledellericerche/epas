# ---!Ups

ALTER TABLE persons DROP COLUMN born_date;
ALTER TABLE persons DROP COLUMN department;
ALTER TABLE persons DROP COLUMN head_office;
ALTER TABLE persons DROP COLUMN room;

ALTER TABLE persons_history DROP COLUMN born_date;
ALTER TABLE persons_history DROP COLUMN department;
ALTER TABLE persons_history DROP COLUMN head_office;
ALTER TABLE persons_history DROP COLUMN room;

ALTER TABLE persons ADD COLUMN iId integer;
ALTER TABLE persons_history ADD COLUMN iId integer;

# ---!Downs

ALTER TABLE persons ADD COLUMN born_date timestamp;
ALTER TABLE persons ADD COLUMN department varchar (255);
ALTER TABLE persons ADD COLUMN head_office varchar (255);
ALTER TABLE persons ADD COLUMN room varchar (255);

ALTER TABLE persons_history ADD COLUMN born_date timestamp;
ALTER TABLE persons_history ADD COLUMN department varchar (255);
ALTER TABLE persons_history ADD COLUMN head_office varchar (255);
ALTER TABLE persons_history ADD COLUMN room varchar (255);

ALTER TABLE persons DROP COLUMN iId;
ALTER TABLE persons_history DROP COLUMN iId;