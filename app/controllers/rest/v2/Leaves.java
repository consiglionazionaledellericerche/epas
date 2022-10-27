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

package controllers.rest.v2;

import cnr.sync.dto.v2.AbsencePeriodDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.AbsenceShowTerseDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.AbsenceDao;
import dao.PersonDao;
import helpers.JodaConverters;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.List;
import javax.inject.Inject;
import lombok.val;
import models.Person;
import models.absences.Absence;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per l'esportazione delle informazioni sulle aspettative.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
public class Leaves extends Controller {
  
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonDao personDao;
  @Inject
  static AbsenceDao absenceDao;

  /**
   * Periodi di tipo aspettativa di una persona in un anno in formato JSON.
   *
   * @param year anno in cui cercare le aspettative
   */
  public static void byPersonAndYear(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number, Integer year, boolean includeDetails) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.getOffice());

    LocalDate start = new LocalDate(LocalDate.now().getYear(), 1, 1);
    LocalDate end = new LocalDate(LocalDate.now().getYear(), 12, 31);
    if (year != null) {
      start = new LocalDate(year, 1, 1);
      end = new LocalDate(year, 12, 31);
    }
    val absences = absenceDao.getAbsencesByDefaultGroup(
        Optional.of(person), start, Optional.of(end), DefaultGroup.G_ASPETTATIVA);
    val absencePeriods = toAbsencesPeriods(absences, includeDetails);

    renderJSON(gsonBuilder.create().toJson(absencePeriods));
  }

  /**
   * Periodi di tipo aspettativa di una sede in un anno in formato JSON. 
   *
   * @param id id in ePAS dell'Ufficio.
   * @param code codice cds dell'ufficio
   * @param codeId sedeId di attestati
   * @param year anno in cui cercare le aspettative
   */
  public static void byOfficeAndYear(Long id, String code, String codeId, Integer year,
      boolean includeDetails) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    LocalDate start = new LocalDate(LocalDate.now().getYear(), 1, 1);
    LocalDate end = new LocalDate(LocalDate.now().getYear(), 12, 31);
    if (year != null) {
      start = new LocalDate(year, 1, 1);
      end = new LocalDate(year, 12, 31);
    }
    val absences = absenceDao.getAbsencesByDefaultGroup(
        Optional.absent(), start, Optional.of(end), DefaultGroup.G_ASPETTATIVA);
    val absencePeriods = toAbsencesPeriods(absences, includeDetails);

    renderJSON(gsonBuilder.create().toJson(absencePeriods));
  }

  /**
   * Attenzione questo metodo funziona solo se le assenze sono passate ordinate per
   * persona e data.
   */ 
  @Util
  private static List<AbsencePeriodDto> toAbsencesPeriods(List<Absence> absences,
      boolean includeDetails) {
    List<AbsencePeriodDto> abs = Lists.newArrayList();
    LocalDate previousDate = null;
    String previousCode = null;
    Person previousPerson = null;
    AbsencePeriodDto currentAbsencePeriod = null;
    
    for (Absence absence : absences) {
      if (previousPerson == null 
          || !previousPerson.id.equals(absence.getPersonDay().getPerson().id)
          || previousCode == null || !previousCode.equals(absence.getAbsenceType().getCode()) 
          || (previousDate == null 
          || previousDate.plusDays(1).compareTo(absence.getPersonDay().getDate()) != 0)) {
        currentAbsencePeriod = 
            new AbsencePeriodDto(
                PersonShowTerseDto.build(absence.getPersonDay().getPerson()),
                absence.getAbsenceType().getCode(), 
                JodaConverters.jodaToJavaLocalDate(absence.getPersonDay().getDate()));
        currentAbsencePeriod.setEnd(currentAbsencePeriod.getStart());
        abs.add(currentAbsencePeriod);
      } else {
        currentAbsencePeriod.setEnd(JodaConverters.jodaToJavaLocalDate(absence.getPersonDay().getDate()));
      }
      if (includeDetails) {
        currentAbsencePeriod.getAbsences().add(AbsenceShowTerseDto.build(absence));
      }
      previousPerson = absence.getPersonDay().getPerson();
      previousCode = absence.getAbsenceType().getCode();
      previousDate = absence.getPersonDay().getDate();
    }
    
    return abs;
  }
}
