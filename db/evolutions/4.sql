# --- !Ups

CREATE SEQUENCE seq_persons_working_time_types

    START WITH 1

    INCREMENT BY 1

    NO MINVALUE

    NO MAXVALUE;

CREATE TABLE persons_working_time_types (
    id bigint DEFAULT nextval('seq_persons_working_time_types'::regclass) NOT NULL,
    begin_date date,
    end_date date,
    person_id bigint,
    working_time_type_id bigint
);

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT persons_working_time_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT fkb943247635555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT fkb9432476e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



insert into persons_working_time_types select nextval('seq_persons_working_time_types'), '2013-01-01', null, id, working_time_type_id from persons;
