package controllers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.history.StampingHistoryDao;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ConsistencyManager;

import models.Person;
import models.PersonDay;
import models.Stamping;
import models.absences.Absence;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

/**
 * Controller per la visualizzazione dello storico dei PersonDay.
 *
 * @author marco
 */
@With({Resecure.class})
public class PersonDays extends Controller {

  @Inject
  static PersonDayHistoryDao personDayHistoryDao;
  @Inject
  static StampingHistoryDao stampingHistoryDao;
  @Inject
  static AbsenceHistoryDao absenceHistoryDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;

  //  /**
  //   * La presenza festiva nell'anno.
  //   *
  //   * @param year anno
  //   */
  //  public static void holidaySituation(int year) {
  //
  //    List<Person> simplePersonList = personDao.list(
  //        Optional.<String>absent(),
  //        secureManager.officesReadAllowed(Security.getUser().get()),
  //        false, new LocalDate(year, 1, 1),
  //        new LocalDate(year, 12, 31), false).list();
  //
  //    List<IWrapperPerson> personList = FluentIterable
  //        .from(simplePersonList)
  //        .transform(wrapperFunctionFactory.person()).toList();
  //    render(personList, year);
  //  }
  //
  //  /**
  //   * La presenza festiva della persona nell'anno.
  //   *
  //   * @param personId persona
  //   * @param year     anno
  //   */
  //  public static void personHolidaySituation(Long personId, int year) {
  //
  //    Person per = personDao.getPersonById(personId);
  //    Preconditions.checkNotNull(per);
  //
  //    rules.checkIfPermitted(per.office);
  //
  //    IWrapperPerson person = wrapperFactory.create(per);
  //
  //    render(person, year);
  //  }


  /**
   * Abilita / disabilita l'orario festivo.
   *
   * @param personDayId giorno
   */
  public static void toggleWorkingHoliday(Long personDayId) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.person.office);

    //    pd.acceptedHolidayWorkingTime = !pd.acceptedHolidayWorkingTime;
    //    if (!pd.acceptedHolidayWorkingTime) {
    //      pd.isTicketForcedByAdmin = false;
    //    }
    //    pd.save();

    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    Person person = personDay.person;

    render(person, personDay);
  }

  /**
   * Forza la decisione sul buono pasto di un giorno specifico per un dipendente.
   */
  public static void forceMealTicket(Long personDayId, boolean confirmed,
      MealTicketDecision mealTicketDecision) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.person.office);

    if (!confirmed) {
      confirmed = true;

      mealTicketDecision = MealTicketDecision.COMPUTED;

      if (personDay.isTicketForcedByAdmin) {
        if (personDay.isTicketAvailable) {
          mealTicketDecision = MealTicketDecision.FORCED_TRUE;
        } else {
          mealTicketDecision = MealTicketDecision.FORCED_FALSE;
        }
      }

      render(personDay, confirmed, mealTicketDecision);
    }

    if (mealTicketDecision.equals(MealTicketDecision.COMPUTED)) {
      personDay.isTicketForcedByAdmin = false;
    } else {
      personDay.isTicketForcedByAdmin = true;
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_FALSE)) {
        personDay.isTicketAvailable = false;
      }
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_TRUE)) {
        personDay.isTicketAvailable = true;
      }
    }

    personDay.save();
    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

    flash.success("Buono Pasto impostato correttamente.");

    Stampings.personStamping(personDay.person.id, personDay.date.getYear(),
        personDay.date.getMonthOfYear());

  }

  public enum MealTicketDecision {
    COMPUTED, FORCED_TRUE, FORCED_FALSE;
  }
  
  /**
   * Visualizzazione dello storico dei PersonDay.
   *
   * @param personDayId l'id del personDay di cui mostrare lo storico
   */
  public static void personDayHistory(long personDayId) {

    boolean found = false;
    final PersonDay personDay = PersonDay.findById(personDayId);
    if (personDay == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Absence>> allAbsences = personDayHistoryDao
        .absences(personDayId);

    Set<Long> absenceIds = Sets.newHashSet();
    for (HistoryValue<Absence> historyValue : allAbsences) {
      absenceIds.add(historyValue.value.id);
    }

    List<Long> sortedAbsencesIds = Lists.newArrayList(absenceIds);
    Collections.sort(sortedAbsencesIds);

    //Lista di absences
    List<List<HistoryValue<Absence>>> historyAbsencesList = Lists.newArrayList();

    for (Long absenceId : sortedAbsencesIds) {

      List<HistoryValue<Absence>> historyAbsence = absenceHistoryDao
          .absences(absenceId);
      historyAbsencesList.add(historyAbsence);
    }

    List<HistoryValue<Stamping>> allStampings = personDayHistoryDao
        .stampings(personDayId);

    Set<Long> stampingIds = Sets.newHashSet();
    for (HistoryValue<Stamping> historyValue : allStampings) {
      stampingIds.add(historyValue.value.id);
    }

    List<Long> sortedStampingsIds = Lists.newArrayList(stampingIds);
    Collections.sort(sortedStampingsIds);

    //Lista di stampings
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (Long stampingId : sortedStampingsIds) {

      List<HistoryValue<Stamping>> historyStamping = stampingHistoryDao
          .stampings(stampingId);
      historyStampingsList.add(historyStamping);
    }
    render(historyStampingsList, historyAbsencesList, personDay, found);
  }
}
