package synch.perseoconsumers;

import org.assertj.core.util.Strings;

import play.Play;

public class PerseoApis {

  private static final String PERSEO_BASE_URL = "perseo.base";
  private static final String PERSEO_USER = "perseo.user";
  private static final String PERSEO_PASS = "perseo.pass";
  private static final String OFFICES_ENDPOINT = "perseo.rest.departments";
  private static final String OFFICE_ENDPOINT = "perseo.rest.departmentbyperseoid";
  private static final String INSTITUTE_ENDPOINT = "perseo.rest.institutebyperseoid";
  private static final String ALL_DEPARTMENT_CONTRACTS_FOR_EPAS_ENDPOINT =
      "perseo.rest.allcontractsindepartmentforepas";
  private static final String ALL_CONTRACTS_FOR_EPAS_ENDPOINT =
      "perseo.rest.allcontractsforepas";
  private static final String CONTRACT_FOR_EPAS_ENDPOINT =
      "perseo.rest.contractforepasbyperseoid";
  private static final String ALL_DEPARTMENT_PEOPLE_FOR_EPAS_ENDPOINT =
      "perseo.rest.alldepartmentpeopleforepas";
  private static final String ALL_PEOPLE_FOR_EPAS_ENDPOINT =
      "perseo.rest.allpeopleforepas";
  private static final String PERSON_FOR_EPAS_ENDPOINT =
      "perseo.rest.personforepasbyperseoid";
  private static final String ALL_ROLES_ENDPOINT = "perseo.rest.allrolesforepas";

  private static String getPerseoBaseUrl() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(PERSEO_BASE_URL))) {
      throw new NoSuchFieldException(PERSEO_BASE_URL);
    }
    return Play.configuration.getProperty(PERSEO_BASE_URL);
  }

  public static String getPerseoUser() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(PERSEO_USER))) {
      throw new NoSuchFieldException(PERSEO_USER);
    }
    return Play.configuration.getProperty(PERSEO_USER);
  }

  public static String getPerseoPass() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(PERSEO_PASS))) {
      throw new NoSuchFieldException(PERSEO_PASS);
    }
    return Play.configuration.getProperty(PERSEO_PASS);
  }

  public static String getOfficesEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(OFFICES_ENDPOINT);
  }

  public static String getOfficeEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(OFFICE_ENDPOINT);
  }

  public static String getInstituteEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(INSTITUTE_ENDPOINT);
  }

  public static String getAllContractsForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(ALL_CONTRACTS_FOR_EPAS_ENDPOINT);
  }
  
  public static String getAllDepartmentContractsForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(ALL_DEPARTMENT_CONTRACTS_FOR_EPAS_ENDPOINT);
  }

  public static String getContractForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(CONTRACT_FOR_EPAS_ENDPOINT);
  }

  public static String getAllDepartmentPeopleForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(ALL_DEPARTMENT_PEOPLE_FOR_EPAS_ENDPOINT);
  }

  public static String getAllPeopleForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(ALL_PEOPLE_FOR_EPAS_ENDPOINT);
  }

  public static String getPersonForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(PERSON_FOR_EPAS_ENDPOINT);
  }
  
  public static String getAllRolesEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(ALL_ROLES_ENDPOINT);
  }

}
