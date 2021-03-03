# ---!Ups


drop table if exists person_months_history;
drop table person_months;
drop sequence seq_person_months;


create sequence seq_person_months_recap
	START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

create table person_months_recap (
	id bigint NOT NULL DEFAULT nextval('seq_person_months_recap'::regclass),
	year integer,
	month integer,
	training_hours integer,
	hours_approved boolean,
	person_id bigint NOT NULL,
	CONSTRAINT person_months_recap_pkey PRIMARY KEY (id),
	CONSTRAINT person_person_months_recap_fkey FOREIGN KEY (person_id)
      REFERENCES persons (id)
);

create table person_months_recap_history (
	id bigint NOT NULL,
  	_revision integer NOT NULL,
  	_revision_type smallint,
  	year integer,
	month integer,
	training_hours integer,
	hours_approved boolean,
	person_id bigint NOT NULL,
	CONSTRAINT person_months_recap_history_pkey PRIMARY KEY (id,  _revision),
	CONSTRAINT revinfo_person_months_recap_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

# ---!Downs



drop table person_months_recap_history;
drop table person_months_recap;
drop sequence seq_person_months_recap;

create sequence seq_person_months
	START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

create table person_months (
	id bigint NOT NULL DEFAULT nextval('seq_person_months'::regclass),
	compensatory_rest_in_minutes integer,
	month integer,
	progressiveatendofmonthinminutes integer,
	recuperi_ore_da_anno_precedente integer,
	remaining_minute_past_year_taken integer,
	residual_past_year integer,
	riposi_compensativi_da_anno_corrente integer,
	riposi_compensativi_da_anno_precedente integer,
	riposi_compensativi_da_inizializzazione integer,
	straordinari integer,
	total_remaining_minutes integer,
	year integer,
	person_id bigint NOT NULL,
	CONSTRAINT person_months_pkey PRIMARY KEY (id),
	CONSTRAINT fkbb6c161de7a7b1be FOREIGN KEY (person_id)
      REFERENCES persons (id)	
);

create table person_months_history
(
	id bigint not null
	
);