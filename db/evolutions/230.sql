# --- !Ups

-- Hash delle password con salt (PBKDF2-HMAC-SHA256), non salvato nello storico.
-- Gli hash MD5 e SHA-512 esistenti restano finché ogni utente non effettua un login
-- o cambia la password.
ALTER TABLE users ADD COLUMN password_hash TEXT;

-- Gli hash non salati delle password non sono più salvati nello storico degli utenti:
-- si eliminano quelli già presenti.
UPDATE users_history SET password = NULL, password_sha512 = NULL
  WHERE password IS NOT NULL OR password_sha512 IS NOT NULL;

# --- !Downs

-- Gli hash eliminati dallo storico non vengono ripristinati
ALTER TABLE users DROP COLUMN password_hash;
