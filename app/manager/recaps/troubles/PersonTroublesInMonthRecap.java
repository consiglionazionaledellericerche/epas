package manager.recaps.troubles;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.PersonDayInTroubleDao;

import manager.PersonManager;

import models.Person;
import models.PersonDayInTrouble;
import models.enumerate.Troubles;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Classe che modella il riepilogo delle timbrature mancanti per la persona tramite una lista di
 * PersonDayInTrouble.
 *
 * @author alessandro
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

  public PersonTroublesInMonthRecap(
      PersonDayInTroubleDao personDayInTroubleDao, PersonManager personManager,
      Person person, LocalDate monthBegin, LocalDate monthEnd) {

    this.person = person;
    List<PersonDayInTrouble> troubles = personDayInTroubleDao.getPersonDayInTroubleInPeriod(person,
        Optional.fromNullable(monthBegin), Optional.fromNullable(monthEnd), Optional.absent());


    for (PersonDayInTrouble trouble : troubles) {

      if (trouble.cause == Troubles.UNCOUPLED_FIXED) {
        troublesAutoFixedL.add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause == Troubles.NO_ABS_NO_STAMP) {
        troublesNoAbsenceNoStampingsL.add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause == Troubles.UNCOUPLED_WORKING) {
        troublesNoAbsenceUncoupledStampingsNotHolidayL.add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause == Troubles.UNCOUPLED_HOLIDAY) {
        troublesNoAbsenceUncoupledStampingsHolidayL.add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause == Troubles.NOT_ENOUGH_WORKTIME) {
        troublesNotEnoughWorkTime.add(trouble.personDay.date.getDayOfMonth());
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
