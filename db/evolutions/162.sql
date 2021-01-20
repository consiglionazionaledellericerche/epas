# --- !Ups

ALTER TABLE contract_stamp_profiles_history ADD end_date DATE;

ALTER TABLE persons_history ADD begin_date DATE;
ALTER TABLE persons_history ADD end_date DATE;

-- Correzione dello storico generato nell'evoluzione 104.
-- Le righe iniziali inserite nelle tabelle competences_history, 
-- contracts_history, vacation_periods_history erano state erroneamente
-- legate a l'ultima revisione esistente in quel momento.
-- Cambiamo l'owner di quella revisione per non generare confusione negli
-- utenti. La revisione è differente nelle varie installazioni presenti.
-- Le update successive correggono i 2 casi attualmente in produzione che
-- hanno applicato l'evoluzione 104 con dei dati esistenti.

-- Correzione storico db epas sede centrale CNR
UPDATE revinfo set owner_id = 10001, ipaddress = '127.0.0.1' where rev = 2658928 and owner_id = 13281;

-- Correzione storico db epas IIT
UPDATE revinfo set owner_id = 1, ipaddress = '127.0.0.1' where rev = 351704 and owner_id = 10004;

# --- !Downs

ALTER TABLE contract_stamp_profiles_history DROP COLUMN end_date;

ALTER TABLE persons_history DROP COLUMN begin_date;
ALTER TABLE persons_history DROP COLUMN end_date;
