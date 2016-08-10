package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.beust.jcommander.internal.Lists;
import com.mysema.query.SearchResults;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;
import dao.PersonReperibilityDayDao;
import dao.wrapper.IWrapperCompetenceCode;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;
import helpers.jpa.ModelQuery.SimpleResults;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.SecureManager;

import manager.recaps.competence.PersonMonthCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecapFactory;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Competence;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.PersonReperibilityType;
import models.TotalOvertime;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

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
  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  private static PersonReperibilityDayDao reperibilityDao;

  /**
   * Crud CompetenceCode.
   */
  public static void manageCompetenceCode() {

    List<CompetenceCode> compCodeList = competenceCodeDao.getCodeWithoutGroup();
    List<CompetenceCodeGroup> groupList = competenceCodeDao.getAllGroups();
    render(compCodeList, groupList);
  }

  /**
   * Nuovo Codice Competenza.
   */
  public static void insertCompetenceCode() {

    render("@edit");
  }
  
  /**
   * 
   * @param competenceCodeId
   * @param confirmed
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
    if (!Validation.hasErrors()) {
      if (competenceCode.limitValue != null && competenceCode.limitDescription != null) {
        validation.addError("competenceCode.limitValue", 
            "Non valorizzare se valorizzato il campo descrizione limite");        
      }
    }
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
   * @param group il gruppo di codici competenza
   */
  public static void saveGroup(@Valid final CompetenceCodeGroup group) {
    if (!Validation.hasErrors()) {
      if (group.limitValue != null && group.limitDescription != null) {
        validation.addError("group.limitValue", 
            "Non valorizzare se valorizzato il campo descrizione limite");        
      }
    }
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
  public static void enabledCompetences(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    LocalDate date = new LocalDate();
    List<Person> personList = personDao.list(Optional.<String>absent(), Sets.newHashSet(office),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();

    render(personList, office);
  }

  /**
   * Crud per abilitare le competenze alla persona.
   *
   * @param personId persona
   */
  public static void updatePersonCompetence(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    
    render(person);
  }

  /**
   * Salva la nuova configurazione delle competenze abilitate per la persona.
   *
   * @param person la persona per cui si intende salvare le competenze abilitate
   */
  public static void saveNewCompetenceConfiguration(Person person) {
    
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    person.save();
    flash.success(String.format("Aggiornate con successo le competenze per %s",
      person.fullName()));
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
   * @param competenceCode filtro competenceCode
   */
  public static void showCompetences(Integer year, Integer month, Long officeId,
      CompetenceCode competenceCode) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    //Redirect in caso di mese futuro
    LocalDate today = LocalDate.now();
    if (today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      showCompetences(year, today.getMonthOfYear(), officeId, competenceCode);
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
      notFoundIfNull(competenceCode);
    }
    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competenceCode);

    List<Competence> compList = competenceDao.getCompetencesInOffice(year, month, 
        Lists.newArrayList(competenceCode.code), office, false);

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

    render(year, month, office, competenceCodeList, wrCompetenceCode, compList, 
        totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, totaleMonteOre);

  }


  /**
   * genera la form di inserimento per le competenze.
   * @param competenceId l'id della competenza da aggiornare.
   */
  public static void insertCompetence(Long competenceId) {
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
   * @param competence la competenza relativa alla persona
   */
  public static void saveCompetence(Integer valueApproved, Competence competence) {

    notFoundIfNull(competence);
    Office office = competence.person.office;
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    if (!validation.hasErrors()) {

      if (!competenceManager.canAddCompetence(competence, valueApproved)) {
        validation.addError("valueApproved", "Non può superare il limite previsto per il codice");
      }
    }
    if (validation.hasErrors()) {
      response.status = 400;
      render("@insertCompetence", competence, office);
    }

    competenceManager.saveCompetence(competence, valueApproved);
    consistencyManager.updatePersonSituation(competence.person.id, 
        new LocalDate(competence.year, competence.month, 1));
    int month = competence.month;
    int year = competence.year;
    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competence.competenceCode);
    List<CompetenceCode> competenceCodeList = competenceDao.activeCompetenceCode(office);
    List<Competence> compList = competenceDao.getCompetencesInOffice(year, month, 
        Lists.newArrayList(competence.competenceCode.code), office, false);
    flash.success("Aggiornato correttamente il valore della competenza");

    render("@showCompetences", year, month, office, 
        wrCompetenceCode, competenceCodeList, compList);
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
   */
  public static void monthlyOvertime(Integer year, Integer month) {

    if (!Security.getUser().get().person.isPersonInCharge) {
      forbidden();
    }
    User user = Security.getUser().get();
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
   * 
   * @param officeId
   */
  public static void activateServices(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    List<PersonReperibilityType> prtList = reperibilityDao.getReperibilityTypeByOffice(office);

    render(office, prtList);
  }

  /**
   * Metodo che renderizza la form di inserimento di un nuovo servizio da attivare per la reperibilità.
   * @param officeId
   */
  public static void addService(Long officeId) {
    PersonReperibilityType type = new PersonReperibilityType();
    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted(office);
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(office), 
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));

    render(type, officePeople);
  }

  /**
   * Metodo per la persistenza del servizio creato dalla form.
   */
  public static void saveService(@Valid PersonReperibilityType type) {
    rules.checkIfPermitted(type.office);
    if (validation.hasErrors()) {      
      response.status = 400;
      List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(type.office), 
          new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
      render("@addService", type, officePeople);
    }
    type.save();
    flash.success("Nuovo servizio %s inserito correttamente per la sede %s", type.description, type.office);
    activateServices(type.office.id);
  }
}
