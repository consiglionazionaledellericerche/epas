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
import manager.attestati.service.CertificationService;
import manager.attestati.service.PersonCertificationStatus;

import models.Office;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

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
@With({Resecure.class})
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
  static CertificationService certificationService;

  /**
   * Pagina principale nuovo invio attestati.
   *
   * @param officeId sede
   * @param year     anno
   * @param month    mese
   */
  public static void certifications(Long officeId, Integer year, Integer month) {

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
    session.put("monthSelected", monthToUpload.get().getMonthOfYear());
    session.put("yearSelected", monthToUpload.get().getYear());
    renderArgs.put("currentData", new CurrentData(monthToUpload.get().getYear(),
        monthToUpload.get().getMonthOfYear(),
        Integer.parseInt(session.get("daySelected")),
        Long.parseLong(session.get("personSelected")),
        office.id));
    // ##########################################################################

    LocalDate monthBegin = new LocalDate(monthToUpload.get().getYear(),
        monthToUpload.get().getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    Set<Integer> matricoleAttestati = new HashSet<>();

    final Map.Entry<Office, YearMonth> key = new AbstractMap
        .SimpleEntry<>(office, monthToUpload.get());

    try {
      matricoleAttestati = CacheValues.AttestatiSerialNumbers.get(key);
    } catch (Exception e) {
      flash.error("Errore di connessione al server di Attestati - %s", cleanMessage(e).getMessage());
      log.error("Errore durante la connessione al server di attestati: {}", e.getMessage());
      render(office, validYear, validMonth);
    }

    List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    final Set<Integer> matricoleEpas = people.stream().map(person -> person.number)
        .distinct().collect(Collectors.toSet());

    final Set<Integer> notInEpas = Sets.difference(matricoleAttestati, matricoleEpas);
    final Set<Integer> notInAttestati = Sets.difference(matricoleEpas, matricoleAttestati);

    final Set<Integer> matchNumbers = Sets.newHashSet(matricoleEpas);
    matchNumbers.retainAll(matricoleAttestati);

    render(office, validYear, validMonth, people, notInEpas, notInAttestati, matchNumbers);
  }

  /**
   * @param personId id della persona
   * @param year     anno
   * @param month    mese
   */
  public static void personStatus(Long personId, int year, int month) {
    final Person person = personDao.getPersonById(personId);

    // Costruisco lo status generale
    PersonCertificationStatus personCertificationStatus = null;
    try {
      personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, null);
    } catch (Exception e) {
      log.error("Errore nel recupero delle informazioni dal server di attestati per la persona {}: {}",
          person, cleanMessage(e).getMessage());
      return;
    }

    final Map.Entry<Office, YearMonth> key = new AbstractMap
        .SimpleEntry<>(person.office, new YearMonth(year, month));

    // La percentuale di completamento della progress bar rispetto al totale da elaborare
    double stepSize = 0;
    try {
      stepSize = CacheValues.elaborationStep.get(key);
    } catch (Exception e) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}: {}",
          person, cleanMessage(e).getMessage());
      return;
    }


    render(personCertificationStatus, stepSize);
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
   * Elaborazione.
   *
   * @param officeId sede
   * @param year     anno
   * @param month    mese
   */
  public static void processAll(Long officeId, Integer year, Integer month) {

    flash.clear();  //non avendo per adesso un meccanismo di redirect pulisco il flash...

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    //Il mese selezionato è abilitato?
    boolean autenticate = certificationService.authentication(office, true);
    if (!autenticate) {
      flash.error("L'utente app.epas non è abilitato alla sede selezionata");
      renderTemplate("@certifications", office, year, month);
    }

    //Lo stralcio è stato effettuato?
    final Map.Entry<Office, YearMonth> key = new AbstractMap
        .SimpleEntry<>(office, new YearMonth(year, month));
    try {
      Set<Integer> matricoleAttestati = CacheValues.AttestatiSerialNumbers.get(key);
    } catch (Exception e) {
      flash.error("Errore di connessione ad Attestati - %s", e.getMessage());
      render(office, year, month);
    }

//    //Lo stralcio è stato effettuato?
//    Set<Integer> numbers = certificationService.peopleList(office, year, month);
//    if (numbers.isEmpty()) {
//      flash.error("E' necessario effettuare lo stralcio dei dati per processare "
//          + "gli attestati (sede %s, anno %s, mese %s).", office.name, year, month);
//      renderTemplate("@certifications", office, year, month, numbers);
//    }

    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
    boolean peopleNotInAttestati = false;

//    for (Person person : people) {
//
//      // Costruisco lo status generale
//      PersonCertificationStatus personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month, numbers);
//
////      if (personCertificationStatus.match()) {
////
////        if (!personCertificationStatus.validate) {
////          // Se l'attestato non è stato validato applico il process
////          certificationService.process(personCertificationStatus, token);
////        }
////        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
////        numbers.remove(person.number);
////      }
//
//      peopleCertificationStatus.add(personCertificationStatus);
//    }

//    flash.success("Elaborazione completata");
//
//    renderTemplate("@certifications", office, year, month, numbers, peopleNotInAttestati,
//        peopleCertificationStatus);
  }

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
//      matricoleAttestati = CacheValues.AttestatiSerialNumbers.get(key);
//    } catch (Exception e) {
//      flash.error("Errore di connessione ad Attestati - %s", e.getMessage());
//      render("@certifications", office, year, month);
//    }
//
//    List<Person> people = personDao.list(Optional.<String>absent(),
//        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
//
//    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
//    boolean peopleNotInAttestati = false;
//
//    for (Person person : people) {
//
//      // Costruisco lo status generale
//      PersonCertificationStatus personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month, numbers);
//
//      // Elimino ogni record
//      certificationService.emptyAttestati(personCertificationStatus);
//
//      // Ricostruzione nuovo stato (coi record eliminati)
//      personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month, numbers);
//
////      if (personCertificationStatus.match()) {
////        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
////        numbers.remove(person.number);
////      }
//
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
   *
   * @param e eccezione
   * @return L'ultimo elemento Throwable di una concatenazione di eccezioni
   */
  private static Throwable cleanMessage(Exception e) {
    // Recupera il messaggio pulito dalla gerarchia delle eccezioni
    Throwable throwable = e.getCause();
    while (throwable.getCause() != null) {
      throwable = throwable.getCause();
    }
    return throwable;
  }

}
