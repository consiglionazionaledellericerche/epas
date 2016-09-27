package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.mysema.query.SearchResults;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperCompetenceCode;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;
import helpers.jpa.ModelQuery.SimpleResults;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.SecureManager;
import manager.recaps.PersonCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecapFactory;

import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.TotalOvertime;
import models.User;

import org.joda.time.LocalDate;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

@Slf4j
@With({Resecure.class})
public class Competences extends Controller {

  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static PersonMonthCompetenceRecapFactory personMonthCompetenceRecapFactory;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static CompetenceManager competenceManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static CompetenceDao competenceDao;
  @Inject
  private static CompetenceCodeDao competenceCodeDao;
  @Inject
  private static ConsistencyManager consistencyManager;

  /**
   * Crud CompetenceCode.
   */
  public static void manageCompetenceCode() {

    List<CompetenceCode> compCodeList = competenceCodeDao.getAllCompetenceCode();
    render(compCodeList);
  }

  /**
   * Nuovo Codice Competenza.
   */
  public static void insertCompetenceCode() {

    render("@edit");
  }

  /**
   * Modifica codice competence.
   *
   * @param competenceCodeId codice
   */
  public static void edit(Long competenceCodeId) {

    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
    render(competenceCode);
  }

  /**
   * Salva codice competenza.
   *
   * @param competenceCode codice
   */
  public static void save(@Valid final CompetenceCode competenceCode) {

    if (Validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());

      render("@edit", competenceCode);
    }
    competenceCode.save();

    flash.success(String.format("Codice %s aggiunto con successo", competenceCode.code));

    manageCompetenceCode();
  }

  /**
   * Riepilogo competenze abilitate per la sede.
   */
  public static void enabledCompetences(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    LocalDate date = new LocalDate();
    List<Person> personList = personDao.list(Optional.<String>absent(), Sets.newHashSet(office),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();

    // TODO: togliere questa storpiaggine.
    List<CompetenceCode> allCodeList = competenceCodeDao.getAllCompetenceCode();


    render(personList, allCodeList, office);
  }

  /**
   * Crud per abilitare le competenze alla persona.
   *
   * @param personId persona
   */
  public static void updatePersonCompetence(Long personId) {

    if (personId == null) {

      flash.error("Persona inesistente");
      Application.indexAdmin();
    }
    Person person = personDao.getPersonById(personId);
    rules.checkIfPermitted(person.office);
    PersonCompetenceRecap pcr = new PersonCompetenceRecap(
        person, competenceCodeDao.getAllCompetenceCode());

    render(pcr, person);
  }

  /**
   * Salva la nuova configurazione delle competenze abilitate per la persona.
   *
   * @param personId   personId
   * @param competence mappa con i valori per ogni competenza
   */
  public static void saveNewCompetenceConfiguration(Long personId,
      Map<String, Boolean> competence) {
    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);

    List<CompetenceCode> competenceCode = competenceCodeDao.getAllCompetenceCode();
    if (competenceManager
        .saveNewCompetenceEnabledConfiguration(competence, competenceCode, person)) {
      flash.success(String.format("Aggiornate con successo le competenze per %s %s",
          person.name, person.surname));
    } else {
      // TODO: ????????????????????
    }
    Competences.enabledCompetences(person.office.id);

  }


  /**
   * Riepilgo competenze del dipendente.
   *
   * @param year  year
   * @param month month
   */
  public static void competences(int year, int month) {

    Optional<User> user = Security.getUser();

    if (!user.isPresent() || user.get().person == null) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }

    //Redirect in caso di mese futuro
    LocalDate today = LocalDate.now();
    if (year > today.getYear() || today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      competences(year, today.getMonthOfYear());
    }

    Person person = user.get().person;

    Optional<Contract> contract = wrapperFactory.create(person)
        .getLastContractInMonth(year, month);

    if (!contract.isPresent()) {
      flash.error("Nessun contratto attivo nel mese.");
      renderTemplate("Application/indexAdmin.html");
    }

    Optional<PersonMonthCompetenceRecap> personMonthCompetenceRecap =
        personMonthCompetenceRecapFactory.create(contract.get(), month, year);

    render(personMonthCompetenceRecap, person, year, month);

  }

  /**
   * Competenze assegnate nel mese nell'officeId col codice specificato.
   *
   * @param year           year
   * @param month          month
   * @param officeId       officeId
   * @param name           filtro nome
   * @param competenceCode filtro competenceCode
   * @param page           filtro pagina
   */
  public static void showCompetences(Integer year, Integer month, Long officeId,
      String name, CompetenceCode competenceCode, Integer page) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    //Redirect in caso di mese futuro
    LocalDate today = LocalDate.now();
    if (today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      showCompetences(year, today.getMonthOfYear(), officeId, name, competenceCode, page);
    }
    if (page == null) {
      page = 0;
    }

    //La lista dei codici competenceCode da visualizzare nella select
    // Ovvero: I codici attualmente attivi per almeno un dipendente di quell'office
    List<CompetenceCode> competenceCodeList = competenceDao.activeCompetenceCode(office);
    if (competenceCodeList.size() == 0) {
      flash.error("Per visualizzare la sezione Competenze è necessario "
          + "abilitare almeno un codice competenza ad un dipendente.");
      Competences.enabledCompetences(officeId);
    }

    if (competenceCode == null || !competenceCode.isPersistent()) {
      competenceCode = competenceCodeList.get(0);
      ;
      notFoundIfNull(competenceCode);
    }
    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competenceCode);

    //Per permettere la modifica delle competenze nel template.
    // TODO: usare qualche tag.
    boolean editCompetence = false;
    if (secureManager.officesWriteAllowed(Security.getUser().get()).contains(office)) {
      editCompetence = true;
    }
    renderArgs.put("editCompetence", editCompetence);

    // Le persone che hanno quella competence attualmente abilitata
    SearchResults<?> results = personDao.listForCompetence(competenceCode,
        Optional.fromNullable(name), Sets.newHashSet(office), false,
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
        Optional.<Person>absent()).listResults();

    // TODO: creare quelle che non esistono (meglio eliminare x-editable).

    // TODO: mancano da visualizzare le competence assegnate nel mese a quelle
    // persone che non hanno più il relativo codice abilitato.

    List<String> code = competenceManager.populateListWithOvertimeCodes();

    List<Competence> competenceList = competenceDao
        .getCompetencesInOffice(year, month, code, office, false);
    int totaleOreStraordinarioMensile = competenceManager.getTotalMonthlyOvertime(competenceList);

    List<Competence> competenceYearList = competenceDao
        .getCompetencesInOffice(year, month, code, office, true);
    int totaleOreStraordinarioAnnuale = competenceManager
        .getTotalYearlyOvertime(competenceYearList);

    List<TotalOvertime> total = competenceDao.getTotalOvertime(year, office);
    int totaleMonteOre = competenceManager.getTotalOvertime(total);

    render(year, month, office, results, name, competenceCodeList, wrCompetenceCode,
        totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, totaleMonteOre);

  }

  /**
   * Aggiorna la competenza.
   *
   * @param pk    pk
   * @param name  name
   * @param value value
   */
  public static void updateCompetence(long pk, String name, Integer value) {
    final Competence competence = competenceDao.getCompetenceById(pk);

    notFoundIfNull(competence);
    if (validation.hasErrors()) {
      error(Messages.get(Joiner.on(",").join(validation.errors())));
    }
    rules.checkIfPermitted(competence.person.office);

    log.info("Anno competenza: {} Mese competenza: {}", competence.year, competence.month);

    competence.valueApproved = value;

    log.info("value approved before = {}", competence.valueApproved);

    competence.save();

    log.info("saved id={} (person={}) code={} (value={})", competence.id, competence.person,
        competence.competenceCode.code, competence.valueApproved);

    consistencyManager.updatePersonSituation(competence.person.id,
        new LocalDate(competence.year, competence.month, 1));

    renderText("ok");
  }


  /**
   * Pagina riepilogo monte ore per anno e sede.
   *
   * @param year     year
   * @param officeId sede
   */
  public static void totalOvertimeHours(int year, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<TotalOvertime> totalList = competenceDao.getTotalOvertime(year, office);
    int totale = competenceManager.getTotalOvertime(totalList);

    render(totalList, totale, year, office);
  }

  /**
   * Salva la nuova posta per straordinari.
   *
   * @param year      anno
   * @param numeroOre valore
   * @param officeId  sede
   */
  public static void saveOvertime(Integer year, String numeroOre, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    if (competenceManager.saveOvertime(year, numeroOre, officeId)) {
      flash.success(String.format("Aggiornato monte ore per l'anno %s", year));
    } else {
      flash.error("Inserire il segno (+) o (-) davanti al numero di ore da aggiungere (sottrarre)");
    }

    Competences.totalOvertimeHours(year, officeId);
  }


  /**
   * Report. Esporta in formato .csv la situazione annuale degli straordinari
   */
  public static void exportCompetences() {
    render();
  }

  /**
   * Esporta in formato .csv la situazione annuale degli straordinari. TODO: parametrico
   * all'office.
   *
   * @param year anno
   */
  public static void getOvertimeInYear(int year) throws IOException {


    List<Person> personList = personDao
        .listForCompetence(competenceCodeDao.getCompetenceCodeByCode("S1"),
            Optional.fromNullable(""),
            secureManager.officesReadAllowed(Security.getUser().get()),
            false, new LocalDate(year, 1, 1),
            new LocalDate(year, 12, 1).dayOfMonth().withMaximumValue(),
            Optional.<Person>absent()).list();

    FileInputStream inputStream = competenceManager.getOvertimeInYear(year, personList);
    renderBinary(inputStream, "straordinari" + year + ".csv");
  }

  /**
   * Le competence approvate nell'anno alle persone della sede. TODO: implementare un metodo nel
   * manager nel quale spostare la business logic di questa azione.
   *
   * @param year        anno
   * @param onlyDefined solo per determinati
   * @param officeId    sede
   */
  public static void approvedCompetenceInYear(int year, boolean onlyDefined, Long officeId) {


    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    Set<Person> personSet = Sets.newHashSet();

    Map<CompetenceCode, Integer> totalValueAssigned = Maps.newHashMap();

    Map<Person, Map<CompetenceCode, Integer>> mapPersonCompetenceRecap = Maps.newHashMap();

    List<Competence> competenceInYear = competenceDao
        .getCompetenceInYear(year, Optional.fromNullable(office));

    for (Competence competence : competenceInYear) {

      //Filtro tipologia del primo contratto nel mese della competenza
      if (onlyDefined) {
        IWrapperPerson wrPerson = wrapperFactory.create(competence.person);
        Optional<Contract> firstContract = wrPerson.getFirstContractInMonth(year, competence.month);
        if (!firstContract.isPresent()) {
          continue;    //questo errore andrebbe segnalato, competenza senza che esista contratto
        }
        IWrapperContract wrContract = wrapperFactory.create(firstContract.get());
        if (!wrContract.isDefined()) {
          continue;    //scarto la competence.
        }
      }
      //Filtro competenza non approvata
      if (competence.valueApproved == 0) {
        continue;
      }

      personSet.add(competence.person);

      //aggiungo la competenza alla mappa della persona
      Person person = competence.person;
      Map<CompetenceCode, Integer> personCompetences = mapPersonCompetenceRecap.get(person);

      if (personCompetences == null) {
        personCompetences = Maps.newHashMap();
      }
      Integer value = personCompetences.get(competence.competenceCode);
      if (value != null) {
        value = value + competence.valueApproved;
      } else {
        value = competence.valueApproved;
      }

      personCompetences.put(competence.competenceCode, value);

      mapPersonCompetenceRecap.put(person, personCompetences);

      //aggiungo la competenza al valore totale per la competenza
      value = totalValueAssigned.get(competence.competenceCode);

      if (value != null) {
        value = value + competence.valueApproved;
      } else {
        value = competence.valueApproved;
      }

      totalValueAssigned.put(competence.competenceCode, value);
    }
    List<IWrapperPerson> personList = FluentIterable
        .from(personSet)
        .transform(wrapperFunctionFactory.person()).toList();

    render(personList, totalValueAssigned, mapPersonCompetenceRecap,
        office, year, onlyDefined);

  }


  /**
   * restituisce il template per il responsabile di gruppo di lavoro contenente le informazioni su
   * giorni di presenza, straordinari, ore a lavoro...
   *
   * @param year  l'anno di riferimento
   * @param month il mese di riferimento
   * @param name  il nome su cui filtrare
   * @param page  la pagina su cui filtrare
   */
  public static void monthlyOvertime(Integer year, Integer month, String name, Integer page) {

    final User user = Security.getUser().get();
    if (page == null) {
      page = 0;
    }
    Table<Person, String, Integer> tableFeature = null;
    LocalDate beginMonth = null;
    if (year == 0 && month == 0) {
      int yearParams = params.get("year", Integer.class);
      int monthParams = params.get("month", Integer.class);
      beginMonth = new LocalDate(yearParams, monthParams, 1);
    } else {
      beginMonth = new LocalDate(year, month, 1);
    }
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    SimpleResults<Person> simpleResults = personDao.listForCompetence(code,
        Optional.fromNullable(name),
        Sets.newHashSet(user.person.office),
        false,
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
        Optional.fromNullable(user.person));
    tableFeature = competenceManager.composeTableForOvertime(year, month,
        page, name, user.person.office, beginMonth, simpleResults, code);


    if (year != 0 && month != 0) {
      render(tableFeature, year, month, simpleResults, name);
    } else {
      int yearParams = params.get("year", Integer.class);
      int monthParams = params.get("month", Integer.class);
      render(tableFeature, yearParams, monthParams, simpleResults, name);
    }
  }

}
