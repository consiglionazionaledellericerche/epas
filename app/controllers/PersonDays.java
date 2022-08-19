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

package controllers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.history.StampingHistoryDao;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.ConsistencyManager;
import models.PersonDay;
import models.Stamping;
import models.ZoneToZones;
import models.absences.Absence;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la visualizzazione dello storico dei PersonDay.
 *
 * @author Marco Andreini
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

  /**
   * Abilita / disabilita l'orario festivo.
   *
   * @param personDayId giorno
   */
  public static void workingHoliday(Long personDayId) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.person.office);
    
    Integer hours = 0;
    Integer minutes = 0;
    if (personDay.approvedOnHoliday > 0) {
      hours = personDay.approvedOnHoliday / 60;
      minutes = personDay.approvedOnHoliday % 60;
    }

    render(personDay, hours, minutes);
  }
  
  /**
   * Action di approvazione del lavoro festivo.
   */
  public static void approveWorkingHoliday(Long personDayId, Integer hours, Integer minutes) {
    
    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());
    Preconditions.checkNotNull(hours);
    Preconditions.checkNotNull(minutes);
    
    rules.checkIfPermitted(personDay.person.office);
    
    Integer approvedMinutes = (hours * 60) + minutes;
    if (approvedMinutes < 0 || approvedMinutes > personDay.onHoliday) {
      Validation.addError("hours", "Valore non consentito.");
      Validation.addError("minutes", "Valore non consentito.");
      response.status = 400;
      render("@workingHoliday", personDay, hours, minutes);
    }
     
    personDay.setApprovedOnHoliday(approvedMinutes);
    
    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);
    
    flash.success("Ore festive approvate correttamente.");
    Stampings.personStamping(personDay.person.id, personDay.date.getYear(), 
        personDay.date.getMonthOfYear());
  }
  
  /**
   * Abilita / disabilita l'orario lavorato fuori fascia apertura / chiusura.
   *
   * @param personDayId giorno
   */
  public static void workingOutOpening(Long personDayId) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.person.office);
    
    Integer hours = 0;
    Integer minutes = 0;
    if (personDay.approvedOutOpening > 0) {
      hours = personDay.approvedOutOpening / 60;
      minutes = personDay.approvedOutOpening % 60;
    }

    render(personDay, hours, minutes);
  }
  
  /**
   * Action di approvazione del lavoro fuori fascia.
   */
  public static void approveWorkingOutOpening(Long personDayId, Integer hours, Integer minutes) {
    
    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());
    Preconditions.checkNotNull(hours);
    Preconditions.checkNotNull(minutes);
    
    rules.checkIfPermitted(personDay.person.office);
    
    Integer approvedMinutes = (hours * 60) + minutes;
    if (approvedMinutes < 0 || approvedMinutes > personDay.outOpening) {
      Validation.addError("hours", "Valore non consentito.");
      Validation.addError("minutes", "Valore non consentito.");
      response.status = 400;
      render("@workingOutOpening", personDay, hours, minutes);
    }
     
    personDay.setApprovedOutOpening(approvedMinutes);
    
    consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);
    
    flash.success("Ore approvate correttamente.");
    Stampings.personStamping(personDay.person.id, personDay.date.getYear(), 
        personDay.date.getMonthOfYear());
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

  /**
   * Decisione sul ricalcolo dei buoni pasto.
   */
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
    boolean zoneDefined = false;
    List<ZoneToZones> link = personDay.person.badges.stream()
        .<ZoneToZones>flatMap(b -> b.badgeReader.zones.stream()
            .map(z -> z.zoneLinkedAsMaster.stream().findAny().orElse(null)))       
        .collect(Collectors.toList());
    if (!link.isEmpty()) {
      zoneDefined = true;
    }
    render(historyStampingsList, historyAbsencesList, personDay, found, zoneDefined);
  }
}
