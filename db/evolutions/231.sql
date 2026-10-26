# --- !Ups

-- I contratti importati (es. da Perseo) venivano creati senza inquadramento, che però è
-- obbligatorio e ne impediva il salvataggio (es. inizializzazione dei buoni pasto).
-- Si deduce dalla presenza in attestati come fatto nella 221.
UPDATE contracts SET contract_type = 'structured_public_administration'
  WHERE contract_type IS NULL AND on_certificate = true;
UPDATE contracts SET contract_type = 'unstructured'
  WHERE contract_type IS NULL AND on_certificate = false;

# --- !Downs
# -- Non è necessaria una down----
