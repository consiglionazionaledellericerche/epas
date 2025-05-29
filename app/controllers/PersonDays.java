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
import com.google.common.base.Strings;
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
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import models.PersonDay;
import models.Stamping;
import models.ZoneToZones;
import models.absences.Absence;
import models.enumerate.MealTicketBehaviour;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la visualizzazione dello storico dei PersonDay.
 *
 * @author Marco Andreini
 */
@Slf4j
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

    rules.checkIfPermitted(personDay.getPerson().getOffice());
    
    Integer hours = 0;
    Integer minutes = 0;
    if (personDay.getApprovedOnHoliday() > 0) {
      hours = personDay.getApprovedOnHoliday() / 60;
      minutes = personDay.getApprovedOnHoliday() % 60;
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
    
    rules.checkIfPermitted(personDay.getPerson().getOffice());
    
    Integer approvedMinutes = (hours * 60) + minutes;
    if (approvedMinutes < 0 || approvedMinutes > personDay.getOnHoliday()) {
      Validation.addError("hours", "Valore non consentito.");
      Validation.addError("minutes", "Valore non consentito.");
      response.status = 400;
      render("@workingHoliday", personDay, hours, minutes);
    }
     
    personDay.setApprovedOnHoliday(approvedMinutes);
    
    consistencyManager.updatePersonSituation(personDay.getPerson().id, personDay.getDate());
    
    flash.success("Ore festive approvate correttamente.");
    Stampings.personStamping(personDay.getPerson().id, personDay.getDate().getYear(), 
        personDay.getDate().getMonthOfYear());
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

    rules.checkIfPermitted(personDay.getPerson().getOffice());
    
    Integer hours = 0;
    Integer minutes = 0;
    if (personDay.getApprovedOutOpening() > 0) {
      hours = personDay.getApprovedOutOpening() / 60;
      minutes = personDay.getApprovedOutOpening() % 60;
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
    
    rules.checkIfPermitted(personDay.getPerson().getOffice());
    
    Integer approvedMinutes = (hours * 60) + minutes;
    if (approvedMinutes < 0 || approvedMinutes > personDay.getOutOpening()) {
      Validation.addError("hours", "Valore non consentito.");
      Validation.addError("minutes", "Valore non consentito.");
      response.status = 400;
      render("@workingOutOpening", personDay, hours, minutes);
    }
     
    personDay.setApprovedOutOpening(approvedMinutes);
    
    consistencyManager.updatePersonSituation(personDay.getPerson().id, personDay.getDate());
    
    flash.success("Ore approvate correttamente.");
    Stampings.personStamping(personDay.getPerson().id, personDay.getDate().getYear(), 
        personDay.getDate().getMonthOfYear());
  }

  /**
   * Inserimento di note associate ad un personDay.
   */
  public static void note(Long personDayId, boolean confirmed, String note) {
    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.getPerson().getOffice());

    if (!confirmed) {
      confirmed = true;
      note = personDay.getNote();
      render(personDay, confirmed, note);
    }

    personDay.setNote(Strings.emptyToNull(note));
    personDay.save();
    log.info("Note impostate su person day {}", personDay);
    flash.success("Note impostate correttamente.");

    Stampings.personStamping(personDay.getPerson().id, personDay.getDate().getYear(),
        personDay.getDate().getMonthOfYear());
  }

  /**
   * Forza la decisione sul buono pasto di un giorno specifico per un dipendente.
   */
  public static void forceMealTicket(Long personDayId, boolean confirmed,
      MealTicketDecision mealTicketDecision) {

    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.getPerson().getOffice());

    if (!confirmed) {
      confirmed = true;

      mealTicketDecision = MealTicketDecision.COMPUTED;

      if (personDay.isTicketForcedByAdmin()) {
        if (personDay.isTicketAvailable()) {
          mealTicketDecision = MealTicketDecision.FORCED_TRUE;
        } else {
          mealTicketDecision = MealTicketDecision.FORCED_FALSE;
        }
      }

      render(personDay, confirmed, mealTicketDecision);
    }

    if (mealTicketDecision.equals(MealTicketDecision.COMPUTED)) {
      personDay.setTicketForcedByAdmin(false);
    } else {
      personDay.setTicketForcedByAdmin(true);
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_FALSE)) {
        personDay.setTicketAvailable(MealTicketBehaviour.notAllowMealTicket);
      }
      if (mealTicketDecision.equals(MealTicketDecision.FORCED_TRUE)) {
        personDay.setTicketAvailable(MealTicketBehaviour.allowMealTicket);
      }
    }

    personDay.save();
    consistencyManager.updatePersonSituation(personDay.getPerson().id, personDay.getDate());

    flash.success("Buono Pasto impostato correttamente.");

    Stampings.personStamping(personDay.getPerson().id, personDay.getDate().getYear(),
        personDay.getDate().getMonthOfYear());

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
    
    //Lista delle revisioni del personday
    List<HistoryValue<PersonDay>> historyPersonDayList = 
        personDayHistoryDao.personDayHistory(personDayId);
    
    boolean zoneDefined = false;
    List<ZoneToZones> link = personDay.getPerson().getBadges().stream()
        .<ZoneToZones>flatMap(b -> b.getBadgeReader().getZones().stream()
            .map(z -> z.getZoneLinkedAsMaster().stream().findAny().orElse(null)))       
        .collect(Collectors.toList());
    if (!link.isEmpty()) {
      zoneDefined = true;
    }
    render(historyStampingsList, historyAbsencesList, personDay, found, zoneDefined, 
        historyPersonDayList);
  }

  
  /**
   * Mostra la form per impostare di ignore il calcolo del permesso breve su un giorno.
   */
  public static void ignoreShortLeave(Long personDayId, boolean confirmed, 
      boolean ignoreShortLeave) {
    PersonDay personDay = personDayDao.getPersonDayById(personDayId);
    Preconditions.checkNotNull(personDay);
    Preconditions.checkNotNull(personDay.isPersistent());

    rules.checkIfPermitted(personDay.getPerson().getOffice());

    if (!confirmed) {
      confirmed = true;
      ignoreShortLeave = personDay.isIgnoreShortLeave();
      render(personDay, confirmed, ignoreShortLeave);
    }

    personDay.setIgnoreShortLeave(ignoreShortLeave);
    personDay.save();
    log.info("ignoreShortLeave impostata a {} su person day {}", ignoreShortLeave, personDay);
    flash.success("Politica di impostazione permesso breve salvata correttamente.");

    Stampings.personStamping(personDay.getPerson().id, personDay.getDate().getYear(),
        personDay.getDate().getMonthOfYear());
  }
  
}
