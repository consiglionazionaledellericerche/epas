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
import dao.ReperibilityTypeMonthDao;
import dao.ShiftTypeMonthDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ReperibilityTypeMonth;
import models.ShiftTypeMonth;
import org.joda.time.LocalDate;

/**
 * Chiude la presenza di una persona nei servizi di reperibilità e turno di una sede quando la
 * persona viene trasferita in un'altra sede.
 */
@Slf4j
public class PersonOfficeServicesManager {

  private final ReperibilityTypeMonthDao reperibilityTypeMonthDao;
  private final ShiftTypeMonthDao shiftTypeMonthDao;

  @Inject
  PersonOfficeServicesManager(ReperibilityTypeMonthDao reperibilityTypeMonthDao,
      ShiftTypeMonthDao shiftTypeMonthDao) {
    this.reperibilityTypeMonthDao = reperibilityTypeMonthDao;
    this.shiftTypeMonthDao = shiftTypeMonthDao;
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
    messages.forEach(m -> log.info("{}: {}", person.getFullname(), m));
    return messages;
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
