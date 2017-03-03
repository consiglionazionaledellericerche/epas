package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import dao.CertificationDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;
import dao.PersonReperibilityDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperCompetenceCode;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;
import helpers.jpa.ModelQuery.SimpleResults;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.SecureManager;
import manager.competences.CompetenceCodeDTO;
import manager.competences.ShiftTimeTableDto;
import manager.recaps.competence.CompetenceRecap;
import manager.recaps.competence.CompetenceRecapFactory;
import manager.recaps.competence.PersonMonthCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecapFactory;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.CertificatedData;
import models.Certification;
import models.Competence;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftShiftType;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.TotalOvertime;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;


@With({Resecure.class})
public class Competences extends Controller {

  @Inject
  private static CompetenceRecapFactory competenceRecapFactory;
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
  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  private static PersonReperibilityDayDao reperibilityDao;
  @Inject
  private static CertificationDao certificationDao;
  @Inject
  private static PersonMonthRecapDao pmrDao;
  @Inject
  private static ShiftDao shiftDao;


  /**
   * Crud CompetenceCode.
   */
  public static void manageCompetenceCode() {

    List<CompetenceCodeGroup> groupList = competenceCodeDao.getAllGroups();
    render(groupList);
  }

  /**
   * Nuovo Codice Competenza.
   */
  public static void insertCompetenceCode() {

    render("@edit");
  }

  /**
   * salva la competenza come abilitata/disabilitata.
   *
   * @param competenceCodeId l'id della competenza da abilitare/disabilitare
   * @param confirmed        se siamo in fase di conferma o meno
   */
  public static void evaluateCompetenceCode(Long competenceCodeId, boolean confirmed) {
    CompetenceCode comp = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
    notFoundIfNull(comp);
    if (!confirmed) {
      confirmed = true;
      render(comp, confirmed);
    }
    if (comp.disabled) {
      comp.disabled = false;
    } else {
      comp.disabled = true;
    }
    comp.save();
    flash.success("Operazione effettuata");
    manageCompetenceCode();
  }

  /**
   * restituisce la form per l'aggiunta di un codice di competenza a un gruppo.
   *
   * @param competenceCodeGroupId l'id del gruppo di codici competenza
   */
  public static void addCompetenceCodeToGroup(Long competenceCodeGroupId) {
    CompetenceCodeGroup group = competenceCodeDao.getGroupById(competenceCodeGroupId);
    notFoundIfNull(group);
    render(group);
  }

  /**
   * aggiunge le competenze al gruppo passato come parametro.
   *
   * @param group il gruppo a cui aggiungere competenze
   */
  public static void addCompetences(CompetenceCodeGroup group, CompetenceCode code) {
    notFoundIfNull(group);
    if (!code.limitUnit.name().equals(group.limitUnit.name())) {
      Validation.addError("code", "L'unità di misura del limite del codice è diversa "
          + "da quella del gruppo");
    }
    if (!code.limitType.name().equals(group.limitType.name())) {
      Validation.addError("code", "Il tipo di limite del codice è diverso da quello del gruppo");
    }
    if (Validation.hasErrors()) {

      response.status = 400;
      render("@addCompetenceCodeToGroup", group);
    }
    code.competenceCodeGroup = group;
    code.save();
    group.competenceCodes.add(code);
    group.save();
    flash.success(String.format("Aggiornate con successo le competenze del gruppo"));
    manageCompetenceCode();

  }

  /**
   * Nuovo gruppo di codici competenza.
   */
  public static void insertCompetenceCodeGroup() {
    CompetenceCodeGroup group = new CompetenceCodeGroup();
    render(group);
  }

  /**
   * Modifica codice competenza. Chiama la show se chi invoca il metodo è un utente fisico.
   *
   * @param competenceCodeId codice
   */
  public static void edit(Long competenceCodeId) {
    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
    if (Security.getUser().get().person != null) {
      render("@show", competenceCode);
    } else {
      render(competenceCode);
    }
  }

  /**
   * metodo che renderizza la sola visualizzazione dei dati di un competenceCode.
   *
   * @param competenceCode il codice di competenza da visualizzare
   */
  public static void show(CompetenceCode competenceCode) {
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
   * salva il gruppo di codici competenza con i codici associati.
   *
   * @param group il gruppo di codici competenza
   */
  public static void saveGroup(@Valid final CompetenceCodeGroup group) {

    if (Validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());
      render("@insertCompetenceCodeGroup", group);
    }
    group.save();
    for (CompetenceCode code : group.competenceCodes) {
      code.competenceCodeGroup = group;
      code.save();
    }

    flash.success(String.format("Gruppo %s aggiunto con successo", group.label));

    manageCompetenceCode();
  }

  /**
   * cancella il codice di competenza dal gruppo.
   *
   * @param competenceCodeId l'id del codice di competenza da cancellare
   */
  public static void deleteCompetenceFromGroup(Long competenceCodeId, boolean confirmed) {
    CompetenceCode code = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
    notFoundIfNull(code);
    if (!confirmed) {
      confirmed = true;
      render(code, confirmed);
    }
    CompetenceCodeGroup group = code.competenceCodeGroup;
    group.competenceCodes.remove(code);
    group.save();
    code.competenceCodeGroup = null;
    code.save();
    flash.success("Codice rimosso con successo");
    manageCompetenceCode();
  }

  /**
   * Riepilogo competenze abilitate per la sede.
   */
  public static void enabledCompetences(Integer year, Integer month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    LocalDate date = new LocalDate(year, month, 1);
    List<Person> personList = personDao.list(Optional.<String>absent(), Sets.newHashSet(office),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();
    Map<Person, List<CompetenceCodeDTO>> map = competenceManager
        .createMap(personList, date.getYear(), date.getMonthOfYear());

    render(office, map, date, year, month);
  }

  /**
   * Crud per abilitare le competenze alla persona.
   *
   * @param personId persona
   */
  public static void updatePersonCompetence(Long personId, int year, int month) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    boolean certificationsSent = false;
    List<Certification> certificationList = certificationDao
        .personCertifications(person, year, month);
    CertificatedData certificatedData = pmrDao.getPersonCertificatedData(person, month, year);
    if (!certificationList.isEmpty() || certificatedData != null) {
      certificationsSent = true;
    }
    LocalDate date = new LocalDate(year, month, 1);
    List<PersonCompetenceCodes> pccList = competenceCodeDao
        .listByPerson(person, Optional.fromNullable(date));
    List<CompetenceCode> codeListIds = Lists.newArrayList();
    for (PersonCompetenceCodes pcc : pccList) {
      codeListIds.add(pcc.competenceCode);
    }
    render(person, codeListIds, year, month, certificationsSent);
  }

  /**
   * salva la nuova configurazione dei codici di competenza abilitati per la persona.
   *
   * @param codeListIds la lista degli id dei codici di competenza per la nuova configurazione
   * @param personId    l'id della persona di cui modificare le competenze abilitate
   * @param year        l'anno di riferimento
   * @param month       il mese di riferimento
   */
  public static void saveNewCompetenceConfiguration(List<Long> codeListIds,
      Long personId, int year, int month) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);

    LocalDate date = new LocalDate(year, month, 1);

    List<PersonCompetenceCodes> pccList = competenceCodeDao
        .listByPerson(person, Optional.fromNullable(date));
    List<CompetenceCode> codeToAdd = competenceManager.codeToSave(pccList, codeListIds);
    List<CompetenceCode> codeToRemove = competenceManager.codeToDelete(pccList, codeListIds);
    
    competenceManager.persistChanges(person, codeToAdd, codeToRemove, date);

    flash.success(String.format("Aggiornate con successo le competenze per %s",
        person.fullName()));
    Competences.enabledCompetences(date.getYear(),
        date.getMonthOfYear(), person.office.id);

  }


  /**
   * Riepilgo competenze del dipendente.
   *
   * @param year  year
   * @param month month
   */
  public static void competences(int year, int month) {

    Optional<User> user = Security.getUser();

    Verify.verify(user.isPresent());
    Verify.verifyNotNull(user.get().person);

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
      Stampings.stampings(year, month);
    }
    
    List<PersonCompetenceCodes> pccList = competenceCodeDao
        .listByPerson(person, Optional.fromNullable(LocalDate.now()
            .withMonthOfYear(month).withYear(year)));
    List<CompetenceCode> codeListIds = Lists.newArrayList();
    for (PersonCompetenceCodes pcc : pccList) {
      codeListIds.add(pcc.competenceCode);
    }
    
    List<Competence> competenceList = competenceDao.getCompetences(Optional.fromNullable(person), 
        year, Optional.fromNullable(month), codeListIds);
    Map<CompetenceCode, String> map = competenceManager.createMapForCompetences(competenceList);

    Optional<PersonMonthCompetenceRecap> personMonthCompetenceRecap =
        personMonthCompetenceRecapFactory.create(contract.get(), month, year);

    render(personMonthCompetenceRecap, person, year, month, competenceList, map);

  }

  /**
   * Competenze assegnate nel mese nell'officeId col codice specificato.
   *
   * @param year           year
   * @param month          month
   * @param officeId       officeId
   * @param competenceCode filtro competenceCode
   */
  public static void showCompetences(Integer year, Integer month, Long officeId,
      CompetenceCode competenceCode) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    boolean servicesInitialized = true;
    //Redirect in caso di mese futuro
    LocalDate today = LocalDate.now();
    if (today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      showCompetences(year, today.getMonthOfYear(), officeId, competenceCode);
    }
    //La lista dei codici competenceCode da visualizzare nella select
    // Ovvero: I codici attualmente attivi per almeno un dipendente di quell'office
    List<CompetenceCode> competenceCodeList = competenceDao
        .activeCompetenceCode(office, new LocalDate(year, month, 1));
    if (competenceCodeList.isEmpty()) {
      flash.error("Per visualizzare la sezione Competenze è necessario "
          + "abilitare almeno un codice competenza ad un dipendente.");
      Competences.enabledCompetences(year, month, officeId);
    }
    // genero un controllo sul fatto che esistano servizi attivi per cui la reperibilità
    // può essere utilizzata
    servicesInitialized = competenceManager
        .isServiceForReperibilityInitialized(office, competenceCodeList);
    if (competenceCode == null || !competenceCode.isPersistent()) {
      competenceCode = competenceCodeList.get(0);
      notFoundIfNull(competenceCode);
    }

    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competenceCode);

    CompetenceRecap compDto = competenceRecapFactory
        .create(office, competenceCode, year, month);

    render(year, month, office, competenceCodeList, wrCompetenceCode, compDto, servicesInitialized);

  }

  /**
   * @param personId     l'id della persona
   * @param competenceId l'id della competenza
   * @param month        il mese
   * @param year         l'anno ritorna la form di inserimento di un codice di competenza per una
   *                     persona.
   */
  public static void insertCompetence(Long personId, Long competenceId, int month, int year) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    CompetenceCode code = competenceCodeDao.getCompetenceCodeById(competenceId);
    notFoundIfNull(code);
    Office office = person.office;
    Competence competence = new Competence(person, code, year, month);
    if (competence.competenceCode.code.equals("S1")) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(competence.person,
          competence.year, competence.month, true);
      render("@editCompetence", competence, psDto, office, year, month, person);
    }
    render("@editCompetence", competence, office, year, month, person);
  }

  /**
   * genera la form di inserimento per le competenze.
   *
   * @param competenceId l'id della competenza da aggiornare.
   */
  public static void editCompetence(Long competenceId) {
    Competence competence = competenceDao.getCompetenceById(competenceId);
    notFoundIfNull(competence);
    Office office = competence.person.office;
    if (competence.competenceCode.code.equals("S1")) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(competence.person,
          competence.year, competence.month, true);
      render(competence, psDto, office);
    }

    render(competence, office);
  }

  /**
   * salva la competenza passata per parametro se è conforme ai limiti eventuali previsti per essa.
   *
   * @param competence la competenza relativa alla persona
   */
  public static void saveCompetence(Integer valueApproved, @Valid Competence competence) {

    notFoundIfNull(competence);
    Office office = competence.person.office;
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    String result = "";

    if (!Validation.hasErrors()) {
      result = competenceManager.canAddCompetence(competence, valueApproved);
      if (!result.isEmpty()) {
        Validation.addError("valueApproved", result);
      }
    }
    if (Validation.hasErrors()) {

      response.status = 400;
      render("@editCompetence", competence, office);
    }


    competenceManager.saveCompetence(competence, valueApproved);
    if (competence.competenceCode.code.equalsIgnoreCase("S1")
        || competence.competenceCode.code.equalsIgnoreCase("S2")
        || competence.competenceCode.code.equalsIgnoreCase("S3")) {

      consistencyManager.updatePersonSituation(competence.person.id,
          new LocalDate(competence.year, competence.month, 1));
    }
    int month = competence.month;
    int year = competence.year;
    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competence.competenceCode);
    List<CompetenceCode> competenceCodeList = competenceDao
        .activeCompetenceCode(office, new LocalDate(year, month, 1));
    List<Competence> compList = competenceDao.getCompetencesInOffice(year, month,
        Lists.newArrayList(competence.competenceCode.code), office, false);
    flash.success("Aggiornato correttamente il valore della competenza");

    CompetenceRecap compDto = competenceRecapFactory
        .create(office, competence.competenceCode, year, month);
    boolean servicesInitialized = competenceManager
        .isServiceForReperibilityInitialized(office, competenceCodeList);


    render("@showCompetences", year, month, office,
        wrCompetenceCode, competenceCodeList, compList, compDto, servicesInitialized);
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
   * Esporta in formato .csv la situazione annuale degli straordinari. 
   * TODO: parametrico all'office.
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
   * Le competence approvate nell'anno alle persone della sede. 
   * TODO: implementare un metodo nel manager nel quale spostare la business logic 
   * di questa azione.
   *
   * @param year        anno
   * @param onlyDefined solo per determinati
   * @param officeId    sede
   */
  public static void approvedCompetenceInYear(int year, boolean onlyDefined, Long officeId) {


    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    
    Set<Person> personSet = Sets.newTreeSet(Person.personComparator());

    Map<CompetenceCode, Integer> totalValueAssigned = Maps.newHashMap();

    Map<Person, Map<CompetenceCode, Integer>> mapPersonCompetenceRecap = 
        Maps.newTreeMap(Person.personComparator());

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
   */
  public static void monthlyOvertime(Integer year, Integer month) {

    final User user = Security.getUser().get();
    Table<Person, String, Integer> tableFeature = null;
    LocalDate beginMonth = null;

    beginMonth = new LocalDate(year, month, 1);

    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    SimpleResults<Person> simpleResults = personDao.listForCompetence(code,
        Optional.<String>absent(),
        Sets.newHashSet(user.person.office),
        false,
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
        Optional.fromNullable(user.person));
    tableFeature = competenceManager.composeTableForOvertime(year, month,
        null, null, user.person.office, beginMonth, simpleResults, code);

    render(tableFeature, year, month, simpleResults);

  }

  /**
   * ricalcola tutti i valori del codice di competenza a presenza mensile recuperato dall'id 
   *     passato come parametro per tutti i dipendenti della sede recuperata dall'id passato 
   *     come parametro per l'anno e il mese passati come parametro.
   * @param officeId l'id della sede per cui fare i conteggi
   * @param codeId l'id del codice di competenza da controllare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void recalculateBonus(Long officeId, Long codeId, int year, int month) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeById(codeId);
    YearMonth yearMonth = new YearMonth(year, month);
    competenceManager.applyBonus(Optional.fromNullable(office), competenceCode, yearMonth);
    
    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competenceCode);
    List<CompetenceCode> competenceCodeList = competenceDao
        .activeCompetenceCode(office, new LocalDate(year, month, 1)); 
    
    CompetenceRecap compDto = competenceRecapFactory
        .create(office, competenceCode, year, month);
    boolean servicesInitialized = competenceManager
        .isServiceForReperibilityInitialized(office, competenceCodeList);
    flash.success("Aggiornati i valori per la competenza %s", competenceCode.code);

    render("@showCompetences", year, month, office,
        wrCompetenceCode, competenceCodeList, compDto, servicesInitialized);
  }

  /* ****************************************************************
   * Parte relativa ai servizi da attivare per reperibilità e turni *
   * ****************************************************************/

  /**
   * Metodo che renderizza la form di visualizzazione dei servizi attivi per un ufficio.
   *
   * @param officeId l'id dell'ufficio per cui visualizzare i servizi attivi
   */
  public static void activateServices(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    List<PersonReperibilityType> prtList = reperibilityDao
        .getReperibilityTypeByOffice(office, Optional.<Boolean>absent());
    List<ShiftCategories> scList = shiftDao
        .getAllCategoriesByOffice(office, Optional.<Boolean>absent());

    render(office, prtList, scList);
  }

  /**
   * Metodo che renderizza la form di inserimento di un nuovo servizio da attivare
   * per la reperibilità.
   *
   * @param officeId l'id dell'ufficio a cui associare il servizio
   */
  public static void addReperibility(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted(office);
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));

    render("@editReperibility", officePeople, office);
  }

  /**
   * Metodo che renderizza la form di inserimento di un nuovo servizio da attivare
   *     per la reperibilità.
   * @param officeId l'id dell'ufficio a cui associare il servizio
   */
  public static void addShift(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted(office);
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    boolean nuovo = true;
    render("@editShift", officePeople, office, nuovo);
  }

  /**
   * metodo che persiste il servizio per reperibilità o comunque lo modifica se già presente.
   *
   * @param type   il servizio da persistere
   * @param office la sede di appartenenza del servizio
   */
  public static void saveReperibility(@Valid PersonReperibilityType type, @Valid Office office) {

    rules.checkIfPermitted(office);

    if (Validation.hasErrors()) {
      response.status = 400;
      List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
          new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
      render("@editShift", type, officePeople, office);
    }
    type.office = office;
    type.save();
    flash.success("Nuovo servizio %s inserito correttamente per la sede %s",
        type.description, type.office);
    activateServices(type.office.id);
  }

  /**
   * 
   * @param cat il servizio per turno
   * @param office la sede a cui si vuole collegare il servizio
   *     metodo che persiste il servizio associandolo alla sede.
   */
  public static void saveShift(@Valid ShiftCategories cat, @Valid Office office) {

    rules.checkIfPermitted(office);

    if (Validation.hasErrors()) {
      response.status = 400;
      List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
          new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
      render("@editShift", cat, officePeople, office);
    }
    cat.office = office;
    cat.save();
    flash.success("Nuovo servizio %s inserito correttamente per la sede %s",
        cat.description, cat.office);
    activateServices(cat.office.id);
  }

  /**
   * metodo che controlla e poi persiste la disabilitazione/abilitazione di un servizio.
   *
   * @param reperibilityTypeId l'id del servizio da disabilitare/abilitare
   * @param confirmed          il booleano per consentire la persistenza di una modifica
   */
  public static void evaluateReperibility(Long reperibilityTypeId, boolean confirmed) {
    PersonReperibilityType type = reperibilityDao.getPersonReperibilityTypeById(reperibilityTypeId);
    notFoundIfNull(type);
    rules.checkIfPermitted(type.office);
    if (!confirmed) {
      confirmed = true;
      render(type, confirmed);
    }
    if (type.disabled) {
      type.disabled = false;
      type.save();
      flash.success("Riabilitato servizio %s", type.description);
      activateServices(type.office.id);
    }
    if (!type.personReperibilities.isEmpty()) {
      type.disabled = true;
      type.save();
      flash.success("Il servizio è stato disabilitato e non rimosso perchè legato con informazioni "
          + "importanti presenti in altre tabelle");

    } else {
      type.delete();
      flash.success("Servizio rimosso con successo");
    }
    activateServices(type.office.id);
  }

  /**
   * metodo che ritorna la form di inserimento/modifica di un servizio.
   *
   * @param reperibilityTypeId l'id del servizio da editare
   */
  public static void editReperibility(Long reperibilityTypeId) {
    PersonReperibilityType type = reperibilityDao.getPersonReperibilityTypeById(reperibilityTypeId);
    Office office = type.office;
    rules.checkIfPermitted(office);
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));

    render(type, officePeople, office);
  }


  /**
   * metodo che controlla e poi persiste la disabilitazione/abilitazione di un servizio.
   * @param shiftCategoryId l'id del servizio da disabilitare/abilitare
   * @param confirmed il booleano per consentire la persistenza di una modifica
   */
  public static void evaluateShift(Long shiftCategoryId, boolean confirmed) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.office);
    if (!confirmed) {
      confirmed = true;
      render(cat, confirmed);
    }
    if (cat.disabled) {
      cat.disabled = false;
      cat.save();
      flash.success("Riabilitato servizio %s", cat.description);
      activateServices(cat.office.id);
    }
    List<ShiftType> shiftTypeList = shiftDao.getTypesByCategory(cat);
    if (!shiftTypeList.isEmpty()) {
      cat.disabled = true;
      cat.save();
      flash.success("Il servizio è stato disabilitato e non rimosso perchè legato con informazioni "
          + "importanti presenti in altre tabelle");

    } else {
      cat.delete();
      flash.success("Servizio rimosso con successo");
    }
    activateServices(cat.office.id);
  }

  /**
   * metodo che ritorna la form di inserimento/modifica di un servizio.
   * @param shiftCategoryId l'id del servizio da editare
   */
  public static void editShift(Long shiftCategoryId) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    Office office = cat.office;
    rules.checkIfPermitted(office);
    Map<ShiftType, List<PersonShiftShiftType>> map = Maps.newHashMap();
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    cat.shiftTypes.forEach(item -> {
      List<PersonShiftShiftType> psstList = shiftDao
          .getAssociatedPeopleToShift(item, Optional.fromNullable(LocalDate.now()));
      map.put(item, psstList);
    });
    boolean nuovo = false;
    render(cat, officePeople, office, map, nuovo);
  }

  /**
   * metodo che ritorna al template le informazioni per poter configurare correttamente il turno.
   * @param shiftCategoryId l'id del servzio da configurare
   */
  public static void configureShift(Long shiftCategoryId) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.office);
    ShiftType type = new ShiftType();
    List<ShiftTimeTable> shiftList = shiftDao.getAllShifts();
    List<ShiftTimeTableDto> dtoList = competenceManager.convertFromShiftTimeTable(shiftList);
    render(dtoList, cat, shiftList, type);
  }
  
  /**
   * metodo che ritorna al template le informazioni sull'attività passata come parametro.
   * @param shiftTypeId l'id dell'attività da configurare
   */
  public static void manageShiftType(Long shiftTypeId) {
    Optional<ShiftType> shiftType = shiftDao.getShiftTypeById(shiftTypeId);
    
    if (!shiftType.isPresent()) {
      flash.error("Si cerca di caricare un'attività inesistente! Verificare l'id");
      activateServices(new Long(session.get("officeSelected")));
    } else {
      rules.checkIfPermitted(shiftType.get().shiftCategories.office);
      ShiftType type = shiftType.get();
      Office office = officeDao.getOfficeById(type.shiftCategories.office.id);
      List<PersonShift> peopleForShift = shiftDao.getPeopleForShift(office);
      List<PersonShift> peopleIds = Lists.newArrayList();
      List<PersonShiftShiftType> associatedPeopleShift = shiftDao
          .getAssociatedPeopleToShift(type, Optional.fromNullable(LocalDate.now()));
      for (PersonShiftShiftType psst : associatedPeopleShift) {
        peopleIds.add(psst.personShift);
      }
      render(peopleIds,type, office, peopleForShift);
    }
  }
  
  /**
   * metodo che persiste i person_shift_shift_type.
   * @param peopleIds la lista degli id delle persone da aggiungere/rimuovere
   * @param type l'attività su cui aggiungere/rimuovere le persone
   */
  public static void linkPeopleToShift(List<Long> peopleIds, @Valid ShiftType type) {
    notFoundIfNull(type);
    rules.checkIfPermitted(type.shiftCategories.office);
    if (Validation.hasErrors()) {
      response.status = 400;
      List<PersonShift> peopleForShift = shiftDao.getPeopleForShift(type.shiftCategories.office);
      Office office = type.shiftCategories.office;     
      render("@manageShiftType", type, peopleForShift, peopleIds, office);
    }
    type.save();
    List<PersonShiftShiftType> psstList = shiftDao.getAssociatedPeopleToShift(type, 
        Optional.fromNullable(LocalDate.now()));
    List<PersonShift> peopleToAdd = competenceManager.peopleToAdd(psstList, peopleIds);
    List<PersonShift> peoleToRemove = competenceManager.peopleToDelete(psstList, peopleIds);
    competenceManager.persistPersonShiftShiftType(peopleToAdd, type,peoleToRemove);
    flash.success("Aggiornata lista di persone appartenenti all'attività di turno %s", 
        type.description);
    activateServices(type.shiftCategories.office.id);
  }
  
  /**
   * metodo che associa il servizio, all'attività e alla timetable associata.
   * @param shift l'id della timetable da associare al turno
   * @param cat il servizio per cui si vuol definire l'attività
   * @param type l'attività da linkare al servizio
   */
  public static void linkTimeTableToShift(Long shift, ShiftCategories cat, @Valid ShiftType type) {
    
    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.office);
    ShiftTimeTable timeTable = shiftDao.getShiftTimeTableById(shift);
    notFoundIfNull(timeTable);  
    
    if (Validation.hasErrors()) {
      response.status = 400;
      List<ShiftTimeTable> shiftList = shiftDao.getAllShifts();
      List<ShiftTimeTableDto> dtoList = competenceManager.convertFromShiftTimeTable(shiftList);
      render("@configureShift", shiftList, dtoList, cat, type);
    }
    type.shiftCategories = cat;
    type.shiftCategories = cat;
    type.shiftTimeTable = timeTable;
    type.save();
    
    flash.success("Configurato correttamente il servizio %s", cat.description);
    activateServices(cat.office.id);
  }
}
