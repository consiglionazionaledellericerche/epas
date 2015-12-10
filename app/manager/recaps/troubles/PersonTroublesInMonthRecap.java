package manager.recaps.troubles;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.PersonDayInTroubleDao;
import manager.PersonManager;
import models.Person;
import models.PersonDayInTrouble;
import models.enumerate.Troubles;

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

  public int holidayWorkingTimeNotAccepted = 0;
  public int holidayWorkingTimeAccepted = 0;

  /**
   * @param person
   * @param monthBegin
   * @param monthEnd
   */
  public PersonTroublesInMonthRecap(PersonDayInTroubleDao personDayInTroubleDao,
                                    PersonManager personManager, Person person, LocalDate monthBegin, LocalDate monthEnd) {

    this.person = person;
    List<PersonDayInTrouble> troubles = personDayInTroubleDao
            .getPersonDayInTroubleInPeriod(person, monthBegin, monthEnd);


    for (PersonDayInTrouble trouble : troubles) {

      if (trouble.cause.equals(Troubles.UNCOUPLED_FIXED)) {
        this.troublesAutoFixedL.add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause.equals(Troubles.NO_ABS_NO_STAMP)) {
        this.troublesNoAbsenceNoStampingsL
                .add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause.equals(Troubles.UNCOUPLED_WORKING)) {
        this.troublesNoAbsenceUncoupledStampingsNotHolidayL
                .add(trouble.personDay.date.getDayOfMonth());
      }

      if (trouble.cause.equals(Troubles.UNCOUPLED_HOLIDAY)) {
        this.troublesNoAbsenceUncoupledStampingsHolidayL
                .add(trouble.personDay.date.getDayOfMonth());
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
