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

/**
 * Controlli sugli indirizzi usati per i redirect dopo il login.
 */
public final class LocalUrls {

  private LocalUrls() {
  }

  /**
   * Verifica che l'indirizzo sia un percorso di questa applicazione, per evitare che un parametro
   * della richiesta possa portare l'utente su un altro sito.
   *
   * @param url l'indirizzo da verificare
   * @return true se l'indirizzo è un percorso locale, senza protocollo né host.
   */
  public static boolean isLocal(String url) {
    if (url == null || !url.startsWith("/") || url.startsWith("//") || url.contains("\\")) {
      return false;
    }
    return url.chars().noneMatch(Character::isISOControl);
  }
}
