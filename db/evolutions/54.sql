# ---!Ups

ALTER TABLE office ALTER COLUMN code SET DATA TYPE text USING code::text;
ALTER TABLE office RENAME COLUMN code TO codeId;
ALTER TABLE office ADD COLUMN code text;
ALTER TABLE office ADD COLUMN cds text;

ALTER TABLE office_history ALTER COLUMN code SET DATA TYPE text USING code::text;
ALTER TABLE office_history RENAME COLUMN code TO codeId;
ALTER TABLE office_history ADD COLUMN code text;
ALTER TABLE office_history ADD COLUMN cds text;

CREATE TABLE badges (
    id BIGSERIAL NOT NULL,
    end_date date,
    number text NOT NULL,
    start_date date,
    person_id bigint NOT NULL
);

CREATE TABLE badges_history (
    id bigint NOT NULL,
  	_revision integer NOT NULL,
  	_revision_type smallint,
    end_date date,
    number text,
    start_date date,
    person_id bigint
);

INSERT INTO badges (number, person_id) 
(SELECT trim(leading '0' from badgenumber),id FROM persons WHERE trim(leading '0' from badgenumber) != '');

INSERT INTO badges_history (id,_revision, _revision_type, number, person_id)
SELECT B.id,r.rev, 0,B.number,B.id 
FROM (SELECT id,number,person_id FROM badges) AS B,(SELECT MAX(rev) AS rev FROM revinfo) AS r;

ALTER TABLE ONLY badges
    ADD CONSTRAINT badges_pkey PRIMARY KEY (id);

ALTER TABLE ONLY badges
    ADD CONSTRAINT uk_6p49ha617hbv6v57cb03puljl UNIQUE (person_id, number);

ALTER TABLE ONLY badges
    ADD CONSTRAINT fk_1k17y9q7dd0tpcxl9nddeak9 FOREIGN KEY (person_id) REFERENCES persons(id);

ALTER TABLE ONLY badges_history
    ADD CONSTRAINT badges_history_pkey PRIMARY KEY (id, _revision);

ALTER TABLE ONLY badges_history
    ADD CONSTRAINT fk_7ve98bxsk1q6dqa09w6k1lt0o FOREIGN KEY (_revision) REFERENCES revinfo(rev);

ALTER TABLE persons DROP COLUMN badgenumber;
ALTER TABLE persons_history DROP COLUMN badgenumber;

UPDATE persons SET number = DEFAULT WHERE number = 0;

# ---!Downs

ALTER TABLE office DROP COLUMN code;
ALTER TABLE office DROP COLUMN cds;
ALTER TABLE office ALTER COLUMN codeId SET DATA TYPE integer USING codeId::integer;
ALTER TABLE office RENAME COLUMN codeId TO code;

ALTER TABLE office_history DROP COLUMN code;
ALTER TABLE office_history DROP COLUMN cds;
ALTER TABLE office_history ALTER COLUMN codeId SET DATA TYPE integer USING codeId::integer;
ALTER TABLE office_history RENAME COLUMN codeId TO code;

ALTER TABLE persons ADD COLUMN badgenumber text;
ALTER TABLE persons_history ADD COLUMN badgenumber text;

UPDATE persons
SET badgenumber=(SELECT number FROM badges WHERE person_id = persons.id );

UPDATE persons_history
SET badgenumber=(SELECT number FROM badges WHERE person_id = persons_history.id );

DROP TABLE badges;
DROP TABLE badges_history;