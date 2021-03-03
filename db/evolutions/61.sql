# -- Evoluzione per la configurazione periodica dell'office.
# -- Crea le tabelle conf_period e conf_period_history.
 
# ---!Ups

CREATE SEQUENCE seq_conf_period
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE conf_period (
    
    id BIGINT NOT NULL DEFAULT nextval('seq_conf_period'::regclass),
    
    field TEXT,
	field_value TEXT,
	date_from DATE, 
	date_to DATE,
	office_id BIGINT,
		
	FOREIGN KEY (office_id) REFERENCES office (id)
);

CREATE TABLE conf_period_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,
	
	field TEXT,
	field_value TEXT,
	date_from DATE, 
	date_to DATE,
    office_id BIGINT NOT NULL,
    
    PRIMARY KEY (id, _revision)
);

# ---!Downs

DROP TABLE conf_period;
DROP TABLE conf_period_history;

	