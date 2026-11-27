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

package common.security;

import common.injection.StaticInject;
import controllers.Security;
import db.h2support.H2Examples;
import javax.inject.Inject;
import javax.persistence.EntityTransaction;
import manager.configurations.EpasParam;
import models.Configuration;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http;
import play.mvc.Scope;
import play.test.UnitTest;

/**
 * Gli esiti calcolati con dati modificati e non ancora committati non vanno condivisi con le
 * altre richieste, e una modifica salvata vale subito anche nella richiesta che la fa.
 */
@StaticInject
public class UnsavedPermissionsDataTest extends UnitTest {

  private static final String ACTION = "InformationRequests.enabled";

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static SecurityRules rules;

  private Http.Request runnerRequest;
  private Scope.Session runnerSession;
  private Long userId;
  private Long configurationId;

  /**
   * Un dipendente della sede con i flussi informativi disattivati.
   */
  @Before
  public void employeeWithoutInformationFlows() {
    runnerRequest = Http.Request.current();
    runnerSession = Scope.Session.current();
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(person.getOffice());
    uro.setRole(Role.find("byName", Role.EMPLOYEE).first());
    uro.save();
    Configuration configuration = informationFlows(person);
    configuration.setFieldValue("false");
    configuration.save();
    userId = person.getUser().getId();
    configurationId = configuration.getId();
    commit();
  }

  /**
   * Il runner dei test chiude la transazione corrente: se un test l'ha chiusa, ne riapre una.
   */
  @After
  public void restore() {
    SharedChecksCache.endInvocation();
    Http.Request.current.set(runnerRequest);
    Scope.Session.current.set(runnerSession);
    EntityTransaction tx = JPA.em().getTransaction();
    if (!tx.isActive()) {
      tx.begin();
    }
  }

  @Test
  public void unsavedChangeIsNotShared() {
    User user = newRequest();
    // es. il binding dei parametri su un'entità caricata, senza save()
    informationFlows(user.getPerson()).setFieldValue("true");
    assertTrue(PermissionsDataListener.hasUnsavedChanges());
    assertTrue("la richiesta vede i propri dati in memoria", rules.checkAction(ACTION));

    // fine della richiesta: Play non salva le entità senza save()
    JPA.em().getTransaction().rollback();
    JPA.em().getTransaction().begin();
    newRequest();
    assertFalse("una nuova richiesta non riusa l'esito calcolato con dati non salvati",
        rules.checkAction(ACTION));
  }

  @Test
  public void savedChangeAppliesInTheSameRequest() {
    User user = newRequest();
    assertFalse(PermissionsDataListener.hasUnsavedChanges());
    assertFalse(rules.checkAction(ACTION));

    Configuration configuration = informationFlows(user.getPerson());
    configuration.setFieldValue("true");
    configuration.save();
    assertTrue("dopo il salvataggio l'esito già calcolato nella richiesta non vale più",
        rules.checkAction(ACTION));
  }

  /**
   * Simula l'inizio di una richiesta HTTP dell'utente, con la sessione JPA vuota.
   */
  private User newRequest() {
    JPA.em().clear();
    User user = User.findById(userId);
    Http.Request request = new Http.Request();
    request.args.put(Security.CURRENT_USER, user);
    Http.Request.current.set(request);
    Scope.Session session = new Scope.Session();
    session.put("username", user.getUsername());
    Scope.Session.current.set(session);
    SharedChecksCache.beginInvocation();
    return user;
  }

  private Configuration informationFlows(Person person) {
    return person.getOffice().getConfigurations().stream()
        .filter(c -> c.getEpasParam() == EpasParam.ENABLE_INFORMATION_FLOWS)
        .findFirst().get();
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }
}
