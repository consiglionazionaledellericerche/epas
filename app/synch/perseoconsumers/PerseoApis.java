package synch.perseoconsumers;

import play.Play;

public class PerseoApis {

  private static final String PERSEO_BASE_URL = "perseo.base";
  private static final String OFFICES_ENDPOINT = "perseo.rest.departments";
  private static final String OFFICE_ENDPOINT = "perseo.rest.departmentbyperseoid";
  private static final String INSTITUTE_ENDPOINT = "perseo.rest.institutebyperseoid";
  private static final String ALL_DEPARTMENT_CONTRACTS_FOR_EPAS_ENDPOINT =
      "perseo.rest.allcontractsindepartmentforepas";
  private static final String CONTRACT_FOR_EPAS_ENDPOINT =
      "perseo.rest.contractforepasbyperseoid";
  private static final String ALL_DEPARTMENT_PEOPLE_FOR_EPAS_ENDPOINT =
      "perseo.rest.alldepartmentpeopleforepas";
  private static final String ALL_PEOPLE_FOR_EPAS_ENDPOINT =
      "perseo.rest.allpeopleforepas";
  private static final String PERSON_FOR_EPAS_ENDPOINT =
      "perseo.rest.personforepasbyperseoid";

  private static String getPerseoBaseUrl() {
    return Play.configuration.getProperty(PERSEO_BASE_URL);
  }

  public static String getOfficesEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(OFFICES_ENDPOINT);
  }

  public static String getOfficeEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(OFFICE_ENDPOINT);
  }

  public static String getInstituteEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(INSTITUTE_ENDPOINT);
  }

  public static String getAllDepartmentContractsForEpasEndpoint() {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(ALL_DEPARTMENT_CONTRACTS_FOR_EPAS_ENDPOINT);
  }

  public static String getContractForEpasEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(CONTRACT_FOR_EPAS_ENDPOINT);
  }

  public static String getAllDepartmentPeopleForEpasEndpoint() {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(ALL_DEPARTMENT_PEOPLE_FOR_EPAS_ENDPOINT);
  }

  public static String getAllPeopleForEpasEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(ALL_PEOPLE_FOR_EPAS_ENDPOINT);
  }

  public static String getPersonForEpasEndpoint() {
    return getPerseoBaseUrl() + Play.configuration.getProperty(PERSON_FOR_EPAS_ENDPOINT);
  }

}
