# --- !Ups

ALTER TABLE shift_time_table ADD COLUMN calculation_type TEXT;
UPDATE shift_time_table SET calculation_type = 'standard_CNR';

CREATE TABLE organization_shift_time_table(
	id BIGSERIAL PRIMARY KEY,
	name TEXT,
	office_id BIGINT NOT NULL,
	calculation_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	FOREIGN KEY (office_id) REFERENCES office (id),
	version INT DEFAULT 0);

CREATE TABLE organization_shift_time_table_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    name TEXT,
	office_id BIGINT,
	calculation_type TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);

ALTER TABLE shift_type ADD COLUMN organization_shift_time_table_id BIGINT;
ALTER TABLE shift_type_history ADD COLUMN organization_shift_time_table_id BIGINT;
ALTER TABLE shift_type ADD FOREIGN KEY (organization_shift_time_table_id) REFERENCES organization_shift_time_table(id);

CREATE TABLE organization_shift_slot(
	id BIGSERIAL PRIMARY KEY,
	shift_time_table_id BIGINT NOT NULL,
	begin_slot VARCHAR(64) NOT NULL,
	end_slot VARCHAR(64) NOT NULL,
	begin_meal_slot VARCHAR(64),
	end_meal_slot VARCHAR(64),
	minutes_slot INTEGER,
	minutes_paid INTEGER,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	FOREIGN KEY (shift_time_table_id) REFERENCES organization_shift_time_table(id),
	version INT DEFAULT 0
	);

CREATE TABLE organization_shift_slot_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    shift_time_table_id BIGINT,
    begin_slot VARCHAR(64),
	end_slot VARCHAR(64),
	begin_meal_slot VARCHAR(64),
	end_meal_slot VARCHAR(64),
	minutes_slot INTEGER,
	minutes_paid INTEGER,
	PRIMARY KEY (id, _revision, _revision_type)
    );

# --- !Downs
-- non è necessaria una down