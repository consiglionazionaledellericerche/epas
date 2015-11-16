# ---!Ups


create sequence seq_office
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

CREATE TABLE office (
    id bigint DEFAULT nextval('seq_office'::regclass) NOT NULL,
    discriminator character varying(31) NOT NULL,
    name character varying(255),
    address character varying(255),
    code integer,
    joining_date date,
    office_id bigint
);

CREATE TABLE office_history (
    id bigint NOT NULL,
    discriminator character varying(31) NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    name character varying(255),
    address character varying(255),
    code integer,
    joining_date date,
    office_id bigint
);

ALTER TABLE ONLY office_history
    ADD CONSTRAINT office_history_pkey PRIMARY KEY (id, _revision);

ALTER TABLE ONLY office_history
    ADD CONSTRAINT fkd52a4e11d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);

ALTER TABLE ONLY office
    ADD CONSTRAINT office_pkey PRIMARY KEY (id);

ALTER TABLE ONLY persons
    ADD COLUMN office_id bigint;

ALTER TABLE ONLY office
    ADD CONSTRAINT fkc3373ebc2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);

ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);

INSERT INTO office (id, discriminator, name, address, code, joining_date, office_id)
VALUES (1, 'O', 'IIT', 'Via Moruzzi 1, Pisa', 1000, NULL, NULL);

update persons set office_id =  subquery.id 
from (select id from office where id =1) as subquery;

insert into permissions (id, description) values (14, 'insertAndUpdateOffices');


