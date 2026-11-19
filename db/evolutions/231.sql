# --- !Ups

-- I giorni che non ricadono in alcun contratto (ad esempio quelli successivi alla chiusura di
-- un contratto) non vengono ricalcolati e restavano con i valori calcolati quando il contratto
-- era ancora attivo, continuando a maturare residuo (#693).
-- Si azzerano come fa il ricalcolo per i giorni fuori contratto: tempo a lavoro, differenza e
-- progressivo a zero, giorno non festivo, nessun tipo di modifica timbrature e buono pasto non
-- spettante (se non forzato dall'amministratore).
-- La fine del contratto è end_contract se presente, altrimenti end_date (nulla = contratto aperto).
UPDATE person_days pd SET
  time_at_work = 0,
  difference = 0,
  progressive = 0,
  is_holiday = false,
  stamp_modification_type_id = NULL,
  is_ticket_available = CASE WHEN COALESCE(pd.is_ticket_forced_by_admin, false)
    THEN pd.is_ticket_available ELSE false END
WHERE NOT EXISTS (
    SELECT 1 FROM contracts c
    WHERE c.person_id = pd.person_id
      AND c.begin_date <= pd.date
      AND (COALESCE(c.end_contract, c.end_date) IS NULL
        OR COALESCE(c.end_contract, c.end_date) >= pd.date))
  AND (COALESCE(pd.time_at_work, 0) <> 0
    OR COALESCE(pd.difference, 0) <> 0
    OR COALESCE(pd.progressive, 0) <> 0
    OR COALESCE(pd.is_holiday, false)
    OR pd.stamp_modification_type_id IS NOT NULL
    OR (COALESCE(pd.is_ticket_available, false)
      AND NOT COALESCE(pd.is_ticket_forced_by_admin, false)));

# --- !Downs

-- Non è necessaria una down: i valori azzerati non vengono ripristinati
