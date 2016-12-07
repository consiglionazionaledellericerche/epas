package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import controllers.RequestInit.CurrentData;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import helpers.CacheValues;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.PersonCertData;

import models.Office;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Il controller per l'invio dei dati certificati al nuovo attestati.
 *
 * @author alessandro
 */
@Slf4j
@With(Resecure.class)
public class Certifications extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory factory;
  @Inject
  static PersonDao personDao;
  @Inject
  static ICertificationService certificationService;
  @Inject
  static CacheValues cacheValues;

  /**
   * Pagina principale nuovo invio attestati.
   *
   * @param officeId sede
   * @param year     anno
   * @param month    mese
   */
  public static void certifications(Long officeId, Integer year, Integer month) {

    // Utilizzato per capire quando effettuare l'invio delle informazioni ad attestati
    // Questo perchè se utilizzassimo un controller apposito che si occupa anche di fare la render
    // rimarrebbe l'url nella barra degli indirizzi e un eventuale refresh ne causerebbe il reinvio
    // TODO trovare una soluzione più elegante
    final String commandKey = String.format("id-%s-year-%s-month-%s", officeId, year, month);
    Boolean process = (Boolean) Cache.get(commandKey);
    Cache.safeDelete(commandKey);

    flash.clear();  //non avendo per adesso un meccanismo di redirect pulisco il flash...

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //Mese selezionato
    Optional<YearMonth> monthToUpload;

    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    } else {
      monthToUpload = factory.create(office).nextYearMonthToUpload();
    }

    Verify.verify(monthToUpload.isPresent());

    int validYear = monthToUpload.get().getYear();
    int validMonth = monthToUpload.get().getMonthOfYear();

    // Patch per la navigazione del menù ... ####################################
    // Al primo accesso (da menù) dove non ho mese e anno devo prendere il default
    // (NextMonthToUpload). In quel caso aggiorno la sessione nel cookie. Dovrebbe
    // occuparsene la RequestInit.
    session.put("monthSelected", validMonth);
    session.put("yearSelected", validYear);
    renderArgs.put("currentData", new CurrentData(validYear, validMonth,
        Integer.parseInt(session.get("daySelected")),
        Long.parseLong(session.get("personSelected")),
        office.id));
    // ##########################################################################

    LocalDate monthBegin = new LocalDate(validYear, validMonth, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    Set<Integer> matricoleAttestati = new HashSet<>();

    final Map.Entry<Office, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(office, monthToUpload.get());

    try {
      matricoleAttestati = cacheValues.attestatiSerialNumbers.get(cacheKey);
    } catch (Exception e) {
      flash.error("Errore di connessione al server di Attestati - %s", cleanMessage(e).getMessage());
      log.error("Errore durante la connessione al server di attestati: {}", e.getMessage());
      render(office, validYear, validMonth);
    }

    if (matricoleAttestati.isEmpty()) {
      flash.error("Nessuna matricola presente per il mese %s/%s.\r\n" +
          "Effettuare lo stralcio sul server di Attestati", validMonth, validYear);
      render(office, validYear, validMonth);
    }

    final List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    final Set<Integer> matricoleEpas = people.stream().map(person -> person.number)
        .distinct().collect(Collectors.toSet());

    final Set<Integer> notInEpas = Sets.difference(matricoleAttestati, matricoleEpas);

    final Set<Integer> notInAttestati = Sets.difference(matricoleEpas, matricoleAttestati);

    final Set<Integer> matchNumbers = Sets.newHashSet(matricoleEpas);
    matchNumbers.retainAll(matricoleAttestati);

    render(office, validYear, validMonth, people, notInEpas, notInAttestati, matchNumbers, process);
  }

  /**
   * Metodo scritto per evitare di passare direttamente al controller il boolean per effetuare
   * l'invio degli attestati.
   * In questo nell'url non rimane mai l'indirizzo che effettua l'invio dei dati e se si ricarica
   * la pagina non si corre il rischio di farlo.
   * La soluzione fa abbastanza schifo...trovarne una migliore
   *
   * @param officeId id Ufficio
   * @param year     anno
   * @param month    mese.
   */
  public static void processAll(Long officeId, Integer year, Integer month) {

    final String commandKey = String.format("id-%s-year-%s-month-%s", officeId, year, month);
    Cache.safeAdd(commandKey, Boolean.TRUE, "10s");
    certifications(officeId, year, month);
  }

  /**
   * invalida tutti i parametri in cache legati a quell'ufficio e quel mese per forzarne il
   * ricalcolo con le interrogazioni ad attestati
   *
   * @param officeId id del'ufficio
   * @param year     anno
   * @param month    mese.
   */
  public static void clearCacheValues(Long officeId, Integer year, Integer month) {
    final Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    final YearMonth yearMonth = new YearMonth(year, month);
    final Map.Entry<Office, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(office, yearMonth);

    cacheValues.attestatiSerialNumbers.invalidate(cacheKey);
    cacheValues.elaborationStep.invalidate(cacheKey);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    personDao.list(Optional.absent(), Sets.newHashSet(Lists.newArrayList(office)),
        false, monthBegin, monthEnd, true).list().forEach(person -> {
      final Map.Entry<Person, YearMonth> personKey = new AbstractMap
          .SimpleEntry<>(person, yearMonth);
      cacheValues.personStatus.invalidate(personKey);
    });
    log.info("Svuotati tutti i valori dalla cache: ufficio {} - mese {}/{}", office, month, year);
    certifications(officeId, year, month);
  }

  /**
   * @param personId id della persona
   * @param year     anno
   * @param month    mese
   */
  public static void personStatus(Long personId, int year, int month) {

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person);

    PersonCertData personCertData = null;
    try {
      // Costruisco lo status generale
      final Map.Entry<Person, YearMonth> cacheKey = new AbstractMap
          .SimpleEntry<>(person, new YearMonth(year, month));
      personCertData = cacheValues.personStatus.get(cacheKey);
    } catch (Exception e) {
      log.error("Errore nel recupero delle informazioni dal server di attestati per la persona {}: {}",
          person, cleanMessage(e).getMessage());
      render();
    }

    // La percentuale di completamento della progress bar rispetto al totale da elaborare
    double stepSize;
    try {
      final Map.Entry<Office, YearMonth> key = new AbstractMap
          .SimpleEntry<>(person.office, new YearMonth(year, month));
      stepSize = cacheValues.elaborationStep.get(key);
    } catch (Exception e) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}: {}",
          person, cleanMessage(e).getMessage());
      return;
    }

    render(personCertData, stepSize, person);
  }

  public static void codici() {
    try {
      renderText(certificationService.absenceCodes());
    } catch (Exception e) {
      renderText("Impossibile recuperare i codici dal server di attestati\r\n" +
          cleanMessage(e).getMessage());
    }
  }

  /**
   * Effettua l'invio dei dati ad attestati e l'elaborazione di una persona
   *
   * @param personId id della persona
   * @param year     anno
   * @param month    mese.
   */
  public static void process(Long personId, int year, int month, boolean redirect) {

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person);

    final Map.Entry<Person, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(person, new YearMonth(year, month));

    PersonCertData previousCertData = null;
    try {
      // Costruisco lo status generale
      previousCertData = cacheValues.personStatus.get(cacheKey);
    } catch (Exception e) {
      log.error("Errore nel recupero delle informazioni dal server di attestati" +
          " per la persona {}: {}", person, cleanMessage(e).getMessage());
      render();
    }

    PersonCertData personCertData = null;
    if (!previousCertData.validate) {
      // Se l'attestato non è stato validato applico il process
      try {
        personCertData = certificationService.process(previousCertData);
      } catch (ExecutionException | NoSuchFieldException e) {
        log.error("Errore nell'invio delle informazioni al server di attestati " +
            "per la persona {}: {}", person, cleanMessage(e).getMessage());
      }
    }
//     Se riesco nell'invio ne aggiorno lo stato in cache
    if (personCertData != null) {
      cacheValues.personStatus.put(cacheKey, personCertData);
    } else {
      // Altrimenti invalido il valore presente
      cacheValues.personStatus.invalidate(cacheKey);
    }

    // La percentuale di completamento della progress bar rispetto al totale da elaborare
    double stepSize;
    try {
      final Map.Entry<Office, YearMonth> key = new AbstractMap
          .SimpleEntry<>(person.office, new YearMonth(year, month));
      stepSize = cacheValues.elaborationStep.get(key);
    } catch (Exception e) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}: {}",
          person, cleanMessage(e).getMessage());
      return;
    }

    // permette di chiamare questo controller anche in maniera sincrona per il reinvio delle
    // informazioni per una sola persona tramite link (button sulla singola persona)
    if (redirect) {
      certifications(person.office.id, year, month);
    }

    render("@personStatus", personCertData, stepSize, person);
  }

  // TODO metodo da rifattorizzare (se può avere un'utilità) o da eliminare
//  public static void emptyCertifications(Long officeId, int year, int month) throws ExecutionException {
//
//    flash.clear();  //non avendo per adesso un meccanismo di redirect pulisco il flash...
//
//    Office office = officeDao.getOfficeById(officeId);
//    notFoundIfNull(office);
//    rules.checkIfPermitted(office);
//
//    LocalDate monthBegin = new LocalDate(year, month, 1);
//    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
//
//    //Il mese selezionato è abilitato?
//    boolean autenticate = certificationService.authentication(office, true);
//    if (!autenticate) {
//      flash.error("L'utente app.epas non è abilitato alla sede selezionata");
//      renderTemplate("@certifications", office, year, month);
//    }
//
//    //Lo stralcio è stato effettuato?
//    Set<Integer> matricoleAttestati = new HashSet<>();
//    //Lo stralcio è stato effettuato?
//    final Map.Entry<Office, YearMonth> key = new AbstractMap
//        .SimpleEntry<>(office, new YearMonth(year, month));
//    try {
//      matricoleAttestati = cacheValues.attestatiSerialNumbers.get(key);
//    } catch (Exception e) {
//      flash.error("Errore di connessione ad Attestati - %s", e.getMessage());
//      render("@certifications", office, year, month);
//    }
//
//    List<Person> people = personDao.list(Optional.<String>absent(),
//        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
//
//    List<PersonCertData> peopleCertificationStatus = Lists.newArrayList();
//    boolean peopleNotInAttestati = false;
//
//    for (Person person : people) {
//
//      // Costruisco lo status generale
//      PersonCertData personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month);
//
//      // Elimino ogni record
//      certificationService.emptyAttestati(personCertificationStatus);
//
//      // Ricostruzione nuovo stato (coi record eliminati)
//      personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month);
//
////      if (personCertificationStatus.match()) {
////        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
////        numbers.remove(person.number);
////      }
//
//      peopleCertificationStatus.add(personCertificationStatus);
//    }
//
//    flash.success("Elaborazione completata");
//
//    renderTemplate("@certifications", office, year, month, numbers, peopleNotInAttestati,
//        peopleCertificationStatus);
//
//  }

  /**
   * I codici assenza in attestati.
   */
  public static void certificationsAbsenceCodes() throws ExecutionException {

    // Mappa dei codici di assenza in attestati
    Map<String, CodiceAssenza> attestatiAbsenceCodes = certificationService.absenceCodes();
    if (attestatiAbsenceCodes.isEmpty()) {
      flash.error("L'utente app.epas non è in grado di ottenere le informazioni richieste.");
    }

    // Mappa dei codici assenza in epas e loro utilizzo
    Map<String, List<Absence>> epasAbsences = Maps.newHashMap();
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    List<Absence> absences = Absence.findAll();
    for (AbsenceType absenceType : absenceTypes) {
      log.info(absenceType.code.trim().toUpperCase());
      epasAbsences.put(absenceType.code.trim().toUpperCase(), Lists.newArrayList());
    }
    for (Absence absence : absences) {
      epasAbsences.get(absence.absenceType.code.trim().toUpperCase()).add(absence);
    }

    // Mappa dei codici epas
    Map<String, AbsenceType> epasAbsenceTypes = Maps.newHashMap();
    for (AbsenceType absenceType : absenceTypes) {
      epasAbsenceTypes.put(absenceType.code.trim().toUpperCase(), absenceType);
    }

    render(attestatiAbsenceCodes, epasAbsences, epasAbsenceTypes);
  }

  /**
   * @param e eccezione
   * @return L'ultimo elemento Throwable di una concatenazione di eccezioni
   */
  private static Throwable cleanMessage(Exception e) {
    // Recupera il messaggio pulito dalla gerarchia delle eccezioni
    Throwable throwable;
    if (e.getCause() != null) {
      throwable = e.getCause();
    } else {
      return e;
    }
    while (throwable.getCause() != null) {
      throwable = throwable.getCause();
    }
    return throwable;
  }

}
