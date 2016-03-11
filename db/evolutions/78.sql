# ---!Ups

# -- Conversione a enumerato 

ALTER TABLE vacation_periods ADD column vacation_code TEXT;

UPDATE vacation_periods vpu SET vacation_code = t.description 
FROM (SELECT vp.id AS id, vc.description AS description FROM vacation_periods vp LEFT OUTER JOIN vacation_codes vc ON vp.vacation_codes_id = vc.id ) AS t 
WHERE vpu.id = t.id;

ALTER TABLE vacation_periods ALTER column vacation_code SET NOT NULL;

UPDATE vacation_periods SET vacation_code = 'CODE_28_4' WHERE vacation_code = '28+4';
UPDATE vacation_periods SET vacation_code = 'CODE_26_4' WHERE vacation_code = '26+4';
UPDATE vacation_periods SET vacation_code = 'CODE_25_4' WHERE vacation_code = '25+4';
UPDATE vacation_periods SET vacation_code = 'CODE_21_4' WHERE vacation_code = '21+4';
UPDATE vacation_periods SET vacation_code = 'CODE_22_3' WHERE vacation_code = '22+3';
UPDATE vacation_periods SET vacation_code = 'CODE_21_3' WHERE vacation_code = '21+3';

# -- Distruzione vecchia implementazione

ALTER TABLE vacation_periods DROP column vacation_codes_id;
DROP TABLE vacation_codes;
DROP TABLE vacation_codes_history;


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


