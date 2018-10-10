# ---!Ups

-- Era stata utilizzata una revisione esistente per modificare massivamente tutte le timbrature
-- fuori sede. Impostato owner_id = NULL per la revisione per farla risultare effettuata da ePAS 
UPDATE revinfo SET owner_id = NULL where rev = 3281986 AND owner_id = 10980;

# ---!Downs

-- Non è necessario tornare indietro
