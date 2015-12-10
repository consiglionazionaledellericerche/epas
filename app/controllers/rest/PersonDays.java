package controllers.rest;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;

import cnr.sync.dto.PersonDayDTO;
import cnr.sync.dto.PersonMonthDTO;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Security;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import it.cnr.iit.epas.DateUtility;
import models.Absence;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.Stamping;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

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
   * (date)
   */

  @BasicAuth
  public static void getDaySituation(String email, LocalDate date) {
    Person person = personDao.byEmail(email).orNull();

    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person);

    PersonDay pd = personDayDao.getPersonDay(person, date).orNull();
    if (pd == null) {
      JsonResponse.notFound("Non sono presenti informazioni per "
              + person.name + " " + person.surname + " nel giorno " + date);
    }
    PersonDayDTO pdDTO = generateDayDTO(pd);
    renderJSON(pdDTO);
  }


  /**
   * metodo rest che ritorna la situazione di una persona relativa al mese e all'anno passati come
   * parametro
   */
  @BasicAuth
  public static void getMonthSituation(String email, int month, int year) {
    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }
    if (Security.getUser().get().person.id != person.id) {
      JsonResponse.badRequest("Le informazioni richieste non sono relative"
              + " alla persona loggata");
    }
    /**
     * TODO: capire perchè mi dà granted all'utilizzo del metodo nonostante
     * la drools (probabilmente scritta male, da capire meglio).
     * Adesso viene bypassato col controllo sopra...però è veramente orrendo
     */
    rules.checkIfPermitted(person);
    List<Contract> monthContracts = wrapperFactory
            .create(person).getMonthContracts(year, month);
    PersonMonthDTO pmDTO = new PersonMonthDTO();
    for (Contract contract : monthContracts) {
      Optional<ContractMonthRecap> cmr = wrapperFactory.create(contract)
              .getContractMonthRecap(new YearMonth(year, month));
      if (cmr.isPresent()) {
        pmDTO = generateMonthDTO(cmr.get());
      } else {
        JsonResponse.notFound("Non sono presenti informazioni per "
                + person.name + " " + person.surname + " nel mese di " + DateUtility.fromIntToStringMonth(month));
      }
    }
    renderJSON(pmDTO);

  }


  /**
   * @return il personDayDTO costruito sulla base del personDay passato come parametro da ritornare
   * alle funzioni rest
   */
  private static PersonDayDTO generateDayDTO(PersonDay pd) {
    PersonDayDTO pdDTO = new PersonDayDTO();
    pdDTO.buonopasto = pd.isTicketAvailable;
    pdDTO.differenza = pd.difference;
    pdDTO.progressivo = pd.progressive;
    pdDTO.tempolavoro = pd.timeAtWork;
    if (pd.absences != null && pd.absences.size() > 0) {
      for (Absence abs : pd.absences) {
        pdDTO.codiceassenza.add(abs.absenceType.code);
      }
    }
    if (pd.stampings != null && pd.stampings.size() > 0) {
      for (Stamping s : pd.stampings) {
        pdDTO.timbrature.add(s.date.toString());
      }
    }
    return pdDTO;
  }

  /**
   * @return il personMonthDTO costruito sulla base del COntractMonthRecap opzionale passato come
   * parametro da ritornare alle funzioni rest
   */
  private static PersonMonthDTO generateMonthDTO(ContractMonthRecap cmr) {
    PersonMonthDTO pmDTO = new PersonMonthDTO();
    pmDTO.buoniMensa = cmr.remainingMealTickets;
    pmDTO.possibileUtilizzareResiduoAnnoPrecedente = cmr.possibileUtilizzareResiduoAnnoPrecedente;
    pmDTO.progressivoFinaleMese = cmr.progressivoFinaleMese;
    pmDTO.straordinari = cmr.straordinariMinuti;
    pmDTO.residuoTotaleAnnoCorrente = cmr.remainingMinutesCurrentYear;
    pmDTO.residuoTotaleAnnoPassato = cmr.remainingMinutesLastYear;
    return pmDTO;
  }

}
