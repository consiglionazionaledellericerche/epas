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

package controllers;

import com.google.common.base.Optional;
import dao.PersonDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ldap.LdapService;
import manager.ldap.LdapUser;
import models.Person;
import play.Play;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Util;

/**
 * Contiene i metodi per l'autentication tramite LDAP.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class Ldap extends Controller {

  @Inject
  static PersonDao personDao;

  @Inject
  static LdapService ldapService;

  /**
   * Redirect to the original user's url.
   */
  private static void redirectToOriginalUrl() {
    String url = flash.get("url");
    if (url == null) {
      url = request.params.get("return");
    }
    if (url == null) {
      url = 
          Play.configuration.getProperty("ldap.login.return", 
              Play.configuration.getProperty("http.path", "/"));
      log.debug("url from ldap.login.return = {}", url);
    }

    if (url.equals(Play.configuration.getProperty("http.path", "/")) 
        && !Play.configuration.getProperty("http.path", "/").endsWith("/")) {
      url = Play.configuration.getProperty("http.path") + "/";
      log.debug("Play.configuration.getProperty('http.path') = {}, url={}", 
          Play.configuration.getProperty("http.path"), url);
    }

    log.debug("Ldap: Redirecting user back to destination location: " + url);
    redirect(url);
  }

  /**
   * Autenticazione tramite LDAP.
   */
  public static void authenticate(String username, String password) {
    log.debug("Richiesta autenticazione tramite credenziali LDAP username={}", username);

    Optional<LdapUser> ldapUser = ldapService.authenticate(username, password);

    if (!ldapUser.isPresent()) {
      log.info("Failed login for {}", username);
      flash.error("Oops! Username o password sconosciuti");
      redirect("/login");
    }

    log.debug("LDAP user = {}", ldapUser.get());

    Person person = getPersonByLdapUser(ldapUser.get(), Optional.of("/login"));

    //Se la person è diversa da null il metodo getPersonByLdapUser dovrebbe 
    //aver già fatto la redirect.
    if (person != null) {
      flash.success("Benvenuto " + person.getName() + ' ' + person.getSurname());
      log.info("user {} successfully logged in using LDAP from ip {}", person.getFullname(),
          Http.Request.current().remoteAddress);
      redirectToOriginalUrl();
    }
  }
  
  /**
   * Verifica che sia presente una Person con eppn uguale
   * a quella dell'ldapUser passato.
   * <p>Se la Person non esiste effettua direttamente la login
   * su failedLoginRedirect se presente, altrimenti su /login</p>
   *
   * @param ldapUser utente ldap di cui verificare l'esistenza su ePAS
   * @param failedLoginRedirect url a cui fare il login se non è stato
   *      possibile trovare una Person corrispondente all'ldapUser passato.
   */
  @Util
  public static Person getPersonByLdapUser(LdapUser ldapUser, 
      Optional<String> failedLoginRedirect) {
    val eppn = ldapUser.getEppn();
    
    if (eppn == null) {
      log.warn("Failed login for {}, {} attribute not set in LDAP", 
          ldapUser.getUid(), ldapService.getEppnAttributeName());
      flash.error("Oops! %s per %s non presente in LDAP. Contattare l'helpdesk.", 
          ldapService.getEppnAttributeName(), ldapUser.getUid());
      redirect(failedLoginRedirect.or("/login"));
    }

    log.debug("LDAP user = {}", ldapUser);
    
    Person person = personDao.byEppn(eppn).orNull();
    if (person != null) {
      Cache.set(person.getUser().getUsername(), person, Security.CACHE_DURATION);
      Cache.set("personId", person.id, Security.CACHE_DURATION);

      session.put("username", person.getUser().getUsername());
      session.put("ldap", true);

      return person;
    } else {
      log.warn("Person with {} {} successfully logged in LDAP but unknonw to ePAS", 
          ldapService.getEppnAttributeName(), eppn);
      flash.error("Oops! %s %s non riconosciuto da ePAS. Contattare l'helpdesk.", 
          ldapService.getEppnAttributeName(), eppn);
      redirect(failedLoginRedirect.or("/login"));
      //Non dovrebbe mai arrivare alla return successiva
      return null;
    }
  }
}
