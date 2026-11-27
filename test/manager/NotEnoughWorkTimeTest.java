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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.wrapper.IWrapperPersonDay;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonalWorkingTime;
import models.Qualification;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Segnalazione del tempo a lavoro insufficiente (#729): per i livelli I-III serve almeno un'ora
 * di tempo da timbrature, salvo presenza automatica e assenze che giustificano tempo.
 */
@StaticInject
public class NotEnoughWorkTimeTest extends UnitTest {

  // Mercoledì
  private static final LocalDate DAY = new LocalDate(2026, 10, 21);
  private static final int WORKING_TIME = 432;

  @Inject
  private static PersonDayManager personDayManager;

  @Test
  public void topLevelWithOneMinuteAndNothingAbsenceIsInTrouble() {
    PersonDay pd = personDay(2, 1);
    absence(pd, JustifiedTypeName.nothing);
    assertTrue(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithoutStampingsAndNothingAbsenceIsInTrouble() {
    PersonDay pd = personDay(3, 0);
    absence(pd, JustifiedTypeName.nothing);
    assertTrue(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithShortStampingsOnlyIsInTrouble() {
    PersonDay pd = personDay(1, 59);
    assertTrue(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithOneHourOfStampingsIsNotInTrouble() {
    PersonDay pd = personDay(2, PersonDayManager.MIN_STAMPINGS_TIME_FOR_TOP_LEVELS);
    absence(pd, JustifiedTypeName.nothing);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithHourlyAbsenceIsNotInTrouble() {
    PersonDay pd = personDay(2, 0);
    absence(pd, JustifiedTypeName.nothing);
    absence(pd, JustifiedTypeName.specified_minutes);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithCompleteDayMissionIsNotInTrouble() {
    PersonDay pd = personDay(2, 0);
    absence(pd, JustifiedTypeName.complete_day_and_add_overtime);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void topLevelWithAllDayAbsenceIsNotInTrouble() {
    PersonDay pd = personDay(2, 0);
    absence(pd, JustifiedTypeName.all_day);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), true));
  }

  @Test
  public void topLevelWithFixedTimeAtWorkIsNotInTrouble() {
    PersonDay pd = personDay(2, 0);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, true), false));
  }

  @Test
  public void topLevelOnHolidayIsNotInTrouble() {
    PersonDay pd = personDay(2, 0);
    pd.setHoliday(true);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
  }

  @Test
  public void lowerLevelKeepsHalfWorkingTimeRule() {
    PersonDay pd = personDay(5, 120);
    pd.setTimeAtWork(WORKING_TIME / 2 - 1);
    assertTrue(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
    pd.setTimeAtWork(WORKING_TIME / 2);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), false));
    pd.setTimeAtWork(0);
    assertFalse(personDayManager.isNotEnoughWorkTime(wrapper(pd, false), true));
  }

  private PersonDay personDay(int level, int stampingsTime) {
    Qualification qualification = new Qualification();
    qualification.setQualification(level);
    Person person = new Person();
    person.setQualification(qualification);
    PersonDay pd = new PersonDay(person, DAY);
    pd.setStampingsTime(stampingsTime);
    pd.setTimeAtWork(stampingsTime);
    return pd;
  }

  private void absence(PersonDay pd, JustifiedTypeName name) {
    JustifiedType justifiedType = new JustifiedType();
    justifiedType.setName(name);
    Absence absence = new Absence();
    absence.setJustifiedType(justifiedType);
    pd.getAbsences().add(absence);
  }

  private IWrapperPersonDay wrapper(PersonDay pd, boolean fixed) {
    WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
    wttd.setWorkingTime(WORKING_TIME);
    return new IWrapperPersonDay() {
      @Override
      public PersonDay getValue() {
        return pd;
      }

      @Override
      public boolean isFixedTimeAtWork() {
        return fixed;
      }

      @Override
      public Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay() {
        return Optional.of(wttd);
      }

      @Override
      public Optional<Contract> getPersonDayContract() {
        return Optional.absent();
      }

      @Override
      public Optional<PersonalWorkingTime> getPersonalWorkingTime() {
        return Optional.absent();
      }

      @Override
      public Optional<PersonDay> getPreviousForNightStamp() {
        return Optional.absent();
      }

      @Override
      public void setPreviousForNightStamp(Optional<PersonDay> potentialOnlyPrevious) {
      }

      @Override
      public Optional<PersonDay> getPreviousForProgressive() {
        return Optional.absent();
      }

      @Override
      public void setPreviousForProgressive(Optional<PersonDay> potentialOnlyPrevious) {
      }

      @Override
      public Stamping getLastStamping() {
        return null;
      }
    };
  }
}
