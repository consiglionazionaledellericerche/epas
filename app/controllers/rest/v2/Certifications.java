package controllers.rest.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import cnr.sync.dto.v2.CertificationDto;
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

    rules.checkIfPermitted(person);

    Map<String, Certification> map = monthData.getCertification(person.get(), year, month);
    List<CertificationDto> list = Lists.newArrayList();
    for (Map.Entry<String, Certification> entry : map.entrySet()) {
      CertificationDto cert = generateCertDto(entry.getValue());
      list.add(cert);
    }
    log.info("Inviate informazioni mensili di {} per l'anno {} e il mese {}", person.get(), year, month);
    renderJSON(list);
  }

  /**
   * Metodo privato che permette la generazione di un dto contenente informazioni
   * mensili del dipendente.
   * @param certification l'oggetto contenente le informazioni mensili
   * @return il dto contenente le informazioni da inviare al chiamante del servizio rest.
   */
  private static CertificationDto generateCertDto(Certification certification) {
    CertificationDto dto =
        CertificationDto.builder()
        .type(certification.certificationType.name())
        .content(certification.content)
        .build();
    return dto;
  }
}

