# ---!Ups


# -- Conversione dei period
ALTER TABLE vacation_periods RENAME COLUMN begin_from TO begin_date;
ALTER TABLE vacation_periods RENAME COLUMN end_to TO end_date;

ALTER TABLE vacation_periods_history RENAME COLUMN begin_from TO begin_date;
ALTER TABLE vacation_periods_history RENAME COLUMN end_to TO end_date;

ALTER TABLE vacation_periods ALTER COLUMN begin_date SET NOT NULL;

# -- Conversione a enumerato 

ALTER TABLE vacation_periods ADD column vacation_code TEXT;

UPDATE vacation_periods vpu SET vacation_code = t.description 
FROM (SELECT vp.id AS id, vc.description AS description FROM vacation_periods vp LEFT OUTER JOIN vacation_codes vc ON vp.vacation_codes_id = vc.id ) AS t 
WHERE vpu.id = t.id;

ALTER TABLE vacation_periods ALTER column vacation_code SET NOT NULL;

# -- Distruzione vecchia implementazione

ALTER TABLE vacation_periods DROP column vacation_codes_id;
ALTER TABLE DROP TABLE vacation_codes;
ALTER TABLE DROP TABLE vacation_codes_history;


# ---!Downs

# -- Down Distruzione vecchia implementazione
# -- L'evoluzione è distruttiva. Non è possibile risalire dal enum al vacation_codes.

# --CREATE TABLE vacation_codes (
# --    id BIGSERIAL PRIMARY KEY,
# --    description text,
# --    permission_days integer,
# --    vacation_days integer
# --);

# --CREATE TABLE vacation_codes_history (
# --    id bigint NOT NULL,
# --    _revision integer NOT NULL,
# --    _revision_type smallint,
# --    description text,
# --    permission_days integer,
# --    vacation_days integer
# --);

# -- ALTER TABLE vacation_periods ADD COLUMN vacation_codes_id BIGINT;

# -- Down Conversione a enumerato

# -- ALTER TABLE vacation_periods DROP COLUMN vacation_code;

# -- Down Conversione a Period

ALTER TABLE vacation_periods RENAME COLUMN begin_date TO begin_from;
ALTER TABLE vacation_periods RENAME COLUMN end_date TO end_to;

ALTER TABLE vacation_periods_history RENAME COLUMN begin_date TO begin_from;
ALTER TABLE vacation_periods_history RENAME COLUMN end_date TO end_to;


