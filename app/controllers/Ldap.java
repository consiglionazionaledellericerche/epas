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
import play.mvc.Router;
import play.mvc.Router.Route;

/**
 * Contiene i metodi per l'autentication tramite LDAP.
 * 
 * @author cristian
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
      url = Play.configuration.getProperty("ldap.login.return","/");
    }

    log.debug("Ldap: Redirecting user back to destination location: " + url);
    redirect(url);
  }

  /**
   * Autenticazione tramite LDAP.
   * 
   */
  public static void authenticate(String username, String password) {
    log.debug("Richiesta autenticazione tramite credenziali LDAP username={}", username);

    Optional<LdapUser> ldapUser = ldapService.authenticate(username, password);

    if (!ldapUser.isPresent()) {
      log.info("Failed login for {}", username);
      flash.error("Oops! Username o password sconosciuti");
      redirect("/login");
    }

    if (ldapUser.get().getMail() == null) {
      log.info("Failed login for {}, mail attribute not set in LDAP", username);
      flash.error("Oops! email per %s non presente in LDAP, contattare l'helpdesk", username);
      redirect("/login");
    }
    
    val eppn = ldapUser.get().getEppn();
    
    log.debug("LDAP user = {}", ldapUser.get());
    
    Person person = personDao.byEppn(eppn).orNull();

    if (person != null) {

      Cache.set(person.user.username, person, Security.CACHE_DURATION);
      Cache.set("personId", person.id, Security.CACHE_DURATION);

      session.put("username", person.user.username);
      session.put("ldap", true);

      flash.success("Benvenuto " + person.name + ' ' + person.surname);
      log.info("user {} successfully logged in using LDAP from ip {}", person.getFullname(),
          Http.Request.current().remoteAddress);
      log.trace("Permission list for {} {}: {}",
          person.name, person.surname, person.user.usersRolesOffices);
      
      redirectToOriginalUrl();
      
    } else {
      log.warn("Person with mail {} successfully logged in LDAP but unknonw to ePAS", eppn);
      flash.error("Oops! email %s non riconosciuta da ePAS contattare l'helpdesk", eppn);
      redirect("/login");
    }
  }
}
