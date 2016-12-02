package manager.attestati.service;

import org.assertj.core.util.Strings;

import play.Play;

public class AttestatiApis {

  private static final String ERROR = "Missing required parameter: ";
  private static final String ATTESTATI_BASE_URL = "attestati.base";
  private static final String ATTESTATI_PASS = "attestati.pass";
  private static final String ATTESTATI_USER = "attestati.user";
  
  public static String getAttestatiBaseUrl() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(ATTESTATI_BASE_URL))) {
      throw new NoSuchFieldException(ERROR + ATTESTATI_BASE_URL);
    }
    return Play.configuration.getProperty(ATTESTATI_BASE_URL);
  }

  public static String getAttestatiUser() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(ATTESTATI_USER))) {
      throw new NoSuchFieldException(ERROR + ATTESTATI_USER);
    }
    return Play.configuration.getProperty(ATTESTATI_USER);
  }

  public static String getAttestatiPass() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(ATTESTATI_PASS))) {
      throw new NoSuchFieldException(ERROR + ATTESTATI_PASS);
    }
    return Play.configuration.getProperty(ATTESTATI_PASS);
  }

}
