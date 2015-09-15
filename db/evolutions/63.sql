# ---!Ups

-- divisione dell'inizializzazione in residui e buoni pasto
ALTER TABLE contracts RENAME COLUMN source_date TO source_date_residual;
ALTER TABLE contracts ADD COLUMN source_date_meal_ticket date;

UPDATE contracts SET source_date_meal_ticket = source_date_residual 
WHERE source_date_residual IS NOT null;

-- inserimento nell'entity person dei campi created_at e updated_at
-- il valore iniziale dei due campi viene recuperato dallo storico
ALTER TABLE persons ADD column created_at timestamp without time zone;
ALTER TABLE persons ADD column updated_at timestamp without time zone;

update persons set created_at = T.tempo, updated_at = T.tempo
from ( select p.id as ide, to_timestamp(r.revtstmp/1000) as tempo 
       from persons p 
       left outer join persons_history ph on p.id = ph.id 
       left outer join revinfo r on r.rev = ph._revision 
       where ph._revision_type = 0 ) 
as T 
where id = T.ide;

# ---!Downs

ALTER TABLE contracts RENAME COLUMN source_date_residual TO source_date;
ALTER TABLE contracts DROP COLUMN source_date_meal_ticket;

ALTER TABLE persons DROP column created_at;
ALTER TABLE persons DROP column updated_at;

 
