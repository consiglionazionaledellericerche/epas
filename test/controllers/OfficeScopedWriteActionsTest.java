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

import com.google.common.collect.ImmutableMap;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.MealTicketCard;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.Role;
import models.ShiftCategories;
import models.ShiftType;
import models.TeleworkValidation;
import models.flows.Affiliation;
import models.flows.Group;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sulle azioni che modificano o mostrano dati di una sede: un ruolo su una sede
 * non permette di usarle sui dati di altre sedi, nemmeno indicando l'id di un record esistente
 * nei form, e le cancellazioni avvengono solo dal form di conferma (POST).
 */
@StaticInject
public class OfficeScopedWriteActionsTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;

  /**
   * Tessere dei buoni pasto: niente accesso ad altre sedi, cancellazione solo da POST.
   */
  @Test
  public void mealTicketCardsAreScopedAndDeletedOnlyByPost() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    MealTicketCard ownCard = card(colleague);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    MealTicketCard otherCard = card(other);
    String manager = roleOn(colleague, Role.MEAL_TICKET_MANAGER);
    commit();

    login(manager);
    assertStatus(403, GET(url("MealTicketCards.infoCard", "mealTicketCardId", otherCard.id)));
    assertStatus(403, GET(url("MealTicketCards.deleteCard", "mealTicketCardId", otherCard.id)));
    assertStatus(403, POST(Router.reverse("MealTicketCards.deleteCard").url,
        ImmutableMap.of("mealTicketCardId", otherCard.id.toString(), "confirmed", "true")));

    // la GET mostra solo la conferma, anche con confirmed=true
    assertIsOk(GET(url("MealTicketCards.deleteCard", "mealTicketCardId", ownCard.id)
        + "&confirmed=true"));
    assertNotNull(reload(MealTicketCard.class, ownCard.id));
    assertStatus(302, POST(Router.reverse("MealTicketCards.deleteCard").url,
        ImmutableMap.of("mealTicketCardId", ownCard.id.toString(), "confirmed", "true")));
    assertNull(reload(MealTicketCard.class, ownCard.id));
    assertNotNull(reload(MealTicketCard.class, otherCard.id));
  }

  /**
   * Affiliazioni: niente cancellazione o modifica di quelle di altre sedi, nemmeno indicando
   * l'id nel form di salvataggio; cancellazione solo da POST.
   */
  @Test
  public void affiliationsAreScopedAndDeletedOnlyByPost() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Affiliation own = affiliation(colleague);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    Affiliation otherAffiliation = affiliation(other);
    Group ownOtherGroup = group(colleague);
    String admin = roleOn(colleague, Role.PERSONNEL_ADMIN);
    commit();

    login(admin);
    assertStatus(403, GET(url("Affiliations.edit", "id", otherAffiliation.id)));
    assertStatus(403, POST(Router.reverse("Affiliations.delete").url,
        ImmutableMap.of("id", otherAffiliation.id.toString(), "confirmed", "true")));
    // riscrittura dell'affiliazione di un'altra sede con persona e gruppo della propria
    assertStatus(403, POST(Router.reverse("Affiliations.save").url, ImmutableMap.of(
        "affiliation.id", otherAffiliation.id.toString(),
        "affiliation.person.id", colleague.id.toString(),
        "affiliation.group.id", ownOtherGroup.id.toString(),
        "affiliation.beginDate", "2026-01-01")));
    assertEquals(other.id, reload(Affiliation.class, otherAffiliation.id).getPerson().id);

    assertIsOk(GET(url("Affiliations.delete", "id", own.id) + "&confirmed=true"));
    assertNotNull(reload(Affiliation.class, own.id));
    assertStatus(302, POST(Router.reverse("Affiliations.delete").url,
        ImmutableMap.of("id", own.id.toString(), "confirmed", "true")));
    assertNull(reload(Affiliation.class, own.id));
  }

  /**
   * Figli dei dipendenti: niente modifica di quelli di altre sedi, nemmeno indicando l'id nel
   * form di salvataggio.
   */
  @Test
  public void childrenAreScoped() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    PersonChildren otherChild = child(other);
    String manager = roleOn(colleague, Role.REGISTRY_MANAGER);
    commit();

    login(manager);
    assertStatus(403, GET(url("Persons.editChild", "childId", otherChild.id)));
    assertStatus(403, POST(Router.reverse("Persons.saveChild").url, ImmutableMap.of(
        "child.id", otherChild.id.toString(), "child.person.id", colleague.id.toString(),
        "child.name", "Nome", "child.surname", "Cognome", "child.bornDate", "01/01/2020")));
    assertEquals(other.id, reload(PersonChildren.class, otherChild.id).getPerson().id);
  }

  /**
   * Validazioni del telelavoro, storico dei giorni, utenti, competenze e attività di turno di
   * altre sedi non sono accessibili.
   */
  @Test
  public void otherOfficeDataIsNotAccessible() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    TeleworkValidation validation = new TeleworkValidation();
    validation.setPerson(other);
    validation.setYear(2026);
    validation.setMonth(9);
    validation.save();
    PersonDay personDay = new PersonDay(other, new LocalDate(2026, 9, 1));
    personDay.save();
    ShiftType shiftType = shiftType(other);
    String supervisor = roleOn(colleague, Role.SEAT_SUPERVISOR);
    String personnelAdmin = roleOn(colleague, Role.PERSONNEL_ADMIN);
    String technicalAdmin = roleOn(colleague, Role.TECHNICAL_ADMIN);
    commit();

    login(supervisor);
    assertStatus(403, GET(url("InformationRequests.revokeValidation", "validationId",
        validation.id)));
    assertNotNull(reload(TeleworkValidation.class, validation.id));
    assertStatus(403, GET(url("PersonDays.personDayHistory", "personDayId", personDay.id)));

    login(personnelAdmin);
    Map<String, Object> args = new HashMap<>();
    args.put("year", 2026);
    args.put("month", 9);
    args.put("officeId", other.getOffice().id);
    assertStatus(403, GET(Router.reverse("Competences.getCompetenceGroupInYearMonth", args).url));
    assertStatus(403, POST(Router.reverse("Competences.editActivity").url,
        ImmutableMap.of("type.id", shiftType.id.toString(), "considerEverySlot", "true")));

    login(technicalAdmin);
    assertStatus(403, GET(url("Users.list", "office.id", other.getOffice().id)));
  }

  /**
   * Il client REST riceve 404 per un'email sconosciuta anche dalle missioni, come da days e
   * competences, e 403 per una persona di un'altra sede.
   */
  @Test
  public void restMissionsAreScoped() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    other.setEmail("other" + other.id + "@example.org");
    other.save();
    String client = roleOn(colleague, Role.REST_CLIENT);
    commit();

    login(client);
    assertStatus(404, GET("/rest/persons/nessuno" + UUID.randomUUID()
        + "@example.org/missions?start=2026-09-01&end=2026-09-30"));
    assertStatus(403, GET("/rest/persons/" + other.getEmail()
        + "/missions?start=2026-09-01&end=2026-09-30"));
  }

  private MealTicketCard card(Person person) {
    MealTicketCard card = new MealTicketCard();
    card.setNumber("card-" + UUID.randomUUID());
    card.setPerson(person);
    card.setDeliveryOffice(person.getOffice());
    card.save();
    return card;
  }

  private Group group(Person person) {
    Group group = new Group();
    group.setName("gruppo-" + UUID.randomUUID());
    group.setOffice(person.getOffice());
    group.setManager(person);
    group.save();
    return group;
  }

  private Affiliation affiliation(Person person) {
    Affiliation affiliation = new Affiliation();
    affiliation.setGroup(group(person));
    affiliation.setPerson(person);
    affiliation.setBeginDate(java.time.LocalDate.of(2026, 1, 1));
    affiliation.save();
    return affiliation;
  }

  private PersonChildren child(Person person) {
    PersonChildren child = new PersonChildren();
    child.setPerson(person);
    child.setName("Figlio");
    child.setSurname("Di " + person.getSurname());
    child.setBornDate(new LocalDate(2020, 1, 1));
    child.save();
    return child;
  }

  private ShiftType shiftType(Person person) {
    ShiftCategories category = new ShiftCategories();
    category.setDescription("servizio-" + UUID.randomUUID());
    category.setOffice(person.getOffice());
    category.setSupervisor(person);
    category.save();
    ShiftType shiftType = new ShiftType();
    shiftType.setType("turno-" + UUID.randomUUID());
    shiftType.setDescription("Turno");
    shiftType.setShiftCategories(category);
    shiftType.save();
    return shiftType;
  }

  private String roleOn(Person person, String roleName) {
    return h2Examples.userWithOfficeRole(person.getOffice(), roleName).getUsername();
  }

  private <T> T reload(Class<T> entityClass, Long id) {
    JPA.em().clear();
    return JPA.em().find(entityClass, id);
  }

  private String url(String action, String param, Object value) {
    Map<String, Object> args = new HashMap<>();
    args.put(param, value);
    return Router.reverse(action, args).url;
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    clearCookies();
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
