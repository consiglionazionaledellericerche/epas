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

package controllers.rest;

import cnr.sync.dto.PersonDayDto;
import cnr.sync.dto.PersonMonthDto;
import com.google.common.base.Optional;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.absences.Absence;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la visualizzazione via REST dei dati delle giornate lavorative.
 */
@With(Resecure.class)
public class PersonDays extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;

  /**
   * metodo rest che ritorna la situazione della persona (passata per email) in un giorno specifico
   * (date).
   */
  @BasicAuth
  public static void getDaySituation(String email, LocalDate date) {
    if (email == null || date == null) {
      notFound();
    }
    Person person = personDao.byEmail(email).orNull();

    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.office);

    PersonDay pd = personDayDao.getPersonDay(person, date).orNull();
    if (pd == null) {
      JsonResponse.notFound("Non sono presenti informazioni per "
              + person.name + " " + person.surname + " nel giorno " + date);
    }
    PersonDayDto pdDto = generateDayDto(pd);
    renderJSON(pdDto);
  }


  /**
   * metodo rest che ritorna la situazione di una persona relativa al mese e all'anno passati come
   * parametro.
   */
  @BasicAuth
  public static void getMonthSituation(String email, int month, int year) {
    if (email == null || month == 0 || year == 0) {
      notFound();
    }
    Person person = personDao.byEmail(email).orNull();
    
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }

    /*
     * TODO: capire perchè mi dà granted all'utilizzo del metodo nonostante
     * la drools (probabilmente scritta male, da capire meglio).
     * Adesso viene bypassato col controllo sopra...però è veramente orrendo
     */
    rules.checkIfPermitted(person);
    List<Contract> monthContracts = wrapperFactory
            .create(person).orderedMonthContracts(year, month);
    PersonMonthDto pmDto = new PersonMonthDto();
    for (Contract contract : monthContracts) {
      Optional<ContractMonthRecap> cmr = wrapperFactory.create(contract)
              .getContractMonthRecap(new YearMonth(year, month));
      if (cmr.isPresent()) {
        pmDto = generateMonthDto(cmr.get());
      } else {
        JsonResponse.notFound(
            "Non sono presenti informazioni per "
            + person.name + " " + person.surname + " nel mese di "
            + DateUtility.fromIntToStringMonth(month));
      }
    }
    renderJSON(pmDto);

  }


  /**
   * Metodo che costruisce il dto sulla base del personDay passato come parametro.
   *
   * @return il personDayDTO costruito sulla base del personDay passato come parametro da ritornare
   *     alle funzioni rest.
   */
  private static PersonDayDto generateDayDto(PersonDay pd) {
    PersonDayDto pdDto = new PersonDayDto();
    pdDto.buonopasto = pd.isTicketAvailable;
    pdDto.differenza = pd.difference;
    pdDto.progressivo = pd.progressive;
    pdDto.tempolavoro = pd.timeAtWork;
    if (pd.absences != null && pd.absences.size() > 0) {
      for (Absence abs : pd.absences) {
        pdDto.codiceassenza.add(abs.absenceType.code);
      }
    }
    if (pd.stampings != null && pd.stampings.size() > 0) {
      for (Stamping s : pd.stampings) {
        pdDto.timbrature.add(s.date.toString());
      }
    }
    return pdDto;
  }

  /**
   * Metodo che genera il dto sulla base del contractMonthRecap passato come parametro.
   *
   * @return il personMonthDTO costruito sulla base del COntractMonthRecap opzionale passato come
   *     parametro da ritornare alle funzioni rest.
   */
  private static PersonMonthDto generateMonthDto(ContractMonthRecap cmr) {
    PersonMonthDto pmDto = new PersonMonthDto();
    pmDto.buoniMensa = cmr.remainingMealTickets;
    pmDto.possibileUtilizzareResiduoAnnoPrecedente = cmr.possibileUtilizzareResiduoAnnoPrecedente;
    pmDto.progressivoFinaleMese = cmr.progressivoFinaleMese;
    pmDto.straordinari = cmr.straordinariMinuti;
    pmDto.residuoTotaleAnnoCorrente = cmr.remainingMinutesCurrentYear;
    pmDto.residuoTotaleAnnoPassato = cmr.remainingMinutesLastYear;
    return pmDto;
  }

}