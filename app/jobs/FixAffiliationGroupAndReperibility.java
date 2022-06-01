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
import com.google.common.collect.Maps;
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
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftShiftType;
import models.flows.Group;
import models.Person;
import models.PersonShift;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job per il fix delle affiliazioni ai gruppi e dei turni di reperibilità per i contratti chiusi.
 *
 * @author loredana
 */
@Slf4j
//@OnApplicationStart(async = true)
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

    LocalDate today = LocalDate.now();

    List<Person> people = Person.findAll();

    Map<Group, Set<Contract>> groups_managers = new HashMap<>();
    Map<PersonShiftShiftType, Set<Contract>> shift_supervisors = new HashMap<>();
    Map<PersonReperibilityType, Set<Contract>> reperibility_supervisors = new HashMap<>();

    for (Person person : people) {

      var contracts = contractDao.getPersonContractList(person);
      Contract last_contract = contracts.size() == 0 ? null : contracts.get(contracts.size() - 1);

      if (last_contract == null) {
        continue;
      }
      val endContractDate = last_contract.calculatedEnd();
      // check se il contratto è scaduto
      if (endContractDate != null && endContractDate.isBefore(today)) {
        //log.info("Persona {} endDate ultimo contratto {}", person.fullName(), endContractDate);

        val groups_to_notify = disableGroupAffiliation(person, endContractDate);
        groups_to_notify.forEach(group -> {
              Set<Contract> contracts_end = groups_managers.get(group);
              if (contracts_end == null) {
                contracts_end = new HashSet<>();
                contracts_end.add(last_contract);
                groups_managers.put(group, contracts_end);
              } else {
                contracts_end.add(last_contract);
              }
            }
        );
        val supervisor_shift_to_notify = disableShift(person, endContractDate);
        supervisor_shift_to_notify.forEach(supervisor -> {
              Set<Contract> contracts_end = shift_supervisors.get(supervisor);
              if (contracts_end == null) {
                contracts_end = new HashSet<>();
                contracts_end.add(last_contract);
                shift_supervisors.put(supervisor, contracts_end);
              } else {
                contracts_end.add(last_contract);
              }
            }
        );

        val supervisor_reperibility_to_notify = disableReperibility(person, endContractDate);
        supervisor_reperibility_to_notify.forEach(supervisor -> {
              Set<Contract> contracts_end = reperibility_supervisors.get(supervisor);
              if (contracts_end == null) {
                contracts_end = new HashSet<>();
                contracts_end.add(last_contract);
                reperibility_supervisors.put(supervisor, contracts_end);
              } else {
                contracts_end.add(last_contract);
              }
            }
        );
      }
    }

    sendNotification(groups_managers, shift_supervisors, reperibility_supervisors);

    log.info("End job FixAffiliationGroupAndReperibility");
  }

  private Set<PersonReperibilityType> disableReperibility(Person person,
      LocalDate endContractDate) {

    Set<PersonReperibilityType> supervisors = Sets.newHashSet();

    List<PersonReperibilityDay> reperibilities = personReperibilityDayDao.getPersonReperibilityDayByPerson(
        person, endContractDate);

    for (PersonReperibilityDay prd : reperibilities) {

      val pr = prd.personReperibility;

      if (pr.endDate == null || pr.endDate.isAfter(endContractDate)) {
        supervisors.add(prd.reperibilityType);
        pr.endDate = endContractDate;
        pr.save();

        long cancelled =
            personReperibilityDayDao.deletePersonReperibilityDay(prd.reperibilityType, prd.date);
        if (cancelled == 1) {
          log.info("Rimossa reperibilità di tipo {} del giorno {} di {}",
              prd.reperibilityType, prd.date, person.fullName());
        }
      }
    }

    return supervisors;
  }


  private Set<PersonShiftShiftType> disableShift(Person person, LocalDate endContractDate) {

    Set<PersonShiftShiftType> shift_to_deactivated = Sets.newHashSet();
    PersonShift personShift = personShiftDayDao.getPersonShiftByPerson(person, endContractDate);

    if (personShift == null) {
      return shift_to_deactivated;
    }
    val personShiftShiftType = shiftDao.getByPersonShiftAndDate(personShift, endContractDate);

    var remove_shift = false;

    for (PersonShiftShiftType psst : personShiftShiftType) {
      if (psst.endDate == null || psst.endDate.isAfter(endContractDate)) {
        remove_shift = true;
        shift_to_deactivated.add(psst);
        psst.endDate = endContractDate;
        psst.save();
        log.info(
            "Aggiornata situazione date di abilitazione ai turni di {}  start date {} end date {}",
            person.fullName(), psst.beginDate, psst.endDate);
      }
    }

    if (remove_shift) {
      personShift.disabled = true;
      personShift.save();
    }

    return shift_to_deactivated;
  }


  private Set<Group> disableGroupAffiliation(Person person, LocalDate endContractDate) {

    Set<Group> groups_to_deactivated = Sets.newHashSet();

    val fromdate = java.time.LocalDate.of(endContractDate.getYear(),
        endContractDate.getMonthOfYear(),
        endContractDate.getDayOfMonth());
    val groups = groupDao.myGroups(person, fromdate);

    groups.forEach(group ->
        {
          group.affiliations.stream()
              .filter(a -> a.isActive()).forEach(a -> {
                groups_to_deactivated.add(a.getGroup());
                java.time.LocalDate endDate = java.time.LocalDate.of(endContractDate.getYear(),
                    endContractDate.getMonthOfYear(), endContractDate.getDayOfMonth());
                a.setEndDate(endDate);
                a.save();
                log.info("Disabilita associazione di {} al gruppo {}",
                    a.getPerson().getFullname(), a.getGroup().getName());
              });
        }
    );

    return groups_to_deactivated;
  }


  /**
   * Approvazione di una richiesta di assenza.
   *
   * @param groups_managers          manager dei gruppi con le persone con i contratti scaduti da
   *                                 notificare.
   * @param shift_supervisors        supervisor dei turni con le persone con i contratti scaduti da
   *                                 notificare.
   * @param reperibility_supervisors supervisor delle reperibilità con le persone con i contratti
   *                                 scaduti da notificar.
   * @return l'eventuale problema riscontrati durante l'approvazione.
   */
  public void sendNotification(Map<Group, Set<Contract>> groups_managers,
      Map<PersonShiftShiftType, Set<Contract>> shift_supervisors,
      Map<PersonReperibilityType, Set<Contract>> reperibility_supervisors) {

    if (!groups_managers.isEmpty()) {
      groups_managers.forEach(
          (group, contracts) -> notificationManager.notificationAffiliationRemoved(contracts,
              group));
    }

    if (!shift_supervisors.isEmpty()) {
      shift_supervisors.forEach(
          (supervisor, contracts) -> notificationManager.notificationShiftRemoved(contracts,
              supervisor));
    }
    if (!reperibility_supervisors.isEmpty()) {
      reperibility_supervisors.forEach(
          (supervisor, contracts) -> notificationManager.notificationReperibilityRemoved(contracts,
              supervisor));
    }

  }

}

