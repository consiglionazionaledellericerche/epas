package controllers.rest.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import cnr.sync.dto.v2.CertificationAbsenceDto;
import cnr.sync.dto.v2.CertificationCompetencesDto;
import cnr.sync.dto.v2.CertificationDto;
import cnr.sync.dto.v2.CertificationMealTicketDto;
import cnr.sync.dto.v2.CertificationTrainingHoursDto;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;
import helpers.JsonResponse;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.show.SeatCertification.PersonCertification;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.PersonCertData;
import manager.attestati.service.PersonMonthlySituationData;
import models.Certification;
import models.Person;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Certifications extends Controller{

  @Inject
  static PersonMonthlySituationData monthData;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;

  /**
   * Metodo rest che permette di ritornare una lista contenente le informazioni mensili
   * del dipendente (assenze, competenze, ore di formazione, buoni pasto).
   * @param email l'indirizzo mail della persona
   * @param eppn il campo eppn della persona
   * @param personPersoId l'identificativo anagrafico della persona
   * @param year l'anno
   * @param month il mese
   */
  @BasicAuth
  public static void getMonthSituation(String email, String eppn, 
      Long personPersoId, int year, int month) {

    log.info("Richieste informazioni mensili da applicazione esterna");
    Optional<Person> person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPersoId);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}", email, eppn, personPersoId);
      
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
          + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);
    
    Map<String, Certification> map = monthData.getCertification(person.get(), year, month);
    CertificationDto dto = generateCertDto(map, year, month);
    renderJSON(dto);

  }

  /**
   * Metodo privato che permette la generazione di un dto contenente informazioni
   * mensili del dipendente.
   * @param map la mappa contenente le informazioni mensili da rielaborare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return il dto contenente le informazioni da inviare al chiamante del servizio rest.
   */   
  private static CertificationDto generateCertDto(Map<String, Certification> map, int year, int month) {
    DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY");
    List<CertificationAbsenceDto> absences = Lists.newArrayList();
    List<CertificationCompetencesDto> competences = Lists.newArrayList();
    List<CertificationMealTicketDto> mealTickets = Lists.newArrayList();
    List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();
    String from;
    String to;
    String[] places;
    for (Map.Entry<String, Certification> entry : map.entrySet()) {
      switch(entry.getValue().certificationType) {
        case ABSENCE:
          places = entry.getValue().content.split(";");
          from = new LocalDate(year, month, Integer.parseInt(places[1])).toString(dtf);
          to = new LocalDate(year, month, Integer.parseInt(places[2])).toString(dtf);
          CertificationAbsenceDto absence = CertificationAbsenceDto.builder()
              .code(places[0])
              .from(from)
              .to(to)
              .build();
          absences.add(absence);
          break;
        case COMPETENCE:
          places = entry.getValue().content.split(";");
          CertificationCompetencesDto competence = CertificationCompetencesDto.builder()
              .code(places[0])
              .quantity(Integer.parseInt(places[1]))
              .build();
          competences.add(competence);
          break;
        case MEAL:
          CertificationMealTicketDto meal = CertificationMealTicketDto.builder()
              .quantity(Integer.parseInt(entry.getValue().content))
              .build();
          mealTickets.add(meal);
          break;
        case FORMATION:
          places = entry.getValue().content.split(";");
          from = new LocalDate(year, month, Integer.parseInt(places[0])).toString(dtf);
          to = new LocalDate(year, month, Integer.parseInt(places[1])).toString(dtf);
          CertificationTrainingHoursDto trainingHour = CertificationTrainingHoursDto.builder()
              .from(from)
              .to(to)
              .quantity(Integer.parseInt(places[2]))
              .build();
          trainingHours.add(trainingHour);
          break;
          default:
            break;
      }
    }
    CertificationDto obj = CertificationDto.builder()
        .absences(absences)
        .competences(competences)
        .tickets(mealTickets)
        .trainingHours(trainingHours)
        .build();
    return obj;
  }
}

