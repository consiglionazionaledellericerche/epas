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

package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.ContractDao;
import dao.GroupDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import manager.NotificationManager;
import models.Contract;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftShiftType;
import models.flows.Group;
import models.Person;
import models.PersonShift;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Job per il fix delle affiliazioni ai gruppi e dei turni di reperibilità per i contratti chiusi.
 *
 * @author loredana
 */
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 1 21 * * ?") // Ore 21:01
public class FixAffiliationGroupAndReperibility extends Job<Void> {

  @Inject
  static PersonShiftDayDao personShiftDayDao;

  @Inject
  static ShiftDao shiftDao;

  @Inject
  static ContractDao contractDao;

  @Inject
  static GroupDao groupDao;

  @Inject
  static PersonReperibilityDayDao personReperibilityDayDao;

  @Inject
  static NotificationManager notificationManager;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    log.info("Start job FixAffiliationGroupAndReperibility");

    LocalDate begin = LocalDate.now().minusDays(7);
    LocalDate end = LocalDate.now();

    List<Contract> expriredContracts = contractDao.getExpiredContractsInPeriod(begin, end,
        Optional.absent());

    Map<Group, Set<Contract>> groupsManagers = new HashMap<>();
    Map<PersonShiftShiftType, Set<Contract>> shiftSupervisors = new HashMap<>();
    Map<PersonReperibilityType, Set<Contract>> reperibilitySupervisors = new HashMap<>();

    for (Contract expiredContract : expriredContracts) {
      LocalDate endContractDate = expiredContract.calculatedEnd();
      Person person = expiredContract.person;
      log.info("Persona {} endDate contratto scaduto {}", person.fullName(), endContractDate);

      val groupsToNotify = disableGroupAffiliation(person, endContractDate);
      groupsToNotify.forEach(group -> {
            Set<Contract> contractsEnd = groupsManagers.get(group);
            if (contractsEnd == null) {
              contractsEnd = new HashSet<>();
              contractsEnd.add(expiredContract);
              groupsManagers.put(group, contractsEnd);
            } else {
              contractsEnd.add(expiredContract);
            }
          }
      );
      val supervisorshiftToNotify = disableShift(person, endContractDate);
      supervisorshiftToNotify.forEach(supervisor -> {
            Set<Contract> contractsEnd = shiftSupervisors.get(supervisor);
            if (contractsEnd == null) {
              contractsEnd = new HashSet<>();
              contractsEnd.add(expiredContract);
              shiftSupervisors.put(supervisor, contractsEnd);
            } else {
              contractsEnd.add(expiredContract);
            }
          }
      );

      val supervisorReperibilityToNotify = disableReperibility(person, endContractDate);
      supervisorReperibilityToNotify.forEach(supervisor -> {
            Set<Contract> contractsEnd = reperibilitySupervisors.get(supervisor);
            if (contractsEnd == null) {
              contractsEnd = new HashSet<>();
              contractsEnd.add(expiredContract);
              reperibilitySupervisors.put(supervisor, contractsEnd);
            } else {
              contractsEnd.add(expiredContract);
            }
          }
      );

    }

    sendNotification(groupsManagers, shiftSupervisors, reperibilitySupervisors);

    log.info("End job FixAffiliationGroupAndReperibility");
  }

  /**
   * Disabilita la reperibilità di una data persona con contratto scaduto impostando come data di
   * termine turno la data di fine contratto.
   *
   * @param person          persona con contratto scaduto da rimuovere dai turni.
   * @param endContractDate data di fine contratto.
   * @return la lista dei supervisori delle reperibilità che sono state disabilitate.
   */
  private Set<PersonReperibilityType> disableReperibility(Person person,
      LocalDate endContractDate) {

    Set<PersonReperibilityType> reperibilityTypes = Sets.newHashSet();
    Set<PersonReperibility> reperibilitiesToUpdate = Sets.newHashSet();

    List<PersonReperibilityDay> reperibilities = personReperibilityDayDao.getPersonReperibilityDayByPerson(
        person, endContractDate);

    for (PersonReperibilityDay prd : reperibilities) {

      val pr = prd.personReperibility;

      if (prd.date.isAfter(
          endContractDate)) {
        reperibilityTypes.add(prd.reperibilityType);
        reperibilitiesToUpdate.add(pr);

        long cancelled =
            personReperibilityDayDao.deletePersonReperibilityDay(prd.reperibilityType, prd.date);
        if (cancelled == 1) {
          log.info("Rimossa reperibilità di tipo {} del giorno {} di {}",
              prd.reperibilityType, prd.date, person.fullName());
        }
      }
    }

    for (PersonReperibility pr : reperibilitiesToUpdate) {
      if (pr.endDate == null || pr.endDate.isAfter(endContractDate)) {
        pr.endDate = endContractDate;
        pr.save();
        log.info(
            "Aggiornata situazione date di reperibilità {}  start date {} end date {}",
            person.fullName(), pr.startDate, pr.endDate);
      }
    }

    return reperibilityTypes;
  }

  /**
   * Disabilita il turno per una data persona con contratto scaduto impostando come data di termine
   * turno la data di fine contratto.
   *
   * @param person          persona con contratto scaduto da rimuovere dai turni.
   * @param endContractDate data di fine contratto.
   * @return la lista dei turni che sono stati disattivati.
   */
  private Set<PersonShiftShiftType> disableShift(Person person, LocalDate endContractDate) {

    Set<PersonShiftShiftType> shiftToDeactivated = Sets.newHashSet();
    PersonShift personShift = personShiftDayDao.getPersonShiftByPerson(person, endContractDate);

    if (personShift == null) {
      return shiftToDeactivated;
    }
    val personShiftShiftType = shiftDao.getByPersonShiftAndDate(personShift, endContractDate);

    var removeShift = false;

    for (PersonShiftShiftType psst : personShiftShiftType) {
      if (psst.endDate == null || psst.endDate.isAfter(endContractDate)) {
        removeShift = true;
        shiftToDeactivated.add(psst);
        psst.endDate = endContractDate;
        psst.save();
        log.info(
            "Aggiornata situazione date di abilitazione ai turni di {}  start date {} end date {}",
            person.fullName(), psst.beginDate, psst.endDate);
      }
    }

    if (removeShift) {
      personShift.disabled = true;
      personShift.save();
    }

    return shiftToDeactivated;
  }

  /**
   * Rimuove una data persona con contratto scaduto da un gruppo impostando come data di uscita dal
   * gruppo la data di fine contratto.
   *
   * @param person          persona con contratto scaduto da rimuovere dai turni.
   * @param endContractDate data di fine contratto.
   * @return la lista dei gruppi da cui è stata rimossa la persona con contratto scaduto.
   */
  private Set<Group> disableGroupAffiliation(Person person, LocalDate endContractDate) {

    Set<Group> groupsToDeactivated = Sets.newHashSet();

    val fromDate = java.time.LocalDate.of(endContractDate.getYear(),
        endContractDate.getMonthOfYear(),
        endContractDate.getDayOfMonth());
    val groups = groupDao.myGroups(person, fromDate);

    groups.forEach(group ->
        {
          group.affiliations.stream()
              .filter(a -> a.isActive()).forEach(a -> {
                groupsToDeactivated.add(a.getGroup());
                java.time.LocalDate endDate = java.time.LocalDate.of(endContractDate.getYear(),
                    endContractDate.getMonthOfYear(), endContractDate.getDayOfMonth());
                a.setEndDate(endDate);
                a.save();
                log.info("Disabilita associazione di {} al gruppo {}",
                    a.getPerson().getFullname(), a.getGroup().getName());
              });
        }
    );

    return groupsToDeactivated;
  }


  /**
   * Invia la notifica dell'eliminazione delle persone con contratto scaduto dai gruppi di turno,
   * reperibilità e affiliazione.
   *
   * @param groupsManagers          manager dei gruppi con le persone con i contratti scaduti da
   *                                notificare.
   * @param shiftSupervisors        supervisor dei turni con le persone con i contratti scaduti da
   *                                notificare.
   * @param reperibilitySupervisors supervisor delle reperibilità con le persone con i contratti
   *                                scaduti da notificar.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public void sendNotification(Map<Group, Set<Contract>> groupsManagers,
      Map<PersonShiftShiftType, Set<Contract>> shiftSupervisors,
      Map<PersonReperibilityType, Set<Contract>> reperibilitySupervisors) {

    if (!groupsManagers.isEmpty()) {
      groupsManagers.forEach(
          (group, contracts) -> notificationManager.notificationAffiliationRemoved(contracts,
              group));
    }

    if (!shiftSupervisors.isEmpty()) {
      shiftSupervisors.forEach(
          (supervisor, contracts) -> notificationManager.notificationShiftRemoved(contracts,
              supervisor));
    }
    if (!reperibilitySupervisors.isEmpty()) {
      reperibilitySupervisors.forEach(
          (supervisor, contracts) -> notificationManager.notificationReperibilityRemoved(contracts,
              supervisor));
    }
  }

}

