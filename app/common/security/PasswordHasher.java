/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package common.security;

import com.google.common.base.Splitter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash delle password con PBKDF2-HMAC-SHA256 e salt casuale.
 *
 * <p>L'hash è salvato nel formato {@code pbkdf2_sha256$iterazioni$salt$hash} (salt e hash in
 * base64), così da poter aumentare in futuro il numero di iterazioni senza invalidare gli
 * hash esistenti.</p>
 */
public final class PasswordHasher {

  static final String ALGORITHM = "pbkdf2_sha256";
  static final int ITERATIONS = 210_000;
  private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int SALT_BYTES = 16;
  private static final int HASH_BITS = 256;
  private static final String SEPARATOR = "$";
  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {
  }

  /**
   * L'hash della password con un nuovo salt casuale.
   *
   * @param password la password in chiaro
   * @return l'hash nel formato {@code pbkdf2_sha256$iterazioni$salt$hash}
   */
  public static String hash(String password) {
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] hash = pbkdf2(password, salt, ITERATIONS);
    return String.join(SEPARATOR, ALGORITHM, String.valueOf(ITERATIONS),
        Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash));
  }

  /**
   * Verifica la password rispetto all'hash salvato, con un confronto a tempo costante.
   *
   * @param password la password in chiaro
   * @param storedHash l'hash salvato
   * @return true se la password corrisponde, false altrimenti (anche per hash non validi).
   */
  public static boolean matches(String password, String storedHash) {
    if (password == null || password.isEmpty() || storedHash == null) {
      return false;
    }
    List<String> parts = Splitter.on(SEPARATOR).splitToList(storedHash);
    if (parts.size() != 4 || !ALGORITHM.equals(parts.get(0))) {
      return false;
    }
    try {
      int iterations = Integer.parseInt(parts.get(1));
      byte[] salt = Base64.getDecoder().decode(parts.get(2));
      byte[] expected = Base64.getDecoder().decode(parts.get(3));
      return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Indica se l'hash è stato calcolato con meno iterazioni di quelle attuali.
   *
   * @param storedHash l'hash salvato
   * @return true se l'hash va ricalcolato con i parametri attuali.
   */
  public static boolean needsRehash(String storedHash) {
    List<String> parts = Splitter.on(SEPARATOR).splitToList(storedHash);
    try {
      return parts.size() != 4 || !ALGORITHM.equals(parts.get(0))
          || Integer.parseInt(parts.get(1)) < ITERATIONS;
    } catch (NumberFormatException e) {
      return true;
    }
  }

  private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
    try {
      return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Algoritmo " + JCA_ALGORITHM + " non disponibile", e);
    } finally {
      spec.clearPassword();
    }
  }
}
