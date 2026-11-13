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
import com.google.common.collect.Lists;
import helpers.JodaConverters;
import dao.GroupDao;
import dao.ReperibilityTypeMonthDao;
import dao.ShiftTypeMonthDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ReperibilityTypeMonth;
import models.Role;
import models.ShiftCategories;
import models.ShiftTypeMonth;
import models.UsersRolesOffices;
import models.flows.Affiliation;
import org.joda.time.LocalDate;

/**
 * Gestisce gli effetti del trasferimento di una persona su ciò che la lega alla sede lasciata:
 * servizi di reperibilità e turno, gruppi, ruoli e incarichi, oltre alle segnalazioni su badge,
 * orari e tessera dei buoni pasto da rivedere.
 */
@Slf4j
public class PersonOfficeServicesManager {

  private final ReperibilityTypeMonthDao reperibilityTypeMonthDao;
  private final ShiftTypeMonthDao shiftTypeMonthDao;
  private final GroupDao groupDao;

  @Inject
  PersonOfficeServicesManager(ReperibilityTypeMonthDao reperibilityTypeMonthDao,
      ShiftTypeMonthDao shiftTypeMonthDao, GroupDao groupDao) {
    this.reperibilityTypeMonthDao = reperibilityTypeMonthDao;
    this.shiftTypeMonthDao = shiftTypeMonthDao;
    this.groupDao = groupDao;
  }

  /**
   * Termina al giorno precedente la data indicata la presenza della persona nei servizi di
   * reperibilità e turno della sede e rimuove dai calendari di quei servizi i giorni a partire
   * dalla data indicata.
   *
   * @param person la persona trasferita
   * @param office la sede di provenienza, a cui appartengono i servizi
   * @param date la data di decorrenza del trasferimento
   * @return i messaggi sulle operazioni effettuate e gli avvisi (es. mesi già approvati).
   */
  public List<String> endServicesInOffice(Person person, Office office, LocalDate date) {
    final List<String> messages = Lists.newArrayList();
    endReperibilities(person, office, date, messages);
    endShifts(person, office, date, messages);
    endGroupAffiliations(person, office, date, messages);
    warnings(person, office, date, messages);
    messages.forEach(m -> log.info("{}: {}", person.getFullname(), m));
    return messages;
  }

  /**
   * Rimuove i ruoli della persona sulla sede lasciata (escluso quello di dipendente, allineato
   * a parte) e la toglie dai gestori dei servizi di reperibilità e turno della sede. Gli incarichi
   * di responsabile, obbligatori, vengono solo segnalati. Va chiamato quando il trasferimento
   * diventa effettivo.
   *
   * @return i messaggi sulle operazioni effettuate.
   */
  public List<String> removeRolesInOffice(Person person, Office office) {
    final List<String> messages = Lists.newArrayList();
    if (person.getUser() != null) {
      for (UsersRolesOffices uro : Lists.newArrayList(person.getUser().getUsersRolesOffices())) {
        if (office.equals(uro.getOffice()) && !Role.EMPLOYEE.equals(uro.getRole().getName())) {
          person.getUser().getUsersRolesOffices().remove(uro);
          uro.delete();
          messages.add(String.format("Rimosso il ruolo %s sulla sede %s",
              uro.getRole().getName(), office.getName()));
        }
      }
    }
    for (PersonReperibilityType type : Lists.newArrayList(person.getReperibilities())) {
      if (office.equals(type.getOffice())) {
        type.getManagers().remove(person);
        person.getReperibilities().remove(type);
        type.save();
        messages.add(String.format("Rimosso dai gestori del servizio di reperibilità %s",
            type.getDescription()));
      }
    }
    for (ShiftCategories category : Lists.newArrayList(person.getCategories())) {
      if (office.equals(category.getOffice())) {
        category.getManagers().remove(person);
        person.getCategories().remove(category);
        category.save();
        messages.add(String.format("Rimosso dai gestori del servizio di turno %s",
            category.getDescription()));
      }
    }
    person.getReperibilityTypes().stream().filter(t -> office.equals(t.getOffice()))
        .forEach(t -> messages.add(String.format("È ancora responsabile del servizio di "
            + "reperibilità %s della sede %s: assegnare un nuovo responsabile",
            t.getDescription(), office.getName())));
    person.getShiftCategories().stream().filter(c -> office.equals(c.getOffice()))
        .forEach(c -> messages.add(String.format("È ancora responsabile del servizio di turno "
            + "%s della sede %s: assegnare un nuovo responsabile",
            c.getDescription(), office.getName())));
    groupDao.groupsByOffice(office, Optional.of(person), Optional.absent())
        .forEach(g -> messages.add(String.format("È ancora responsabile del gruppo %s della "
            + "sede %s: assegnare un nuovo responsabile", g.getName(), office.getName())));
    messages.forEach(m -> log.info("{}: {}", person.getFullname(), m));
    return messages;
  }

  private void endGroupAffiliations(Person person, Office office, LocalDate date,
      List<String> messages) {
    final LocalDate lastDay = date.minusDays(1);
    for (Affiliation affiliation : Lists.newArrayList(person.getAffiliations())) {
      if (affiliation.getGroup() == null || !office.equals(affiliation.getGroup().getOffice())) {
        continue;
      }
      final String group = affiliation.getGroup().getName();
      if (!affiliation.getBeginDate().isBefore(JodaConverters.jodaToJavaLocalDate(date))) {
        person.getAffiliations().remove(affiliation);
        affiliation.delete();
        messages.add(String.format("Eliminata l'appartenenza al gruppo %s, che iniziava dopo "
            + "il trasferimento", group));
      } else if (affiliation.getEndDate() == null
          || !affiliation.getEndDate().isBefore(JodaConverters.jodaToJavaLocalDate(date))) {
        affiliation.setEndDate(JodaConverters.jodaToJavaLocalDate(lastDay));
        affiliation.save();
        messages.add(String.format("Terminata al %s l'appartenenza al gruppo %s",
            lastDay.toString("dd/MM/yyyy"), group));
      }
    }
  }

  /**
   * Segnala gli elementi legati alla sede lasciata che la nuova sede deve rivedere: badge sui
   * rilevatori, orari di lavoro e fasce obbligatorie della sede nel contratto, tessera dei buoni
   * pasto elettronici rilasciata dalla sede.
   */
  private void warnings(Person person, Office office, LocalDate date, List<String> messages) {
    person.getBadges().stream()
        .filter(b -> b.getBadgeSystem() != null && office.equals(b.getBadgeSystem().getOffice()))
        .map(b -> b.getCode()).distinct()
        .forEach(code -> messages.add(String.format("Il badge %s è ancora associato al sistema "
            + "badge della sede %s", code, office.getName())));
    for (Contract contract : person.getContracts()) {
      if (contract.calculatedEnd() != null && contract.calculatedEnd().isBefore(date)) {
        continue;
      }
      contract.getContractWorkingTimeType().stream()
          .filter(cwtt -> (cwtt.getEndDate() == null || !cwtt.getEndDate().isBefore(date))
              && office.equals(cwtt.getWorkingTimeType().getOffice()))
          .forEach(cwtt -> messages.add(String.format("Il contratto usa l'orario di lavoro %s "
              + "definito dalla sede %s: va rivisto dalla nuova sede",
              cwtt.getWorkingTimeType().getDescription(), office.getName())));
      contract.getContractMandatoryTimeSlots().stream()
          .filter(cmts -> (cmts.getEndDate() == null || !cmts.getEndDate().isBefore(date))
              && office.equals(cmts.getTimeSlot().getOffice()))
          .forEach(cmts -> messages.add(String.format("Il contratto usa la fascia obbligatoria "
              + "%s definita dalla sede %s: va rivista dalla nuova sede",
              cmts.getTimeSlot().getLabel(), office.getName())));
    }
    person.getMealTicketCards().stream()
        .filter(card -> card.isActive() && office.equals(card.getDeliveryOffice()))
        .forEach(card -> messages.add(String.format("La tessera dei buoni pasto elettronici %s "
            + "è stata rilasciata dalla sede %s", card.getNumber(), office.getName())));
  }

  private void endReperibilities(Person person, Office office, LocalDate date,
      List<String> messages) {
    final LocalDate lastDay = date.minusDays(1);
    for (PersonReperibility pr : Lists.newArrayList(person.getReperibility())) {
      if (pr.getPersonReperibilityType() == null
          || !office.equals(pr.getPersonReperibilityType().getOffice())) {
        continue;
      }
      final String service = pr.getPersonReperibilityType().getDescription();

      final List<PersonReperibilityDay> days = pr.getPersonReperibilityDays() == null
          ? Lists.newArrayList()
          : pr.getPersonReperibilityDays().stream()
              .filter(d -> !d.getDate().isBefore(date)).collect(Collectors.toList());
      for (PersonReperibilityDay day : days) {
        final Optional<ReperibilityTypeMonth> month = reperibilityTypeMonthDao
            .byReperibilityTypeAndDate(pr.getPersonReperibilityType(), day.getDate());
        if (month.isPresent() && month.get().isApproved()) {
          messages.add(String.format("Rimossa la reperibilità del %s nel servizio %s: il mese "
              + "era già approvato, le competenze vanno riapprovate",
              day.getDate().toString("dd/MM/yyyy"), service));
        }
        pr.getPersonReperibilityDays().remove(day);
        day.delete();
      }
      if (!days.isEmpty()) {
        messages.add(String.format("Rimossi %d giorni di reperibilità dal %s nel servizio %s",
            days.size(), date.toString("dd/MM/yyyy"), service));
      }

      if (pr.getStartDate() != null && !pr.getStartDate().isBefore(date)) {
        person.getReperibility().remove(pr);
        pr.delete();
        messages.add(String.format("Eliminata l'assegnazione al servizio di reperibilità %s, "
            + "che iniziava dopo il trasferimento", service));
      } else if (pr.getEndDate() == null || !pr.getEndDate().isBefore(date)) {
        pr.setEndDate(lastDay);
        pr.save();
        messages.add(String.format("Terminata al %s l'assegnazione al servizio di reperibilità %s",
            lastDay.toString("dd/MM/yyyy"), service));
      }
    }
  }

  private void endShifts(Person person, Office office, LocalDate date, List<String> messages) {
    final LocalDate lastDay = date.minusDays(1);
    for (PersonShift personShift : person.getPersonShifts()) {
      final List<PersonShiftDay> days = personShift.getPersonShiftDays().stream()
          .filter(d -> !d.getDate().isBefore(date) && d.getShiftType() != null
              && office.equals(d.getShiftType().getShiftCategories().getOffice()))
          .collect(Collectors.toList());
      for (PersonShiftDay day : days) {
        final Optional<ShiftTypeMonth> month =
            shiftTypeMonthDao.byShiftTypeAndDate(day.getShiftType(), day.getDate());
        if (month.isPresent() && month.get().isApproved()) {
          messages.add(String.format("Rimosso il turno del %s nell'attività %s: il mese era già "
              + "approvato, le competenze vanno riapprovate",
              day.getDate().toString("dd/MM/yyyy"), day.getShiftType().getDescription()));
        }
        personShift.getPersonShiftDays().remove(day);
        day.delete();
      }
      if (!days.isEmpty()) {
        messages.add(String.format("Rimossi %d giorni di turno dal %s",
            days.size(), date.toString("dd/MM/yyyy")));
      }

      if (personShift.getPersonShiftShiftTypes() == null) {
        continue;
      }
      for (PersonShiftShiftType psst : Lists.newArrayList(personShift.getPersonShiftShiftTypes())) {
        if (psst.getShiftType() == null
            || !office.equals(psst.getShiftType().getShiftCategories().getOffice())) {
          continue;
        }
        final String activity = psst.getShiftType().getDescription();
        if (psst.getBeginDate() != null && !psst.getBeginDate().isBefore(date)) {
          personShift.getPersonShiftShiftTypes().remove(psst);
          psst.delete();
          messages.add(String.format("Eliminata l'assegnazione all'attività di turno %s, che "
              + "iniziava dopo il trasferimento", activity));
        } else if (psst.getEndDate() == null || !psst.getEndDate().isBefore(date)) {
          psst.setEndDate(lastDay);
          psst.save();
          messages.add(String.format("Terminata al %s l'assegnazione all'attività di turno %s",
              lastDay.toString("dd/MM/yyyy"), activity));
        }
      }
    }
  }
}
