# ---!Ups

CREATE SEQUENCE seq_institutes
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE institutes (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_institutes'::regclass),
    
    created_at timestamp without time zone,
    updated_at timestamp without time zone,

	oldid BIGINT NOT NULL,
    
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    
    PRIMARY KEY (id),
    CONSTRAINT institute_code_unique UNIQUE(code),
    CONSTRAINT institute_name_unique UNIQUE(name)
);

CREATE TABLE institutes_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
    code TEXT,
    name TEXT,
        
    PRIMARY KEY (id, _revision)
);

ALTER TABLE office ADD COLUMN institute_id BIGINT;
ALTER TABLE office_history ADD COLUMN institute_id BIGINT;

ALTER TABLE office ADD COLUMN headquarter BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE office_history ADD COLUMN headquarter BOOLEAN ;

delete from conf_year where id in (
select cy.id 
from conf_year cy 
left outer join office i on cy.office_id = i.id 
where i.contraction is not null);

delete from users_roles_offices where id in (
select uro.id from users_roles_offices uro 
left outer join office i on uro.office_id = i.id 
where i.contraction is not null);

insert into institutes (code, name, oldid) 
select i.contraction as code, i.name as name, i.id as oldid 
from office i 
where i.contraction is not null;

update office set institute_id = I.newid from (
select o.id as oid, i.id as newid 
from office o left outer join institutes i on o.office_id = i.oldid 
where o.code is not null) as I 
where id = I.oid;

-- rimozione vecchia relazione fra office e institutes
ALTER TABLE office DROP CONSTRAINT fkc3373ebc2d0fa45e;

ALTER TABLE office ADD CONSTRAINT "office_institute_id_fkey" FOREIGN KEY (institute_id) REFERENCES institutes(id);

delete from office where contraction is not null;

--rimozione dell'entità area
delete from users_roles_offices where id in (
select uro.id from users_roles_offices uro 
left outer join office a on uro.office_id = a.id 
where a.institute_id is null);

delete from conf_general where id in (
select cg.id 
from conf_general cg 
left outer join office a on cg.office_id = a.id 
where a.institute_id is null);

delete from office where id in (
select id from office a where a.institute_id is null);

alter table office drop column office_id;
alter table office_history drop column office_id;
alter table office drop column contraction;
alter table office_history drop contraction; 

update office set headquarter = false;
alter table institutes drop column oldid;

update institutes set created_at = current_timestamp;

-- fix tabella storico conf general
alter table conf_general_history alter column office_id drop not null;

-- constraint sugli user role office
ALTER TABLE users_roles_offices ALTER COLUMN office_id SET NOT NULL;
ALTER TABLE users_roles_offices ALTER COLUMN role_id SET NOT NULL;
ALTER TABLE users_roles_offices ALTER COLUMN user_id SET NOT NULL;

-- ------------------------------------------------------------------------------
-- la relazione fra badge_readers e l'office proprietario
-- ------------------------------------------------------------------------------

ALTER TABLE badge_readers ADD COLUMN office_owner_id BIGINT;
ALTER TABLE badge_readers_history ADD column office_owner_id BIGINT;
ALTER TABLE badge_readers ADD CONSTRAINT "badge_reader_owner_id_fkey" 
  FOREIGN KEY (office_owner_id) REFERENCES office(id);

-- riempire il campo con un valore di default (esempio tutti alla prima sede)
update badge_readers set office_owner_id = (select id from office order by id limit 1);

-- rimuovere i badge_reader che continuano ad avere il campo office_owner_id null
delete from badge_readers where office_owner_id is null;
-- TODO: potrebbe saltare se rimuovo un badge_reader con badge associati (ma è un caso che 
-- non si verifica mai)

-- aggiungere il vincolo not null
ALTER TABLE badge_readers ALTER COLUMN office_owner_id SET NOT NULL;

INSERT INTO roles(name) values('tecnicalAdmin');

-- TODO impostare not null i nuovi campi institute_id e headquarter della tabella office
