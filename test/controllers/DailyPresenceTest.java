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

import com.google.common.collect.Lists;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import manager.PersonDayManager;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.ContractType;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Presenza giornaliera della sede: contenuto della pagina e numero di query, che non deve
 * crescere con il numero di persone presenti.
 */
@StaticInject
public class DailyPresenceTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static PersonDayManager personDayManager;

  /**
   * La pagina elenca le persone con le timbrature del giorno, comprese quelle ancora in servizio.
   */
  @Test
  public void listsPeopleWithTodayStampings() {
    Person first = h2Examples.activeNormalEmployee(BEGIN_2026);
    Office office = first.getOffice();
    List<Person> people = Lists.newArrayList(first);
    people.add(h2Examples.activeNormalEmployee(office, BEGIN_2026));
    people.add(h2Examples.activeNormalEmployee(office, BEGIN_2026));
    inService(people.get(0));
    inService(people.get(1));
    inAndOut(people.get(2));
    User admin = h2Examples.userWithOfficeRole(office, Role.PERSONNEL_ADMIN);
    commit();

    login(admin.getUsername());
    Response response = GET(dailyPresenceUrl(office));
    assertIsOk(response);
    for (Person person : people) {
      assertContentMatch(person.getFullname(), response);
    }
  }

  /**
   * Il numero di query della pagina non dipende dal numero di persone presenti.
   */
  @Test
  public void queriesDoNotGrowWithPeople() {
    long three = queriesForOfficeWith(3);
    long nine = queriesForOfficeWith(9);
    // Prima delle letture in blocco erano circa 10 query in più per ogni persona (53 e 116).
    assertTrue("query con 3 persone: " + three + ", con 9 persone: " + nine,
        nine - three <= 6);
  }

  private long queriesForOfficeWith(int count) {
    Person first = h2Examples.activeNormalEmployee(BEGIN_2026);
    Office office = first.getOffice();
    inService(first);
    for (int i = 1; i < count; i++) {
      Person person = h2Examples.activeNormalEmployee(office, BEGIN_2026);
      if (i % 2 == 0) {
        inService(person);
      } else {
        inAndOut(person);
      }
    }
    User admin = h2Examples.userWithOfficeRole(office, Role.PERSONNEL_ADMIN);
    commit();

    login(admin.getUsername());
    String url = dailyPresenceUrl(office);
    assertIsOk(GET(url));
    Statistics statistics = ((Session) JPA.em().getDelegate()).getSessionFactory()
        .getStatistics();
    statistics.clear();
    assertIsOk(GET(url));
    return statistics.getPrepareStatementCount();
  }

  private void inService(Person person) {
    structured(person);
    PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person,
        LocalDate.now());
    stamping(personDay, new LocalTime(0, 1), WayType.in);
    personDay.save();
  }

  private void inAndOut(Person person) {
    structured(person);
    PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person,
        LocalDate.now());
    stamping(personDay, new LocalTime(0, 1), WayType.in);
    stamping(personDay, new LocalTime(0, 2), WayType.out);
    personDay.save();
  }

  /**
   * La presenza giornaliera mostra solo il personale strutturato.
   */
  private void structured(Person person) {
    for (Contract contract : person.getContracts()) {
      contract.setContractType(ContractType.structured_public_administration);
      contract.save();
    }
  }

  private void stamping(PersonDay personDay, LocalTime time, WayType way) {
    Stamping stamping = new Stamping(personDay,
        new LocalDateTime(personDay.getDate().toDateTime(time)));
    stamping.setWay(way);
    stamping.save();
  }

  private String dailyPresenceUrl(Office office) {
    LocalDate today = LocalDate.now();
    Map<String, Object> args = new HashMap<>();
    args.put("officeId", office.id);
    args.put("year", today.getYear());
    args.put("month", today.getMonthOfYear());
    args.put("day", today.getDayOfMonth());
    return Router.reverse("Stampings.dailyPresence", args).url;
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
