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

package controllers;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import helpers.OilConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.exports.ReportData;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.libs.Mail;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test sulle segnalazioni: l'invio resta disponibile senza autenticazione (pagina di login) ma
 * con limiti, le risposte ai ticket OIL richiedono l'autenticazione e parametri validi, alle
 * segnalazioni non vengono allegati token e segreti della sessione.
 */
@StaticInject
public class ReportCentreTest extends FunctionalTest {

  private static final String DEFAULT_REPORT_TO = "epas@iit.cnr.it";
  private static final String REPORT = "{\"note\": \"non riesco ad accedere\", \"url\": \"/login\", "
      + "\"html\": \"<html>pagina</html>\", \"img\": \"data:image/png;base64,AAAA\", "
      + "\"browser\": {\"userAgent\": \"test\"}}";

  @Inject
  private static H2Examples h2Examples;

  @Before
  public void resetMails() {
    Mail.Mock.reset();
  }

  /**
   * Senza autenticazione le pagine di risposta ai ticket OIL rimandano al login.
   */
  @Test
  public void oilUserReplyRequiresAuthentication() {
    Response response = GET(oilUserReplyUrl("1", "risposta"));
    assertStatus(302, response);
    assertTrue(response.getHeader("Location").contains("login"));

    response = POST(Router.reverse("ReportCentre.sendOilUserReplay").url,
        ImmutableMap.of("id", "1", "azione", "risposta", "description", "testo"));
    assertStatus(302, response);
    assertTrue(response.getHeader("Location").contains("login"));
  }

  /**
   * Con id o azione non validi (es. con ritorni a capo o separatori di OIL) la risposta non
   * viene inviata.
   */
  @Test
  public void oilUserReplyRejectsInvalidParameters() {
    Person person = employee();
    commit();
    login(person.getUser().getUsername());

    assertFalse(getContent(GET(oilUserReplyUrl("1", "a~~b"))).contains("description"));
    POST(Router.reverse("ReportCentre.sendOilUserReplay").url,
        ImmutableMap.of("id", "1\r\nBcc: x@example.org", "azione", "risposta",
            "description", "testo"));
    assertNull(Mail.Mock.getLastMessageReceivedBy(OilConfig.emailTo()));
  }

  /**
   * L'invio di una segnalazione resta possibile anche senza autenticazione, senza l'HTML
   * della pagina.
   */
  @Test
  public void sendReportWorksWithoutAuthentication() {
    Response response = POST(newRequest(uniqueAddress()), sendReportUrl(), "application/json",
        REPORT);
    assertIsOk(response);
    String mail = Mail.Mock.getLastMessageReceivedBy(DEFAULT_REPORT_TO);
    assertNotNull(mail);
    assertTrue(mail.contains("image.png"));
    assertFalse(mail.contains("page.html.gz"));
  }

  /**
   * Le segnalazioni anonime da uno stesso indirizzo sono limitate.
   */
  @Test
  public void anonymousReportsAreLimited() {
    String address = uniqueAddress();
    for (int i = 0; i < ReportCentre.MAX_ANONYMOUS_REPORTS; i++) {
      assertIsOk(POST(newRequest(address), sendReportUrl(), "application/json", REPORT));
    }
    assertStatus(429, POST(newRequest(address), sendReportUrl(), "application/json", REPORT));
    assertIsOk(POST(newRequest(uniqueAddress()), sendReportUrl(), "application/json", REPORT));
  }

  /**
   * Le segnalazioni oltre la dimensione massima vengono rifiutate.
   */
  @Test
  public void tooLargeReportsAreRejected() {
    String report = "{\"note\": \"" + Strings.repeat("x", ReportCentre.MAX_REPORT_CHARS) + "\"}";
    assertStatus(400, POST(newRequest(uniqueAddress()), sendReportUrl(), "application/json",
        report));
    assertNull(Mail.Mock.getLastMessageReceivedBy(DEFAULT_REPORT_TO));
  }

  /**
   * La segnalazione di un utente autenticato arriva all'amministratore del personale con la
   * sessione, ma senza le chiavi riservate.
   */
  @Test
  public void reportOfAuthenticatedUserHasSessionWithoutReservedKeys() {
    Person person = employee();
    Person admin = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    admin.setEmail("admin." + admin.id + "@example.org");
    admin.save();
    admin.changeOffice(person.getOffice(), new LocalDate(2026, 1, 1)).save();
    addRole(admin, Role.PERSONNEL_ADMIN);
    commit();

    login(person.getUser().getUsername());
    assertIsOk(POST(sendReportUrl(), "application/json", REPORT));
    String mail = Mail.Mock.getLastMessageReceivedBy(admin.getEmail());
    assertNotNull(mail);
    assertTrue(mail.contains(person.getUser().getUsername()));
    assertTrue(mail.contains("page.html.gz"));
    assertFalse(mail.contains("___"));
  }

  /**
   * Dalla sessione allegata alle segnalazioni sono esclusi i token OAuth, le chiavi interne
   * di Play e le chiavi che dal nome possono contenere credenziali.
   */
  @Test
  public void sessionForReportExcludesTokens() {
    Map<String, String> session = new HashMap<>();
    session.put("username", "mario.rossi");
    session.put("___id_token", "id-token");
    session.put("___refresh_token", "refresh-token");
    session.put("___AT", "authenticity-token");
    session.put("jwt", "x");
    session.put("accessToken", "x");
    session.put("userPassword", "x");

    assertEquals(ImmutableMap.of("username", "mario.rossi"),
        ReportData.sessionForReport(session));
  }

  private Person employee() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    person.setEmail("person." + person.id + "@example.org");
    person.save();
    addRole(person, Role.EMPLOYEE);
    return person;
  }

  private void addRole(Person person, String roleName) {
    Role role = Role.find("byName", roleName).first();
    if (role == null) {
      role = new Role();
      role.setName(roleName);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(person.getOffice());
    uro.setRole(role);
    uro.save();
  }

  private String oilUserReplyUrl(String id, String azione) {
    Map<String, Object> args = new HashMap<>();
    args.put("id", id);
    args.put("azione", azione);
    return Router.reverse("ReportCentre.oilUserReply", args).url;
  }

  private String sendReportUrl() {
    return Router.reverse("ReportCentre.sendReport").url;
  }

  private String uniqueAddress() {
    return "198.51.100." + (Math.abs(UUID.randomUUID().hashCode()) % 250 + 1);
  }

  private Request newRequest(String address) {
    Request request = newRequest();
    request.remoteAddress = address;
    return request;
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
