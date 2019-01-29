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
  
  
  public static String code() {
    return Play.configuration.getProperty(COMPANY_CODE, COMPANY_DEFAULT_CODE);
  }
  
  public static String name() {
    return Play.configuration.getProperty(COMPANY_NAME, COMPANY_DEFAULT_NAME);
  }
  
  public static String url() {
    return Play.configuration.getProperty(COMPANY_URL, COMPANY_DEFAULT_URL);
  }
}
