# ---!Ups

--- 1) takable_absence_behaviours
CREATE TABLE takable_absence_behaviours (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  amount_type TEXT NOT NULL,
  --- takable_count_behaviour TEXT NOT NULL, (per ora inutile 
  --- takaen_count_behaviour TEXT NOT NULL,   sempre period)
  fixed_limit INT NOT NULL,
  takable_amount_adjust TEXT
);

CREATE TABLE takable_absence_behaviours_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  amount_type TEXT,
  --- takable_count_behaviour TEXT,
  --- takaen_count_behaviour TEXT,
  fixed_limit INT,
  takable_amount_adjust TEXT
);

--- 2) taken_codes_group
CREATE TABLE taken_codes_group (
  id BIGSERIAL PRIMARY KEY,
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id)
);

CREATE TABLE taken_codes_group_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT,
  takable_behaviour_id BIGINT
);

--- 3) takable_codes_group
CREATE TABLE takable_codes_group (
  id BIGSERIAL PRIMARY KEY,
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id)
);

CREATE TABLE takable_codes_group_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT,
  takable_behaviour_id BIGINT
);

--- 4) complation_absence_behaviours
CREATE TABLE complation_absence_behaviours (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  amount_type TEXT NOT NULL
);

CREATE TABLE complation_absence_behaviours_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  amount_type TEXT
);

--- 5) complation_codes_group
CREATE TABLE complation_codes_group (
  id BIGSERIAL PRIMARY KEY,
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id)
);

CREATE TABLE complation_codes_group_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT,
  complation_behaviour_id BIGINT
);


--- 6) replacing_codes_group
CREATE TABLE replacing_codes_group (
  id BIGSERIAL PRIMARY KEY,
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id)
);

CREATE TABLE replacing_codes_group_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT,
  complation_behaviour_id BIGINT
);

--- 7) group_absence_types

CREATE TABLE group_absence_types (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  pattern TEXT NOT NULL,
  period_type TEXT,
  takable_behaviour_id BIGINT,
  complation_behaviour_id BIGINT,
  next_group_to_check_id BIGINT,
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id),
  FOREIGN KEY (next_group_to_check_id) REFERENCES group_absence_types (id)
);

CREATE TABLE group_absence_types_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  pattern TEXT,
  period_type TEXT,
  takable_behaviour_id BIGINT,
  complation_behaviour_id BIGINT,
  next_group_to_check_id BIGINT
);

ALTER TABLE absence_types ADD COLUMN time_for_mealticket BOOLEAN default false;
ALTER TABLE absence_types ADD COLUMN justified_time INT;
ALTER TABLE absence_types ADD COLUMN justified_type_permitted TEXT;

ALTER TABLE absence_types_history ADD COLUMN time_for_mealticket BOOLEAN;
ALTER TABLE absence_types_history ADD COLUMN justified_time INT;
ALTER TABLE absence_types_history ADD COLUMN justified_type_permitted TEXT;

# ---!Downs

ALTER TABLE absence_types DROP COLUMN time_for_mealticket;
ALTER TABLE absence_types DROP COLUMN justified_time;
ALTER TABLE absence_types DROP COLUMN justifiedTypePermitted;

ALTER TABLE absence_types_history DROP COLUMN time_for_mealticket;
ALTER TABLE absence_types_history DROP COLUMN justified_time;
ALTER TABLE absence_types_history DROP COLUMN justified_type_permitted;

DROP TABLE taken_codes_group;
DROP TABLE taken_codes_group_history;
DROP TABLE takable_codes_group;
DROP TABLE takable_codes_group_history;

DROP TABLE complation_codes_group;
DROP TABLE complation_codes_group_history;
DROP TABLE replacing_codes_group;
DROP TABLE replacing_codes_group_history;

DROP TABLE group_absence_types;
DROP TABLE group_absence_types_history;

DROP TABLE takable_absence_behaviours;
DROP TABLE takable_absence_behaviours_history;

DROP TABLE complation_absence_behaviours;
DROP TABLE complation_absence_behaviours_history;










