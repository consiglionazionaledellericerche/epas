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

package manager;

import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import manager.services.absences.AbsenceService;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Cancellando dal cartellino l'ultima timbratura copiata dal telelavoro deve essere rimosso
 * anche il codice 103RT; se restano altre timbrature il codice rimane.
 */
@StaticInject
public class TeleworkAbsenceCodeRemovalTest extends UnitTest {

  private static final LocalDate DAY = new LocalDate(2026, 10, 14);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static TeleworkStampingManager teleworkStampingManager;

  @Test
  public void lastStampingDeletedRemoves103rt() {
    long personDayId = dayWith103rtAndStampings(1);

    PersonDay pd = personDayDao.getPersonDayById(personDayId);
    teleworkStampingManager.deleteStampingAndTeleworkAbsenceCode(pd, pd.getStampings().get(0));

    assertEquals(0, countStampings(personDayId));
    assertEquals(0, count103rt(personDayId));
  }

  @Test
  public void otherStampingsLeftKeep103rt() {
    long personDayId = dayWith103rtAndStampings(2);

    PersonDay pd = personDayDao.getPersonDayById(personDayId);
    teleworkStampingManager.deleteStampingAndTeleworkAbsenceCode(pd, pd.getStampings().get(0));

    assertEquals(1, countStampings(personDayId));
    assertEquals(1, count103rt(personDayId));
  }

  /**
   * Giorno con il 103RT e il numero di timbrature indicato, salvato e scaricato dalla sessione
   * per riprodurre il caricamento della richiesta di cancellazione.
   */
  private long dayWith103rtAndStampings(int stampings) {
    absenceService.enumInitializator();
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, DAY);

    for (int i = 0; i < stampings; i++) {
      Stamping stamping = new Stamping(pd, new LocalDateTime(2026, 10, 14, 9 + i, 0));
      stamping.setWay(i % 2 == 0 ? WayType.in : WayType.out);
      stamping.setMarkedByTelework(true);
      stamping.save();
    }

    Absence absence = new Absence();
    absence.setPersonDay(pd);
    absence.setAbsenceType(absenceComponentDao.absenceTypeByCode("103RT").get());
    absence.setJustifiedType(
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.nothing));
    absence.save();
    pd.getAbsences().add(absence);
    pd.save();

    JPA.em().flush();
    JPA.em().clear();
    return pd.id;
  }

  private static long countStampings(long personDayId) {
    JPA.em().flush();
    return (Long) JPA.em()
        .createQuery("select count(s) from Stamping s where s.personDay.id = :id")
        .setParameter("id", personDayId).getSingleResult();
  }

  private static long count103rt(long personDayId) {
    JPA.em().flush();
    return (Long) JPA.em()
        .createQuery("select count(a) from Absence a "
            + "where a.personDay.id = :id and a.absenceType.code = '103RT'")
        .setParameter("id", personDayId).getSingleResult();
  }
}
