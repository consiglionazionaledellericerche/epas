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

package absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import java.util.List;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.ServiceFactories;
import models.Person;
import models.PersonConfiguration;
import models.User;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;
import models.absences.definitions.DefaultGroup;
import models.enumerate.AccountRole;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Test sul limite mensile personale dei giorni di lavoro agile (LAGILE/LAGILEBP).
 * Gli ammontari prendibili sono espressi in centesimi di unità (10 giorni = 1000).
 */
@StaticInject
public class AgileWorkLimitTest extends UnitTest {

  public static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  public static final LocalDate MID_FEBRUARY_2026 = new LocalDate(2026, 2, 16);
  public static final LocalDate MID_MARCH_2026 = new LocalDate(2026, 3, 16);
  public static final LocalDate MID_SEPTEMBER_2026 = new LocalDate(2026, 9, 15);
  //Ottobre 2026: 22 giorni lavorativi
  public static final LocalDate MID_OCTOBER_2026 = new LocalDate(2026, 10, 15);
  //Marzo 2027: almeno 22 giorni lavorativi
  public static final LocalDate MID_MARCH_2027 = new LocalDate(2027, 3, 15);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ServiceFactories serviceFactories;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static ConfigurationManager configurationManager;

  /**
   * Senza limite personale si applica il limite standard: 10 giorni, 8 a febbraio.
   */
  @Test
  public void standardLimitWithoutPersonalLimit() {
    absenceService.enumInitializator();
    Person person = employee();

    assertEquals(1000, takableAmount(person, MID_OCTOBER_2026, Lists.newArrayList()));
    assertEquals(800, takableAmount(person, MID_FEBRUARY_2026, Lists.newArrayList()));
  }

  /**
   * Con il limite personale vale solo quello, anche a febbraio e anche se supera il 10.
   */
  @Test
  public void personalLimitReplacesStandardLimit() {
    absenceService.enumInitializator();

    Person person = employee();
    setLimit(person, 6, BEGIN_2026, Optional.absent());
    assertEquals(600, takableAmount(person, MID_OCTOBER_2026, Lists.newArrayList()));

    person = employee();
    setLimit(person, 15, BEGIN_2026, Optional.absent());
    assertEquals(1500, takableAmount(person, MID_OCTOBER_2026, Lists.newArrayList()));
    assertEquals(1500, takableAmount(person, MID_FEBRUARY_2026, Lists.newArrayList()));

    person = employee();
    setLimit(person, 0, BEGIN_2026, Optional.absent());
    assertEquals(0, takableAmount(person, MID_OCTOBER_2026, Lists.newArrayList()));
  }

  /**
   * L'inizializzazione del gruppo nel mese ha la precedenza sul limite personale.
   */
  @Test
  public void initializationWinsOverPersonalLimit() {
    absenceService.enumInitializator();
    Person person = employee();
    setLimit(person, 15, BEGIN_2026, Optional.absent());

    InitializationGroup initialization =
        new InitializationGroup(person, groupLagile(), MID_OCTOBER_2026);
    initialization.setTakableTotal(5);

    assertEquals(500, takableAmount(person, MID_OCTOBER_2026,
        Lists.newArrayList(initialization)));
  }

  /**
   * Periodi consecutivi con valori diversi; fuori dai periodi torna il limite standard.
   */
  @Test
  public void consecutivePeriodsWithDifferentLimits() {
    absenceService.enumInitializator();
    Person person = employee();
    setLimit(person, 6, BEGIN_2026, Optional.of(new LocalDate(2026, 6, 30)));
    setLimit(person, 8, new LocalDate(2026, 7, 1), Optional.of(new LocalDate(2026, 12, 31)));

    assertEquals(600, takableAmount(person, MID_MARCH_2026, Lists.newArrayList()));
    assertEquals(800, takableAmount(person, MID_SEPTEMBER_2026, Lists.newArrayList()));
    // Marzo 2027 è fuori dai periodi: limite standard
    assertEquals(1000, takableAmount(person, MID_MARCH_2027, Lists.newArrayList()));
  }

  /**
   * Il limite personale è modificabile solo da amministratori di sistema e gestori anagrafica.
   */
  @Test
  public void onlyAuthorizedUsersCanEditPersonalLimit() {
    absenceService.enumInitializator();
    Person person = employee();
    PersonConfiguration agileLimit = configuration(person, EpasParam.AGILE_WORK_MONTHLY_LIMIT);
    PersonConfiguration otherParam = configuration(person, EpasParam.TELEWORK);

    User user = new User();
    assertFalse(configurationManager.isEditableBy(agileLimit, user));
    assertTrue(configurationManager.isEditableBy(otherParam, user));

    user.getRoles().add(AccountRole.RO_ADMIN);
    assertFalse(configurationManager.isEditableBy(agileLimit, user));

    for (AccountRole role : Lists.newArrayList(
        AccountRole.DEVELOPER, AccountRole.ADMIN, AccountRole.REGISTRY_MANAGER)) {
      User authorized = new User();
      authorized.getRoles().add(role);
      assertTrue(configurationManager.isEditableBy(agileLimit, authorized));
    }
  }

  /**
   * Dipendente con orario normale e data di attivazione (necessaria per i periodi
   * dei parametri di configurazione) dal primo gennaio 2026.
   */
  private Person employee() {
    Person person = h2Examples.normalEmployee(BEGIN_2026, Optional.absent());
    person.setBeginDate(BEGIN_2026);
    person.save();
    for (PersonConfiguration configuration : person.getPersonConfigurations()) {
      configuration.setBeginDate(BEGIN_2026);
      configuration.save();
    }
    return person;
  }

  private void setLimit(Person person, Integer limit, LocalDate begin, Optional<LocalDate> end) {
    configurationManager.updateInteger(EpasParam.AGILE_WORK_MONTHLY_LIMIT, person, limit,
        Optional.of(begin), end, true);
  }

  private PersonConfiguration configuration(Person person, EpasParam epasParam) {
    return person.getPersonConfigurations().stream()
        .filter(pc -> pc.getEpasParam().equals(epasParam)).findFirst().get();
  }

  private GroupAbsenceType groupLagile() {
    return absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_LAGILE.name()).get();
  }

  private int takableAmount(Person person, LocalDate date,
      List<InitializationGroup> initializations) {
    PeriodChain periodChain = serviceFactories.buildPeriodChainPhase1(person, groupLagile(),
        date,
        Lists.newArrayList(),
        person.getContracts(),
        initializations,
        Lists.newArrayList());
    assertFalse(periodChain.periods.isEmpty());
    return periodChain.periods.get(0).getPeriodTakableAmount();
  }
}
