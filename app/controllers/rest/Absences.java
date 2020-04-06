package controllers.rest;

import cnr.sync.dto.AbsenceAddedRest;
import cnr.sync.dto.AbsenceRest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.AbsenceManager;
import manager.services.absences.AbsenceService;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.absences.Absence;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Absences extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceManager absenceManager;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;

  /**
   * Restituisce un Json con la lista delle assenze corrispondenti ai parametri passati.
   * 
   * @param email email della persona di cui cercare le assenze
   * @param begin data di inizio delle assenze da cercare
   * @param end data di fine della assenze da cercare
   */
  @BasicAuth
  public static void absencesInPeriod(String eppn, String email, Long personPerseoId, 
      LocalDate begin, LocalDate end) {
    Person person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPerseoId).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.office);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    List<AbsenceRest> absences = FluentIterable.from(absenceDao.getAbsencesInPeriod(
        Optional.fromNullable(person), begin, Optional.fromNullable(end), false))
        .transform(new Function<Absence, AbsenceRest>() {
          @Override
          public AbsenceRest apply(Absence absence) {
            AbsenceRest ar = new AbsenceRest();
            ar.absenceCode = absence.absenceType.code;
            ar.description = absence.absenceType.description;
            ar.date = absence.personDay.date.toString();
            ar.name = absence.personDay.person.name;
            ar.surname = absence.personDay.person.surname;
            return ar;
          }
        }).toList();
    renderJSON(absences);
  }

  /**
   * Inserisce un assenza con il codice specificato in tutti i giorni compresi nel periodo indicato
   * (saltando i feriali se l'assenza non è prendibile nei giorni feriali).
   * Restituisce un Json con la lista dei giorni in cui è stata inserita l'assenza ed gli effetti
   * codici inseriti.
   * Il campo eppn se passato viene usato come preferenziale per cercare la persona.
   * 
   * @param eppn eppn della persona di cui inserire l'assenza
   * @param email email della persona di cui inserire l'assenza
   * @param personPerseoId perseoId della persona di cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param hours (opzionale) le ore giustificate in caso di assenza oraria
   * @param minutes (opzionale) i muniti giustificati in caso di assenza oraria
   */
  @BasicAuth
  public static void insertAbsence(
      String eppn, String email, Long personPerseoId, String absenceCode, LocalDate begin, 
      LocalDate end, Integer hours, Integer minutes) {
    Person person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPerseoId).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.office);
    log.debug("Richiesto inserimento assenza via REST -> eppn = {}, email = {}, absenceCode = {}, "
        + "begin = {}, end = {}, hours = {}, minutes = {}", 
        eppn, email, absenceCode, begin, end, hours, minutes);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    List<AbsenceAddedRest> list = Lists.newArrayList();
    try {
      val absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
      if (!absenceType.isPresent()) {
        JsonResponse.badRequest("Tipo assenza non valido");
      }

      val justifiedType = absenceType.get().justifiedTypesPermitted.iterator().next();
      val groupAbsenceType = absenceType.get().defaultTakableGroup(); 
      val report = absenceService.insert(person, groupAbsenceType, begin, end, absenceType.get(),
          justifiedType, hours, minutes, false, absenceManager);

      report.insertTemplateRows.stream().forEach(templateRow -> {
        AbsenceAddedRest aar = new AbsenceAddedRest();
        aar.date = templateRow.absence.getDate().toString();
        aar.absenceCode = templateRow.absence.absenceType.code;
        aar.isOk = templateRow.absenceErrors.isEmpty();
        aar.reason = Joiner.on(", ").join(
            templateRow.absenceErrors.stream()
            .map(ae -> Messages.get(ae.absenceProblem)).collect(Collectors.toList()));
        list.add(aar);
      });

      absenceManager.saveAbsences(report, person, begin, null, justifiedType, groupAbsenceType);

      renderJSON(list);
    } catch (Exception ex) {
      log.warn("Eccezione durante inserimento assenza via REST", ex);
      JsonResponse.badRequest("Errore nei parametri passati al server");
    }


  }

  /**
   * Verifica se è possibile prendere il tipo di assenza passato nel periodo indicato.
   * La persona viene individuata tramite il suo indirizzo email.
   * Il campo eppn se passato viene usato come preferenziale per cercare la persona. 
   * 
   * @param eppn eppn della persona di cui inserire l'assenza
   * @param email email della persona di cui inserire l'assenza
   * @param personPerseoId l'identificativo della persona per cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param hours le eventuali ore d'assenza 
   * @param minutes gli eventuali minuti di assenza
   */
  @BasicAuth
  public static void checkAbsence(String eppn, String email, Long personPerseoId,
      String absenceCode, LocalDate begin, LocalDate end, 
      Integer hours, Integer minutes) 
          throws JsonProcessingException {

    Optional<Person> person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPerseoId);
    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    Optional<Contract> contract = wrapperFactory
        .create(person.get()).getCurrentContract();
    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
        .getContractMonthRecap(new YearMonth(end.getYear(),
            end.getMonthOfYear()));

    if (!recap.isPresent()) {
      JsonResponse.notFound("Non esistono riepiloghi per" + person.get().name + " "
          + person.get().surname + " da cui prender le informazioni per il calcolo");
    } else {
      val absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
      if (!absenceType.isPresent()) {
        JsonResponse.badRequest("Tipo assenza non valido");
      }
      val justifiedType = absenceType.get().justifiedTypesPermitted.iterator().next();
      val groupAbsenceType = absenceType.get().defaultTakableGroup(); 
      val report = absenceService.insert(person.get(), groupAbsenceType, begin, end, 
          absenceType.get(), justifiedType, hours, minutes, false, absenceManager);

      List<AbsenceAddedRest> list = Lists.newArrayList();

      report.insertTemplateRows.stream().forEach(templateRow -> {
        AbsenceAddedRest aar = new AbsenceAddedRest();
        aar.date = templateRow.absence.getDate().toString();
        aar.absenceCode = templateRow.absence.absenceType.code;
        aar.isOk = templateRow.absenceErrors.isEmpty();
        aar.reason = Joiner.on(", ").join(
            templateRow.absenceErrors.stream()
            .map(ae -> Messages.get(ae.absenceProblem)).collect(Collectors.toList()));
        list.add(aar);
      });

      renderJSON(list);
    }
  }

}