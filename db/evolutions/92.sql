# ---!Ups
INSERT INTO roles (name) VALUES ('shiftSupervisor');

CREATE TABLE shift_categories_persons (
	categories_id BIGINT NOT NULL,
	manager_id BIGINT NOT NULL,
	FOREIGN KEY (categories_id) REFERENCES shift_categories (id),
	FOREIGN KEY (manager_id) REFERENCES persons (id)
);

CREATE TABLE shift_categories_persons_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,

    categories_id BIGINT,
    manager_id BIGINT,
    
    PRIMARY KEY (id, _revision)
);

# ---!Downs
DELETE FROM roles WHERE name = 'shiftSupervisor';

DROP TABLE persons_shift_categories;
DROP TABLE persons_shift_categories_history;