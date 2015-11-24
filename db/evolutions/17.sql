# ---!Ups

create sequence seq_contracts_working_time_types
	START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

CREATE TABLE contracts_working_time_types
(
  id bigint NOT NULL DEFAULT nextval('seq_contracts_working_time_types'::regclass),
  begin_date date,
  end_date date,
  contract_id bigint,
  working_time_type_id bigint,
  CONSTRAINT contracts_working_time_types_pkey PRIMARY KEY (id),
  CONSTRAINT fk353559b335555570 FOREIGN KEY (working_time_type_id)
      REFERENCES working_time_types (id),
  CONSTRAINT fk353559b3fb1f039e FOREIGN KEY (contract_id)
      REFERENCES contracts (id)
);

insert into contracts_working_time_types 
  select nextval('seq_contracts_working_time_types'), begin_contract, end_contract, id, 1 
  from contracts where end_contract is not null;


insert into contracts_working_time_types  
  select nextval('seq_contracts_working_time_types'), begin_contract, expire_contract, id, 1 
  from contracts where end_contract is null;

# ---!Downs

drop table contracts_working_time_types;
drop sequence seq_contracts_working_time_types;