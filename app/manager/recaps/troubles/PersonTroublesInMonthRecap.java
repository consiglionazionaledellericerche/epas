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

package manager.recaps.troubles;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.PersonDayInTroubleDao;
import java.util.List;
import manager.PersonManager;
import models.Person;
import models.PersonDayInTrouble;
import models.enumerate.Troubles;
import org.joda.time.LocalDate;

/**
 * Classe che modella il riepilogo delle timbrature mancanti per la persona tramite una lista di
 * PersonDayInTrouble.
 *
 * @author Alessandro Martelli
 */
public class PersonTroublesInMonthRecap {

  public Person person;

  public List<Integer> troublesAutoFixedL = Lists.newArrayList();
  public List<Integer> troublesNoAbsenceNoStampingsL = Lists.newArrayList();
  public List<Integer> troublesNoAbsenceUncoupledStampingsNotHolidayL = Lists.newArrayList();
  public List<Integer> troublesNoAbsenceUncoupledStampingsHolidayL = Lists.newArrayList();
  public List<Integer> troublesNotEnoughWorkTime = Lists.newArrayList();

  public int holidayWorkingTimeNotAccepted = 0;
  public int holidayWorkingTimeAccepted = 0;

  /**
   * Costruttore.
   *
   * @param personDayInTroubleDao dao sui persondayInTrouble
   * @param personManager manager coi metodi sulla persona
   * @param person la persona
   * @param monthBegin la data di inizio del mese
   * @param monthEnd la data di fine del mese
   */
  public PersonTroublesInMonthRecap(
      PersonDayInTroubleDao personDayInTroubleDao, PersonManager personManager,
      Person person, LocalDate monthBegin, LocalDate monthEnd) {

    this.person = person;
    List<PersonDayInTrouble> troubles = personDayInTroubleDao.getPersonDayInTroubleInPeriod(person,
        Optional.fromNullable(monthBegin), Optional.fromNullable(monthEnd), Optional.absent());


    for (PersonDayInTrouble trouble : troubles) {

      if (trouble.getCause() == Troubles.UNCOUPLED_FIXED) {
        troublesAutoFixedL.add(trouble.getPersonDay().getDate().getDayOfMonth());
      }

      if (trouble.getCause() == Troubles.NO_ABS_NO_STAMP) {
        troublesNoAbsenceNoStampingsL.add(trouble.getPersonDay().getDate().getDayOfMonth());
      }

      if (trouble.getCause() == Troubles.UNCOUPLED_WORKING) {
        troublesNoAbsenceUncoupledStampingsNotHolidayL.add(trouble.getPersonDay().getDate().getDayOfMonth());
      }

      if (trouble.getCause() == Troubles.UNCOUPLED_HOLIDAY) {
        troublesNoAbsenceUncoupledStampingsHolidayL.add(trouble.getPersonDay().getDate().getDayOfMonth());
      }

      if (trouble.getCause() == Troubles.NOT_ENOUGH_WORKTIME) {
        troublesNotEnoughWorkTime.add(trouble.getPersonDay().getDate().getDayOfMonth());
      }
    }

    this.holidayWorkingTimeNotAccepted = personManager
        .holidayWorkingTimeNotAccepted(person,
            Optional.fromNullable(monthBegin.getYear()),
            Optional.fromNullable(monthBegin.getMonthOfYear()));

    this.holidayWorkingTimeAccepted = personManager
        .holidayWorkingTimeAccepted(person,
            Optional.fromNullable(monthBegin.getYear()),
            Optional.fromNullable(monthBegin.getMonthOfYear()));

  }

}