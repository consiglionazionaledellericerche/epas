# ---!Ups

CREATE TABLE meal_ticket_card(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	begin_date DATE,
	end_date DATE,
	number TEXT,
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
	number TEXT,
	is_active BOOLEAN,
	delivery_date DATE,
	delivery_office_id BIGINT);
	
ALTER TABLE meal_ticket ADD COLUMN meal_ticket_card_id BIGINT;
ALTER TABLE meal_ticket_history ADD COLUMN meal_ticket_card_id BIGINT;
ALTER TABLE meal_ticket ADD FOREIGN KEY (meal_ticket_card_id) REFERENCES meal_ticket_card(id);

CREATE INDEX meal_ticket_card_person_id_idx ON meal_ticket_card(person_id);
CREATE INDEX meal_ticket_card_delivery_office_idx ON meal_ticket_card(delivery_office_id);
CREATE INDEX meal_ticket_card_delivery_date_idx ON meal_ticket_card(delivery_date);

CREATE INDEX meal_ticket_meal_ticket_card_id_idx ON meal_ticket(meal_ticket_card_id);

# ---!Downs

DROP INDEX meal_ticket_meal_ticket_card_id_idx;
DROP INDEX meal_ticket_card_delivery_date_idx;
DROP INDEX meal_ticket_card_delivery_office_idx;
DROP INDEX meal_ticket_card_person_id_idx;

DROP TABLE meal_ticket_card_history;
DROP TABLE meal_ticket_card;