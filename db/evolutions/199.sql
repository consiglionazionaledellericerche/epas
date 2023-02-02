# --- !Ups

UPDATE persons SET fiscal_code  = NULL WHERE fiscal_code = '';
UPDATE persons SET eppn  = NULL WHERE eppn = '';

# --- !Downs

-- non è necessaria una down