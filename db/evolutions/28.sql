# --- !Ups

ALTER TABLE persons ADD COLUMN cnr_email varchar(255);
ALTER TABLE persons ADD COLUMN fax varchar (255);
ALTER TABLE persons ADD COLUMN mobile varchar (255);
ALTER TABLE persons ADD COLUMN telephone varchar (255);
ALTER TABLE persons ADD COLUMN department varchar (255);
ALTER TABLE persons ADD COLUMN head_office varchar (255);
ALTER TABLE persons ADD COLUMN room varchar (255);
ALTER TABLE persons ADD COLUMN want_email boolean;

ALTER TABLE persons_history ADD COLUMN cnr_email varchar(255);
ALTER TABLE persons_history ADD COLUMN fax varchar (255);
ALTER TABLE persons_history ADD COLUMN mobile varchar (255);
ALTER TABLE persons_history ADD COLUMN telephone varchar (255);
ALTER TABLE persons_history ADD COLUMN department varchar (255);
ALTER TABLE persons_history ADD COLUMN head_office varchar (255);
ALTER TABLE persons_history ADD COLUMN room varchar (255);
ALTER TABLE persons_history ADD COLUMN want_email boolean;

UPDATE persons set email = contact_data.email from contact_data where persons.id = contact_data.person_id;
UPDATE persons set fax = contact_data.fax from contact_data where persons.id = contact_data.person_id;
UPDATE persons set mobile = contact_data.mobile from contact_data where persons.id = contact_data.person_id;
UPDATE persons set telephone = contact_data.telephone from contact_data where persons.id = contact_data.person_id;
UPDATE persons set department = locations.department from locations where persons.id = locations.person_id;
UPDATE persons set head_office = locations.headoffice from locations where persons.id = locations.person_id;
UPDATE persons set room = locations.room from locations where persons.id = locations.person_id;


DROP TABLE contact_data_history;
DROP TABLE contact_data;
DROP TABLE locations;

DROP sequence seq_contact_data;
DROP sequence seq_locations;


# ---!Downs

CREATE SEQUENCE seq_contact_data
START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;
  
CREATE SEQUENCE seq_locations
START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;
  
CREATE TABLE contact_data(
id bigint not null DEFAULT nextval('seq_contact_data'::regclass),
email varchar(255),
fax varchar(255),
mobile varchar(255),
telephone varchar(255),
person_id bigint,
CONSTRAINT contact_data_pkey PRIMARY KEY (id),
CONSTRAINT contact_data_fkey FOREIGN KEY (person_id)
      REFERENCES persons (id));
      
CREATE TABLE locations(
id bigint not null DEFAULT nextval('seq_locations'::regclass),
department varchar(255),
head_office varchar(255),
room varchar(255),
person_id bigint,
CONSTRAINT locations_pkey PRIMARY KEY (id),
CONSTRAINT locations_fkey FOREIGN KEY (person_id)
      REFERENCES persons (id));

CREATE TABLE contact_data_history(
id bigint not null,
_revision integer NOT NULL,
_revision_type smallint,
email varchar(255),
fax varchar(255),
mobile varchar(255),
telephone varchar(255),
person_id bigint,
CONSTRAINT contact_data_history_pkey PRIMARY KEY (id, _revision),
CONSTRAINT revinfo_contact_data_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev));

      
insert into contact_data(person_id) select id from persons;
update contact_data set email = persons.email, fax = persons.fax, mobile = persons.mobile, telephone = persons.telephone 
from persons where persons.id = contact_data.person_id;

insert into locations(person_id) select id from persons;
update locations set department = persons.department, head_office = persons.head_office, room = persons.room 
from persons where persons.id = locations.person_id;