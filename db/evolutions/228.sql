# --- !Ups

-- Permette più periodi di validità per lo stesso parametro della persona (es. AGILE_WORK_MONTHLY_LIMIT)
ALTER TABLE person_configurations DROP CONSTRAINT unique_param;
ALTER TABLE person_configurations
ADD CONSTRAINT unique_param UNIQUE (person_id, epas_param, begin_date);

# --- !Downs

-- Prima di ripristinare il vincolo si riduce ad un solo periodo ogni coppia (persona, parametro)
-- con più periodi: si mantiene il valore del periodo più recente esteso all'intervallo
-- complessivo dei periodi. I valori dei periodi precedenti vengono persi.
CREATE TEMP TABLE person_configurations_228 AS
SELECT person_id, epas_param, MIN(begin_date) AS begin_date, MAX(begin_date) AS last_begin_date,
  CASE WHEN BOOL_OR(end_date IS NULL) THEN NULL ELSE MAX(end_date) END AS end_date
FROM person_configurations
GROUP BY person_id, epas_param
HAVING COUNT(*) > 1;

DELETE FROM person_configurations pc USING person_configurations_228 d
WHERE pc.person_id = d.person_id AND pc.epas_param = d.epas_param
  AND pc.begin_date < d.last_begin_date;

UPDATE person_configurations pc SET begin_date = d.begin_date, end_date = d.end_date
FROM person_configurations_228 d
WHERE pc.person_id = d.person_id AND pc.epas_param = d.epas_param;

DROP TABLE person_configurations_228;

ALTER TABLE person_configurations DROP CONSTRAINT unique_param;
ALTER TABLE person_configurations
ADD CONSTRAINT unique_param UNIQUE (person_id, epas_param);
