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
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Person;
import models.Role;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Test funzionali sulle timbrature inviate dai client (lettori badge e REST v3): date troppo
 * nel passato o nel futuro rifiutate, testo normalizzato.
 */
@StaticInject
public class StampingsFromClientTest extends FunctionalTest {

  private static final String READER_PASSWORD = "passwordDelLettore";
  private static final String BADGE = "4711";

  @Inject
  private static H2Examples h2Examples;

  /**
   * Timbrature nel futuro entro la tolleranza (orologio del lettore leggermente avanti)
   * vengono accettate.
   */
  @Test
  public void stampingsSlightlyInTheFutureAreAccepted() {
    Fixture fixture = fixture();
    Response response = PUT(reader(fixture), "/stampingsfromclient/createnotrecompute",
        "application/json", json(LocalDateTime.now().plusMinutes(30), null, null));
    assertIsOk(response);
    assertEquals(1, stampings(fixture.person).size());
  }

  /**
   * Timbrature nel futuro oltre la tolleranza vengono rifiutate da tutti i punti d'ingresso.
   */
  @Test
  public void stampingsFarInTheFutureAreRejected() {
    Fixture fixture = fixture();
    LocalDateTime future = LocalDateTime.now().plusHours(3);
    assertStatus(400, PUT(reader(fixture), "/stampings/create", "application/json",
        json(future, null, null)));
    assertStatus(400, PUT(reader(fixture), "/stampingsfromclient/createnotrecompute",
        "application/json", json(future, null, null)));
    assertStatus(400, POST(reader(fixture), "/rest/v3/stampings/create", "application/json",
        "{\"badgeNumber\": \"" + BADGE + "\", \"wayType\": \"in\", \"dateTime\": \""
        + future.toString("yyyy-MM-dd'T'HH:mm") + "\"}"));
    assertTrue(stampings(fixture.person).isEmpty());
  }

  /**
   * Anche il caricamento senza ricalcolo rifiuta le timbrature troppo nel passato.
   */
  @Test
  public void createNotRecomputeRejectsStampingsTooFarInThePast() {
    Fixture fixture = fixture();
    assertStatus(400, PUT(reader(fixture), "/stampingsfromclient/createnotrecompute",
        "application/json", json(LocalDateTime.now().minusYears(2), null, null)));
    assertTrue(stampings(fixture.person).isEmpty());
  }

  /**
   * I campi di testo sono salvati senza caratteri di controllo e con lunghezza massima.
   */
  @Test
  public void clientTextIsNormalizedWhenSaved() {
    Fixture fixture = fixture();
    Response response = PUT(reader(fixture), "/stampingsfromclient/createnotrecompute",
        "application/json", json(LocalDateTime.now().minusHours(1),
            "uscita\\nservizio\\t esterno", Strings.repeat("p", 300)));
    assertIsOk(response);
    List<Stamping> stampings = stampings(fixture.person);
    assertEquals(1, stampings.size());
    assertEquals("uscita servizio esterno", stampings.get(0).getNote());
    assertEquals(255, stampings.get(0).getPlace().length());
  }

  private static final class Fixture {
    Person person;
    String readerUsername;
  }

  private Fixture fixture() {
    Fixture fixture = new Fixture();
    fixture.person = h2Examples.activeNormalEmployee(new LocalDate(2020, 1, 1));

    User readerUser = new User();
    readerUser.setUsername("reader." + UUID.randomUUID());
    readerUser.updatePassword(READER_PASSWORD);
    readerUser.save();
    Role role = Role.find("byName", Role.BADGE_READER).first();
    if (role == null) {
      role = new Role();
      role.setName(Role.BADGE_READER);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(readerUser);
    uro.setOffice(fixture.person.getOffice());
    uro.setRole(role);
    uro.save();

    BadgeReader badgeReader = new BadgeReader();
    badgeReader.setCode("lettore-" + UUID.randomUUID());
    badgeReader.setUser(readerUser);
    badgeReader.save();
    BadgeSystem badgeSystem = new BadgeSystem();
    badgeSystem.setName("sistema-" + UUID.randomUUID());
    badgeSystem.setOffice(fixture.person.getOffice());
    badgeSystem.save();
    Badge badge = new Badge();
    badge.setCode(BADGE);
    badge.setPerson(fixture.person);
    badge.setBadgeReader(badgeReader);
    badge.setBadgeSystem(badgeSystem);
    badge.save();

    fixture.readerUsername = readerUser.getUsername();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
    return fixture;
  }

  private Request reader(Fixture fixture) {
    Request request = newRequest();
    request.user = fixture.readerUsername;
    request.password = READER_PASSWORD;
    return request;
  }

  private String json(LocalDateTime dateTime, String note, String place) {
    StringBuilder json = new StringBuilder("{\"matricolaFirma\": \"" + BADGE + "\", "
        + "\"operazione\": 0, \"anno\": " + dateTime.getYear() + ", \"mese\": "
        + dateTime.getMonthOfYear() + ", \"giorno\": " + dateTime.getDayOfMonth()
        + ", \"ora\": " + dateTime.getHourOfDay() + ", \"minuti\": "
        + dateTime.getMinuteOfHour());
    if (note != null) {
      json.append(", \"note\": \"").append(note).append('"');
    }
    if (place != null) {
      json.append(", \"luogo\": \"").append(place).append('"');
    }
    return json.append('}').toString();
  }

  private List<Stamping> stampings(Person person) {
    JPA.em().clear();
    return Stamping.find("personDay.person.id = ?1", person.id).fetch();
  }
}
