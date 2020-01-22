package controllers.rest;

import cnr.sync.dto.AbsenceAddedRest;
import cnr.sync.dto.AbsenceRest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
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
import groovy.util.logging.Log;
import helpers.JsonResponse;
import helpers.rest.JacksonModule;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.cache.AbsenceTypeManager;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.absences.AbsenceService;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.db.jpa.Blob;
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
  private static AbsenceTypeManager absenceTypeManager;
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
  public static void absencesInPeriod(String eppn, String email, LocalDate begin, LocalDate end) {
    Person person = personDao.byEppnOrEmail(eppn, email).orNull();
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
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param justifiedMinutes (opzionale) i minuti giustificati in caso di assenza oraria
   */
  @BasicAuth
  public static void insertAbsence(
      String eppn, String email, String absenceCode, LocalDate begin, LocalDate end, 
      Integer hours, Integer minutes) {
    Person person = personDao.byEppnOrEmail(eppn, email).orNull();
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
            templateRow.absenceErrors.stream().map(ae -> Messages.get(ae.absenceProblem)).collect(Collectors.toList()));
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
   * 
   * Il campo eppn se passato viene usato come preferenziale per cercare la persona. 
   * 
   * @param eppn eppn della persona di cui inserire l'assenza
   * @param email email della persona di cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param justifiedMinutes (opzionale) i minuti giustificati in caso di assenza oraria
   */
  @BasicAuth
  public static void checkAbsence(String eppn, String email, String absenceCode, 
      LocalDate begin, LocalDate end, Integer hours, Integer minutes) 
      throws JsonProcessingException {
    
    Optional<Person> person = personDao.byEppnOrEmail(eppn, email);
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
      val report = absenceService.insert(person.get(), groupAbsenceType, begin, end, absenceType.get(),
              justifiedType, hours, minutes, false, absenceManager);

      List<AbsenceAddedRest> list = Lists.newArrayList();

      report.insertTemplateRows.stream().forEach(templateRow -> {
        AbsenceAddedRest aar = new AbsenceAddedRest();
        aar.date = templateRow.absence.getDate().toString();
        aar.absenceCode = templateRow.absence.absenceType.code;
        aar.isOk = templateRow.absenceErrors.isEmpty();
        aar.reason = Joiner.on(", ").join(
            templateRow.absenceErrors.stream().map(ae -> Messages.get(ae.absenceProblem)).collect(Collectors.toList()));
        list.add(aar);
      });
      
      renderJSON(list);
    }
  }
  
  /**
   * Verifica se è possibile prendere il tipo di assenza passato nel periodo indicato.
   * La persona viene individuata tramite il perseoId.
   *
   * @param personPerseoId perseoId della persona di cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param justifiedMinutes (opzionale) i minuti giustificati in caso di assenza oraria
   * 
   */
  @BasicAuth
  public static void checkAbsenceByPerseoId(Long personPerseoId, String absenceCode, 
      LocalDate begin, LocalDate end, Integer justifiedMinutes) throws JsonProcessingException {
    
    //TODO: questo metodo dovrebbe richiamare il precedente metodo checkAbsence o viceversa.
    
    //Bad request
    if (personPerseoId == null) {
      JsonResponse.badRequest("personPerseoId null");
    }
    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    if (absenceCode == null) {
      JsonResponse.badRequest("absenceCode null");
    }
    
    //Not found
    Person person = personDao.getPersonByPerseoId(personPerseoId);
    if (person == null) {
      JsonResponse.notFound("Persona con perseoId " + personPerseoId + " non trovata.");
    }
    
    rules.checkIfPermitted(person.office);
    
    Optional<AbsenceType> absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
    if (!absenceType.isPresent()) {
      JsonResponse.notFound("Codice Assenza non trovato.");
    }
    
    Optional<Contract> contract = wrapperFactory
            .create(person).getCurrentContract();
    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
            .getContractMonthRecap(new YearMonth(end.getYear(),
                    end.getMonthOfYear()));

    if (!recap.isPresent()) {
      JsonResponse.notFound("Impossibile completare la richiesta" + person.name + " "
              + person.surname + " da cui prender le informazioni per il calcolo");
    }

    AbsenceInsertReport air = absenceManager.insertAbsenceSimulation(
            person, begin, Optional.fromNullable(end), absenceType.get(),
            Optional.<Blob>absent(), Optional.<String>absent(), Optional.fromNullable(justifiedMinutes));

    renderJSON(mapper.writer(JacksonModule.filterProviderFor(SimpleBeanPropertyFilter
            .serializeAllExcept("absenceAdded"))).writeValueAsString(air.getAbsences()));

  }

  /**
   * TODO: o tutte o nessuna ?...
   * 
   * @param personPerseoId il perseoId della persona
   * @param absenceCode l'absenceCode da inserire
   * @param begin la data di inizio dell'assenza
   * @param end la data di fine dell'assenza
   * @param justifiedMinutes (opzionale) i minuti giustificati in caso di assenza oraria
   * 
   */
  @BasicAuth
  public static void insertAbsenceByPerseoId(Long personPerseoId, String absenceCode, 
      LocalDate begin, LocalDate end, Integer justifiedMinutes) throws JsonProcessingException {
    
    //Bad request
    if (personPerseoId == null) {
      JsonResponse.badRequest("personPerseoId null");
    }
    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    if (absenceCode == null) {
      JsonResponse.badRequest("absenceCode null");
    }
    
    //Not found
    Person person = personDao.getPersonByPerseoId(personPerseoId);
    if (person == null) {
      JsonResponse.notFound("Persona con perseoId " + personPerseoId + " non trovata.");
    }
    
    rules.checkIfPermitted(person.office);
    
    Optional<AbsenceType> absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
    if (!absenceType.isPresent()) {
      JsonResponse.notFound("Codice Assenza non trovato.");
    }
    
    Optional<Contract> contract = wrapperFactory
            .create(person).getCurrentContract();
    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
            .getContractMonthRecap(new YearMonth(end.getYear(),
                    end.getMonthOfYear()));

    if (!recap.isPresent()) {
      JsonResponse.notFound("Impossibile completare la richiesta" + person.name + " "
              + person.surname + " da cui prender le informazioni per il calcolo");
    }

    try {
      AbsenceInsertReport air = absenceManager
          .insertAbsenceRecompute(person, begin, Optional.fromNullable(end), 
              Optional.<LocalDate>absent(), absenceType.get(), Optional.<Blob>absent(), 
              Optional.<String>absent(), Optional.fromNullable(justifiedMinutes));

      List<AbsenceAddedRest> absencesAdded = Lists.newArrayList();
      for (AbsencesResponse absenceResponse : air.getAbsences()) {
        AbsenceAddedRest absenceAddedRest = new AbsenceAddedRest();
        absenceAddedRest.absenceCode = absenceResponse.getAbsenceCode();
        absenceAddedRest.date = absenceResponse.getDate().toString();
        absenceAddedRest.isOk = absenceResponse.isInsertSucceeded();
        absenceAddedRest.reason = absenceResponse.getWarning();
        absencesAdded.add(absenceAddedRest);
      }
      renderJSON(absencesAdded);
      renderJSON(mapper.writer(JacksonModule.filterProviderFor(SimpleBeanPropertyFilter
          .serializeAllExcept("absenceAdded"))).writeValueAsString(air.getAbsences()));
      
    } catch (Exception ex) {
      JsonResponse.badRequest("Errore nei parametri passati al server");
    }

  }

}
