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

package controllers.rest.v3;

import cnr.sync.dto.v3.VacationSituationDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import models.Contract;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Rappresenta i dati relativi al tempo disponibile come straordinario 
 * o recupero.
 */
@With(Resecure.class)
@Slf4j
public class Vacations extends Controller {

  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Informazioni sulla situazione delle ferie di una persona ad una certa data.
   */
  public static void byPersonAndYear(Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, String number, Integer year) {

    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);

    log.debug("Chiamata Vacations::byPersonAndYear, person={}, year = {}", 
        person.getFullname(), year);

    if (year == null) {
      year = LocalDate.now().getYear();
    }

    rules.checkIfPermitted(person.getOffice(new LocalDate(year, 1, 1)).get());

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    List<VacationSituation> vacationSituations = Lists.newArrayList();

    for (Contract contract : wrPerson.orderedYearContracts(year)) {
      VacationSituation vacationSituation = 
          absenceService.buildVacationSituation(
              contract, year, vacationGroup, Optional.absent(), false);
      vacationSituations.add(vacationSituation);
    }

    renderJSON(gsonBuilder.create().toJson(
        vacationSituations.stream().map(VacationSituationDto::build).collect(Collectors.toList())));
  }
}
