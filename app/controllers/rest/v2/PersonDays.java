package controllers.rest.v2;

import cnr.sync.dto.v2.AbsenceDto;
import cnr.sync.dto.v2.PersonDayDto;
import cnr.sync.dto.v2.StampingDto;
import com.google.common.base.Optional;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.absences.Absence;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
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
   * Metodo rest che ritorna la situazione della persona (passata per email o eppn) in un giorno specifico
   * (date).
   * Nel caso venga passato sia eppn che email la precedenza nella ricerca della persona va al campo eppn.
   */
  @BasicAuth
  public static void getDaySituation(String email, String eppn, LocalDate date) {
    log.debug("getDaySituation -> email={}, eppn={}, date={}", email, date);
    if ((email == null && eppn == null) || date == null) {
      notFound();
    }
    Optional<Person> person = personDao.byEppnOrEmail(eppn, email);

    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
              + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);

    PersonDay pd = personDayDao.getPersonDay(person.get(), date).orNull();
    if (pd == null) {
      JsonResponse.notFound("Non sono presenti informazioni per "
              + person.get().name + " " + person.get().surname + " nel giorno " + date);
    }
    PersonDayDto pdDTO = generateDayDTO(pd);
    renderJSON(pdDTO);
  }

  /**
   * @return il personDayDTO costruito sulla base del personDay passato come 
   *    parametro da ritornare alle funzioni rest.
   */
  private static PersonDayDto generateDayDTO(PersonDay pd) {
    PersonDayDto pdDto = 
        PersonDayDto.builder()
          .buonoPasto(pd.isTicketAvailable)
          .differenza(pd.difference)
          .progressivo(pd.progressive)
          .tempoLavoro(pd.timeAtWork)
          .build();
    if (pd.absences != null && pd.absences.size() > 0) {
      for (Absence abs : pd.absences) {
        pdDto.getCodiciAssenza().add(AbsenceDto.build(abs));
      }
    }
    if (pd.stampings != null && pd.stampings.size() > 0) {
      for (Stamping s : pd.stampings) {
        pdDto.getTimbrature().add(StampingDto.build(s));
      }
    }
    return pdDto;
  }

}
