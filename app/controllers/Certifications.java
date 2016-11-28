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

import lombok.extern.slf4j.Slf4j;

import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.service.CertificationService;
import manager.attestati.service.PersonCertificationStatus;
import manager.configurations.ConfigurationManager;

import models.Office;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static IWrapperFactory factory;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static CertificationService certificationService;

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
    Optional<YearMonth> monthToUpload = factory.create(office).nextYearMonthToUpload();
    Verify.verify(monthToUpload.isPresent());

    if (year != null && month != null) {
      monthToUpload = Optional.fromNullable(new YearMonth(year, month));
    }

    year = monthToUpload.get().getYear();
    month = monthToUpload.get().getMonthOfYear();

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


    //Il mese selezionato è abilitato?
    boolean autenticate = certificationService.authentication(office, true);
    if (!autenticate) {
      flash.error("L'utente app.epas non è abilitato alla sede selezionata");
      render(office, year, month);
    }

    Set<Integer> matricoleAttestati = new HashSet<>();
    //Lo stralcio è stato effettuato?
    try {
      matricoleAttestati = certificationService.peopleList(office, year, month);
    } catch (Exception e) {
      flash.error("Errore di connessione ad Attestati - %s", e.getMessage());
      render(office, year, month);
    }

    List<Person> people = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    final Set<Integer> matricoleEpas = people.stream().map(person -> person.number)
        .distinct().collect(Collectors.toSet());

    final Set<Integer> notInEpas = Sets.difference(matricoleAttestati, matricoleEpas);
    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();

//    log.info("MATRICOLE ATTESTATI {} {}", matricoleAttestati.size(), matricoleAttestati.stream()
//        .sorted().collect(Collectors.toList()));
//    log.info("MATRICOLE EPAS {} {}", matricoleEpas.size(), matricoleEpas.stream()
//        .sorted().collect(Collectors.toList()));
//    log.info("DIFFERENZE {} {}", notInEpas.size(), notInEpas.stream()
//        .sorted().collect(Collectors.toList()));
//    for (Person person : people) {
//
//      // Costruisco lo status generale
//      PersonCertificationStatus personCertificationStatus = certificationService
//          .buildPersonStaticStatus(person, year, month, matricoleAttestati);
//
//      peopleCertificationStatus.add(personCertificationStatus);
//    }

    render(office, year, month, people, notInEpas, matricoleAttestati);
  }

  public static void personStatus(Long personId, int year, int month, int totalSize) {
    final Person person = personDao.getPersonById(personId);

    // Costruisco lo status generale
    PersonCertificationStatus personCertificationStatus = certificationService
        .buildPersonStaticStatus(person, year, month, null);
    int stepSize = totalSize;
    render(personCertificationStatus, stepSize);
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
    Set<Integer> numbers = certificationService.peopleList(office, year, month);
    if (numbers.isEmpty()) {
      flash.error("E' necessario effettuare lo stralcio dei dati per processare "
          + "gli attestati (sede %s, anno %s, mese %s).", office.name, year, month);
      renderTemplate("@certifications", office, year, month, numbers);
    }

    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
    boolean peopleNotInAttestati = false;

    for (Person person : people) {

      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, numbers);

//      if (personCertificationStatus.match()) {
//
//        if (!personCertificationStatus.validate) {
//          // Se l'attestato non è stato validato applico il process
//          certificationService.process(personCertificationStatus, token);
//        }
//        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
//        numbers.remove(person.number);
//      }

      peopleCertificationStatus.add(personCertificationStatus);
    }

    flash.success("Elaborazione completata");

    renderTemplate("@certifications", office, year, month, numbers, peopleNotInAttestati,
        peopleCertificationStatus);
  }

  public static void emptyCertifications(Long officeId, int year, int month) {

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
    Set<Integer> numbers = certificationService.peopleList(office, year, month);
    if (numbers.isEmpty()) {
      flash.error("E' necessario effettuare lo stralcio dei dati per processare "
          + "gli attestati (sede %s, anno %s, mese %s).", office.name, year, month);
      renderTemplate("@certifications", office, year, month, numbers);
    }

    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();

    List<PersonCertificationStatus> peopleCertificationStatus = Lists.newArrayList();
    boolean peopleNotInAttestati = false;

    for (Person person : people) {

      // Costruisco lo status generale
      PersonCertificationStatus personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, numbers);

      // Elimino ogni record
      certificationService.emptyAttestati(personCertificationStatus);

      // Ricostruzione nuovo stato (coi record eliminati)
      personCertificationStatus = certificationService
          .buildPersonStaticStatus(person, year, month, numbers);

//      if (personCertificationStatus.match()) {
//        // La matricola la rimuovo da quelle in attestati (alla fine rimangono quelle non trovate)
//        numbers.remove(person.number);
//      }


      peopleCertificationStatus.add(personCertificationStatus);
    }

    flash.success("Elaborazione completata");

    renderTemplate("@certifications", office, year, month, numbers, peopleNotInAttestati,
        peopleCertificationStatus);

  }

  /**
   * I codici assenza in attestati.
   */
  public static void certificationsAbsenceCodes() {

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


}
