# ---!Ups

CREATE TABLE meal_ticket_card(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	begin_date DATE,
	end_date DATE,
	number INTEGER,
	is_active BOOLEAN,
	delivery_date DATE,
	delivery_office_id BIGINT REFERENCES office(id),
	version INT DEFAULT 0);
	
CREATE TABLE meal_ticket_card_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	person_id BIGINT,
	begin_date DATE,
	end_date DATE,
	number INTEGER,
	is_active BOOLEAN,
	delivery_date DATE,
	delivery_office_id BIGINT);
	
ALTER TABLE meal_ticket ADD COLUMN meal_ticket_card_id BIGINT;
ALTER TABLE meal_ticket_history ADD COLUMN meal_ticket_card_id BIGINT;
ALTER TABLE meal_ticket ADD FOREIGN KEY (meal_ticket_card_id) REFERENCES meal_ticket_card(id);

# ---!Downs

DROP TABLE meal_ticket_card_history;
DROP TABLE meal_ticket_card;
