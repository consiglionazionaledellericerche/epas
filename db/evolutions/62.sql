# ---!Ups

-- Elimina gli user orfani nel caso di doppioni
DELETE from users WHERE id IN(
	SELECT U.id FROM persons S 
	RIGHT OUTER JOIN(
		SELECT id,username FROM users WHERE username IN (
  		SELECT username
  		FROM users
  		GROUP BY username
  		HAVING COUNT(*) > 1 
			)) U
		ON S.user_id=U.id
	where s.name is null);

ALTER TABLE users ADD CONSTRAINT users_unique_key UNIQUE (username);

# ---!Downs

ALTER TABLE users DROP CONSTRAINT users_unique_key;