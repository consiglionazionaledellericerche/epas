/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package helpers;

import play.Play;

/**
 * Nella configurazione ci possono essere:
 * <dl>
 *  <dt>company.code</dt><dd>Sigla dell'ente/azienda che utilzza ePAS, default CNR</dd>
 *  <dt>company.name</dt><dd>Nome dell'ente/azienda che utilzza ePAS, default 
 *      Consiglio Nazionale delle Ricerche</dd>
 *  <dt>company.url</dt><dd>Indirizzo del sito web dell'ente azienda, default https://www.cnr.it</dd>
 * </dl>
 * Comunque ci sono dei default.
*/
public class CompanyConfig {

  /**
   * È possibile configurare i dati dell'ente che utilizza ePAS inserendo 
   * questi parametri nella configurazione del play.
   */
  private static final String COMPANY_CODE = "company.code";
  private static final String COMPANY_NAME = "company.name";
  private static final String COMPANY_URL = "company.url";
   
  // default per parametri dell'ente/azienda
  private static final String COMPANY_DEFAULT_CODE = "CNR";
  private static final String COMPANY_DEFAULT_NAME = "Consiglio Nazionale delle Ricerche";
  private static final String COMPANY_DEFAULT_URL = "https://www.cnr.it";


  /**
   * Preleva il COMPANY_CODE dalla configurazione. 
   */
  public static String code() {
    return Play.configuration.getProperty(COMPANY_CODE, COMPANY_DEFAULT_CODE);
  }

  /**
   * Preleva il COMPANY_NAME dalla configurazione. 
   */
  public static String name() {
    return Play.configuration.getProperty(COMPANY_NAME, COMPANY_DEFAULT_NAME);
  }

  /**
   * Preleva il COMPANY_URL dalla configurazione. 
   */
  public static String url() {
    return Play.configuration.getProperty(COMPANY_URL, COMPANY_DEFAULT_URL);
  }
}
