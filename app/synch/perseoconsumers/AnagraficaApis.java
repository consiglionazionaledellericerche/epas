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

package synch.perseoconsumers;

import com.google.common.base.Strings;
import play.Play;

/**
 * Endpoint per l'API dell'Anagrafica.
 */
public class AnagraficaApis {

  private static final String PERSEO_BASE_URL = "anagrafica.base";
  private static final String PERSEO_USER = "anagrafica.user";
  private static final String PERSEO_PASS = "anagrafica.pass";
  private static final String OFFICES_ENDPOINT = "anagrafica.offices";
  private static final String OFFICE_ENDPOINT = "anagrafica.office";
  private static final String INSTITUTE_ENDPOINT = "anagrafica.institute";
  private static final String CONTRACTS_IN_OFFICE = "anagrafica.contractsinoffice";
  private static final String CONTRACTS = "anagrafica.contracts";
  private static final String CONTRACT = "anagrafica.contract";
  private static final String CONTRACTS_BY_PERSON_ID = "anagrafica.contractsByPersonId";
  private static final String PEOPLE_IN_OFFICE = "anagrafica.peopleinoffice";
  private static final String PEOPLE = "anagrafica.people";
  private static final String PERSON_FOR_EPAS_ENDPOINT = "anagrafica.person";
  private static final String ALL_ROLES_ENDPOINT = "anagrafica.roles";
  private static final String OFFICE_BADGES = "anagrafica.badgesinoffice";
  private static final String PERSON_BADGE = "anagrafica.badge";

  private static String getPerseoBaseUrl() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(PERSEO_BASE_URL))) {
      throw new NoSuchFieldException(PERSEO_BASE_URL);
    }
    return Play.configuration.getProperty(PERSEO_BASE_URL);
  }

  /** 
   * Utente per autenticazione anagrafica.
   *
   * @return l'utente con cui autenticarsi con l'anagrafica. 
   */
  public static String getPerseoUser() throws NoSuchFieldException {
    if (Strings.isNullOrEmpty(Play.configuration.getProperty(PERSEO_USER))) {
      throw new NoSuchFieldException(PERSEO_USER);
    }
    return Play.configuration.getProperty(PERSEO_USER);
  }
  
  /** 
   * Password per autenticazione anagrafica.
   *
   * @return password con cui autenticarsi con l'anagrafica. 
   */
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
        + Play.configuration.getProperty(CONTRACTS);
  }

  public static String getAllDepartmentContractsForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(CONTRACTS_IN_OFFICE);
  }

  public static String getContractForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(CONTRACT);
  }

  public static String getContractsByPersonIdForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(CONTRACTS_BY_PERSON_ID);
  }
  
  public static String getAllDepartmentPeopleForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl()
        + Play.configuration.getProperty(PEOPLE_IN_OFFICE);
  }

  @Deprecated
  public static String getPeople() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(PEOPLE);
  }

  public static String getPersonForEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(PERSON_FOR_EPAS_ENDPOINT);
  }

  public static String getAllRolesEpasEndpoint() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(ALL_ROLES_ENDPOINT);
  }

  public static String getDepartmentsBadges() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(OFFICE_BADGES);
  }

  public static String getPersonBadge() throws NoSuchFieldException {
    return getPerseoBaseUrl() + Play.configuration.getProperty(PERSON_BADGE);
  }
}
