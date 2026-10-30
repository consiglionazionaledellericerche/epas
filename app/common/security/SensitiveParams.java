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

import java.util.Locale;

/**
 * Riconosce i parametri HTTP che contengono credenziali, da non scrivere nei log o nei cookie.
 */
public final class SensitiveParams {

  private static final String[] SENSITIVE_NAMES = {"password", "token", "secret"};
  private static final String MASK = "***";

  private SensitiveParams() {
  }

  /**
   * Verifica se il parametro contiene una credenziale.
   *
   * @param name il nome del parametro, anche composto (es. user.password)
   * @return true se il nome indica una password, un token o un secret.
   */
  public static boolean isSensitive(String name) {
    final String lowerCaseName = name.toLowerCase(Locale.ROOT);
    for (String sensitiveName : SENSITIVE_NAMES) {
      if (lowerCaseName.contains(sensitiveName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Il valore da scrivere nei log per il parametro.
   *
   * @param name il nome del parametro
   * @param value il valore del parametro
   * @return il valore del parametro, oscurato se è una credenziale.
   */
  public static String loggable(String name, String value) {
    return isSensitive(name) ? MASK : value;
  }
}
