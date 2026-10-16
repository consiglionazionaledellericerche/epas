# --- !Ups

-- Hash delle password con salt (PBKDF2-HMAC-SHA256), non salvato nello storico.
-- Gli hash MD5 e SHA-512 esistenti restano finché ogni utente non effettua un login
-- o cambia la password.
ALTER TABLE users ADD COLUMN password_hash TEXT;

# --- !Downs

ALTER TABLE users DROP COLUMN password_hash;
