/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import common.security.SecurityRules;
import dao.CertificationDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.GeneralSettingDao;
import dao.MonthlyCompetenceTypeDao;
import dao.OfficeDao;
import dao.OrganizationShiftTimeTableDao;
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
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.CompetenceManager;
import manager.ConsistencyManager;
import manager.ShiftOrganizationManager;
import manager.competences.ShiftTimeTableDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
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
import models.GeneralSetting;
import models.MonthlyCompetenceType;
import models.Office;
import models.OrganizationShiftTimeTable;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.TotalOvertime;
import models.User;
import models.dto.OrganizationTimeTable;
import models.dto.TimeTableDto;
import models.enumerate.CalculationType;
import models.enumerate.PaymentType;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle competenze.
 */
@With({Resecure.class})
@Slf4j
public class Competences extends Controller {

  private static final String SHIFT_TYPE_SERVICE_STEP = "sts";
  private static final String TIME_TABLE_STEP = "time";
  private static final String EXTERNAL_TIME_TABLE_STEP = "externalTime";

  @Inject
  private static CompetenceRecapFactory competenceRecapFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static PersonMonthCompetenceRecapFactory personMonthCompetenceRecapFactory;
  @Inject
  private static OfficeDao officeDao;
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
  @Inject
  private static ShiftOrganizationManager shiftOrganizationManager;
  @Inject
  private static OrganizationShiftTimeTableDao shiftTimeTableDao;
  @Inject
  private static MonthlyCompetenceTypeDao monthlyDao;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static GeneralSettingDao settingDao;

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
    if (comp.isDisabled()) {
      comp.setDisabled(false);
    } else {
      comp.setDisabled(true);
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
    if (!code.getLimitUnit().name().equals(group.getLimitUnit().name())) {
      Validation.addError("code", "L'unità di misura del limite del codice è diversa "
          + "da quella del gruppo");
    }
    if (!code.getLimitType().name().equals(group.getLimitType().name())) {
      Validation.addError("code", "Il tipo di limite del codice è diverso da quello del gruppo");
    }
    if (Validation.hasErrors()) {

      response.status = 400;
      render("@addCompetenceCodeToGroup", group);
    }
    code.setCompetenceCodeGroup(group);
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
    if (Security.getUser().get().getPerson() != null) {
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

    flash.success(String.format("Codice %s aggiunto con successo", competenceCode.getCode()));

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
      code.setCompetenceCodeGroup(group);
      code.save();
    }

    flash.success(String.format("Gruppo %s aggiunto con successo", group.getLabel()));

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
    CompetenceCodeGroup group = code.getCompetenceCodeGroup();
    group.competenceCodes.remove(code);
    group.save();
    code.setCompetenceCodeGroup(null);
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

    DateInterval monthInterval = DateUtility.getMonthInterval(year, month);

    List<Person> withoutCompetences = Lists.newArrayList();
    Map<Person, List<PersonCompetenceCodes>> mapEnabledCompetences = Maps.newHashMap();

    for (Person person : personDao.list(Optional.<String>absent(), Sets.newHashSet(office),
        false, monthInterval.getBegin(), monthInterval.getEnd(), true).list()) {
      boolean hasCompetence = false;
      for (PersonCompetenceCodes personCompetenceCode : person.getPersonCompetenceCodes()) {
        if (DateUtility
            .intervalIntersection(personCompetenceCode.periodInterval(), monthInterval) != null) {
          List<PersonCompetenceCodes> list = mapEnabledCompetences.get(person);
          if (list == null) {
            list = Lists.newArrayList();
            mapEnabledCompetences.put(person, list);
          }
          hasCompetence = true;
          list.add(personCompetenceCode);
        }
      }
      if (!hasCompetence) {
        withoutCompetences.add(person);
      }
    }

    render(office, mapEnabledCompetences, withoutCompetences, year, month);
  }

  /**
   * Crud per abilitare le competenze alla persona.
   *
   * @param personId persona
   */
  public static void updatePersonCompetence(Long personId, int year, int month) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
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
      codeListIds.add(pcc.getCompetenceCode());
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
    rules.checkIfPermitted(person.getOffice());

    LocalDate date = new LocalDate(year, month, 1);

    List<PersonCompetenceCodes> pccList = competenceCodeDao
        .listByPerson(person, Optional.fromNullable(date));
    List<CompetenceCode> codeToAdd = competenceManager.codeToSave(pccList, codeListIds);
    List<CompetenceCode> codeToRemove = competenceManager.codeToDelete(pccList, codeListIds);

    competenceManager.persistChanges(person, codeToAdd, codeToRemove, date);

    flash.success(String.format("Aggiornate con successo le competenze per %s",
        person.fullName()));
    Competences.enabledCompetences(date.getYear(),
        date.getMonthOfYear(), person.getOffice().id);

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
    Verify.verifyNotNull(user.get().getPerson());

    LocalDate today = LocalDate.now();
    if (year > today.getYear() || today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      competences(year, today.getMonthOfYear());
    }

    Person person = user.get().getPerson();

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
      codeListIds.add(pcc.getCompetenceCode());
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
   * @param year             year
   * @param month            month
   * @param officeId         officeId
   * @param competenceCodeId filtro competenceCode
   */
  public static void showCompetences(Integer year, Integer month, Long officeId,
      Long competenceCodeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //Redirect in caso di mese futuro 
    LocalDate today = LocalDate.now();
    if (today.getYear() == year && month > today.getMonthOfYear()) {
      flash.error("Impossibile accedere a situazione futura, "
          + "redirect automatico a mese attuale");
      showCompetences(year, today.getMonthOfYear(), officeId, competenceCodeId);
    }

    //Competenze con almeno un dipendente abilitato nel mese
    List<CompetenceCode> competenceCodeList = competenceDao
        .activeCompetenceCode(office, new LocalDate(year, month, 1));
    if (competenceCodeList.isEmpty()) {
      render(year, month, office, competenceCodeList);
    }

    //Il competence code selezionato
    CompetenceCode competenceCode = competenceCodeList.iterator().next();
    if (competenceCodeId != null) {
      competenceCode = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
      notFoundIfNull(competenceCode);
      if (!competenceCodeList.contains(competenceCode)) {
        competenceCode = competenceCodeList.iterator().next();
      }
    }

    // genero un controllo sul fatto che esistano servizi attivi per cui la reperibilità
    // può essere utilizzata
    boolean servicesInitialized = true;
    servicesInitialized = competenceManager.isServiceForReperibilityInitialized(office, 
        competenceCodeList);

    IWrapperCompetenceCode wrCompetenceCode = wrapperFactory.create(competenceCode);
    //FIXME: in questo compDto ci sarebbe molto da discutere, vengono create le competenze mancanti
    // al momento che l'utente visita la pagina, e questo genera un problema di concorrenza.
    CompetenceRecap compDto = competenceRecapFactory.create(office, competenceCode, year, month);

    render(year, month, office, competenceCodeList, wrCompetenceCode, compDto, servicesInitialized);

  }

  /**
   * Ritorna la form di inserimento della competenza alla persona.
   *
   * @param personId l'id della persona
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
    Office office = person.getOffice();
    Competence competence = new Competence(person, code, year, month);
    if (competence.getCompetenceCode().getCode().equals("S1")) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(competence.getPerson(),
          competence.getYear(), competence.getMonth(), true);
      render("@editCompetence", competence, psDto, office, year, month, person);
    }
    render("@editCompetence", competence, office, year, month, person);
  }

  /**
   * genera la form di inserimento per le competenze.
   *
   * @param personId l'id della persona
   * @param competenceCodeId l'id del codice di competenza
   * @param year l'anno
   * @param month il mese
   */
  public static void editCompetence(long personId, long competenceCodeId, int year, int month) {

    Person person = personDao.getPersonById(personId);
    CompetenceCode code = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
    Optional<Competence> comp = competenceDao.getCompetence(person, year, month, code);
    Competence competence = new Competence();
    if (comp.isPresent()) {
      competence = comp.get();
    }

    notFoundIfNull(code);
    notFoundIfNull(person);
    Office office = person.getOffice();
    if (code.getCode().equals("S1") || code.getCode().equals("S2") || code.getCode().equals("S3")) {
      boolean check = (Boolean) configurationManager
          .configValue(person, EpasParam.DISABLE_OVERTIME_LIMIT);
      PersonStampingRecap psDto = stampingsRecapFactory.create(person,
          year, month, true);
      render(person, code, year, month, psDto, office, competence, check);
    }

    render(person, code, year, month, office, competence);
  }

  /**
   * salva la competenza passata per parametro se è conforme ai limiti eventuali previsti per essa.
   *
   * @param competence la competenza relativa alla persona
   */
  public static void saveCompetence(@Required Integer valueApproved, @Required Competence competence) {

    notFoundIfNull(competence);

    Office office = competence.getPerson().getOffice();
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    String result = "";

    if (!Validation.hasErrors() && valueApproved != null && valueApproved > 0) {
      result = competenceManager.canAddCompetence(competence, valueApproved);
      if (!result.isEmpty()) {
        Validation.addError("valueApproved", result);
      }
    }
    if (Validation.hasErrors()) {

      response.status = 400;
      Person person = competence.getPerson();
      CompetenceCode code = competence.getCompetenceCode();
      render("@editCompetence", competence, office, person, code);
    }

    competenceManager.saveCompetence(competence, valueApproved);
    if (competence.getCompetenceCode().getCode().equalsIgnoreCase("S1")
        || competence.getCompetenceCode().getCode().equalsIgnoreCase("S2")
        || competence.getCompetenceCode().getCode().equalsIgnoreCase("S3")) {

      consistencyManager.updatePersonSituation(competence.getPerson().id,
          new LocalDate(competence.getYear(), competence.getMonth(), 1));
    }
    int month = competence.getMonth();
    int year = competence.getYear();

    flash.success("Competenza %s di %s aggiornata correttamente", 
        competence.getCompetenceCode().getCode(), 
        competence.getPerson().fullName());

    showCompetences(year, month, office.id, competence.getCompetenceCode().id);

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
    /* Recupero le info per le ore del monte ore straordinari per la sede*/
    List<TotalOvertime> totalList = competenceDao.getTotalOvertime(year, office);
    int totale = competenceManager.getTotalOvertime(totalList);
    
    /* Recupero la lista degli abilitati allo straordinario per assegnargli le ore di 
       monte ore personale (previa verifica del parametro associato)
     */
    List<Person> personList = Lists.newArrayList();
    GeneralSetting settings = settingDao.generalSetting();
    if (!settings.isEnableOvertimePerPerson()) {
      log.warn("Non abilitato il parametro per gli straordinari per persona!!");
    } else {
      CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
      Set<Office> offices = Sets.newHashSet();
      offices.add(office);
      personList = personDao
          .listForCompetence(code, Optional.absent(), offices, true, 
              LocalDate.now().withYear(year).monthOfYear().withMinimumValue()
              .dayOfMonth().withMinimumValue(), LocalDate.now(), Optional.absent()).list();
    }
    
    render(totalList, totale, year, office, personList);
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
   * Report. Esporta in formato .csv la situazione annuale degli straordinari.
   */
  public static void exportCompetences(Long officeId, int year, int month) {
    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted();
    List<CompetenceCodeGroup> list = competenceCodeDao.getAllGroups();
    boolean temporary = false;
    render(office, list, year, month, temporary);
  }

  /**
   * Esporta in formato .csv la situazione annuale degli straordinari. 
   * TODO: parametrico all'office.
   *
   * @param year anno
   */
  public static void getCompetenceGroupInYearMonth(int year, int month, Long officeId, 
      CompetenceCodeGroup group, boolean temporary) throws IOException {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    Set<Office> set = Sets.newHashSet();
    set.add(office);

    List<Person> personList = personDao
        .listForCompetenceGroup(group, set, false, new LocalDate(year, month, 1), 
            new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), temporary);

    FileInputStream inputStream = competenceManager
        .getCompetenceGroupInYearMonth(year, month, personList, group);
    renderBinary(inputStream, "competenze_per_gruppo_" + group.getLabel() 
        + DateUtility.fromIntToStringMonth(month) + year + ".csv");
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
        IWrapperPerson wrPerson = wrapperFactory.create(competence.getPerson());
        Optional<Contract> firstContract = wrPerson
            .getFirstContractInMonth(year, competence.getMonth());
        if (!firstContract.isPresent()) {
          continue;    //questo errore andrebbe segnalato, competenza senza che esista contratto
        }
        IWrapperContract wrContract = wrapperFactory.create(firstContract.get());
        if (!wrContract.isDefined()) {
          continue;    //scarto la competence.
        }
      }
      //Filtro competenza non approvata
      if (competence.getValueApproved() == 0) {
        continue;
      }

      personSet.add(competence.getPerson());

      //aggiungo la competenza alla mappa della persona
      Person person = competence.getPerson();
      Map<CompetenceCode, Integer> personCompetences = mapPersonCompetenceRecap.get(person);

      if (personCompetences == null) {
        personCompetences = Maps.newHashMap();
      }
      Integer value = personCompetences.get(competence.getCompetenceCode());
      if (value != null) {
        value = value + competence.getValueApproved();
      } else {
        value = competence.getValueApproved();
      }

      personCompetences.put(competence.getCompetenceCode(), value);

      mapPersonCompetenceRecap.put(person, personCompetences);

      //aggiungo la competenza al valore totale per la competenza
      value = totalValueAssigned.get(competence.getCompetenceCode());

      if (value != null) {
        value = value + competence.getValueApproved();
      } else {
        value = competence.getValueApproved();
      }

      totalValueAssigned.put(competence.getCompetenceCode(), value);
    }
    List<IWrapperPerson> personList = FluentIterable
        .from(personSet)
        .transform(wrapperFunctionFactory.person()).toList();

    render(personList, totalValueAssigned, mapPersonCompetenceRecap,
        office, year, onlyDefined);

  }


  /**
   * restituisce il template per il responsabile di gruppo di lavoro contenente le informazioni su
   * giorni di presenza, straordinari, ore a lavoro.
   *
   * @param year  l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void monthlyOvertime(Integer year, Integer month) {

    final User user = Security.getUser().get();
    Table<Person, String, Integer> tableFeature = null;
    LocalDate beginMonth = null;

    beginMonth = new LocalDate(year, month, 1);
    
    if (user.getPerson() == null || user.getPerson().getOffice() == null) {
      flash.error("%s è un'utenza di servizio non associata a nessuna persona, "
          + "non sono quindi presenti informazioni associate alla sua sede.", 
          user.getUsername());
      render(year, month);
    }

    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    SimpleResults<Person> simpleResults = personDao.listForCompetence(code,
        Optional.<String>absent(),
        Sets.newHashSet(user.getPerson().getOffice()),
        false,
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
        Optional.fromNullable(user.getPerson()));
    tableFeature = competenceManager.composeTableForOvertime(year, month,
        null, null, user.getPerson().getOffice(), beginMonth, simpleResults, code);

    render(tableFeature, year, month, simpleResults);

  }

  /**
   * ricalcola tutti i valori del codice di competenza a presenza mensile recuperato dall'id 
   * passato come parametro per tutti i dipendenti della sede recuperata dall'id passato 
   * come parametro per l'anno e il mese passati come parametro.
   *
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

    flash.success("Aggiornati i valori per la competenza %s", competenceCode.getCode());

    showCompetences(year, month, office.id, competenceCode.id);


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
    List<Office> linkedOffices = officeDao.byInstitute(office.getInstitute());
    rules.checkIfPermitted(office);
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(linkedOffices),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    List<MonthlyCompetenceType> types = MonthlyCompetenceType.findAll();

    render("@editReperibility", officePeople, office, types);
  }

  /**
   * Metodo che renderizza la form di inserimento di un nuovo servizio da attivare
   * per la reperibilità.
   *
   * @param officeId l'id dell'ufficio a cui associare il servizio
   */
  public static void addShift(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    List<Office> linkedOffices = Lists.newArrayList();
    rules.checkIfPermitted(office);
    if (Security.getUser().get().isSystemUser()) {
      linkedOffices = officeDao.allEnabledOffices();
    } else {
      linkedOffices = officeDao.byInstitute(office.getInstitute());
    }
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(linkedOffices),
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
      render("@editReperibility", type, officePeople, office);
    }
    type.setOffice(office);
    type.save();
    flash.success("Nuovo servizio %s inserito correttamente per la sede %s",
        type.getDescription(), type.getOffice());
    activateServices(type.getOffice().id);
  }

  /**
   * Salva il servizio di turno.
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
      Map<ShiftType, List<PersonShiftShiftType>> map = Maps.newHashMap();
      cat.getShiftTypes().forEach(item -> {
        List<PersonShiftShiftType> psstList = shiftDao
            .getAssociatedPeopleToShift(item, Optional.fromNullable(LocalDate.now()));
        map.put(item, psstList);
      });
      render("@editShift", cat, map, officePeople, office);
    }

    cat.setOffice(office);
    cat.save();


    flash.success("Nuovo servizio %s inserito correttamente per la sede %s",
        cat.getDescription(), cat.getOffice());
    activateServices(cat.getOffice().id);
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
    rules.checkIfPermitted(type.getOffice());
    if (!confirmed) {
      confirmed = true;
      render(type, confirmed);
    }
    if (type.isDisabled()) {
      type.setDisabled(false);
      type.save();
      flash.success("Riabilitato servizio %s", type.getDescription());
      activateServices(type.getOffice().id);
    }

    log.debug("Reperibility type = {}, personReperibilities.size = {}, "
        + "personReperibilitiesDays.size = {}", 
        type, type.getPersonReperibilities().size(), 
        type.getPersonReperibilities().stream()
        .flatMap(pr -> pr.getPersonReperibilityDays().stream())
        .collect(Collectors.toList()).size());
    
    if (!type.getPersonReperibilities().isEmpty() 
        && !type.getPersonReperibilities().stream()
        .flatMap(pr -> pr.getPersonReperibilityDays().stream())
        .collect(Collectors.toList()).isEmpty()) {
      type.setDisabled(true);
      type.save();
      log.info("Disabilitato servizio {}", type);
      flash.success("Il servizio è stato disabilitato e non rimosso perchè legato con informazioni "
          + "importanti presenti in altre tabelle");

    } else {
      type.getPersonReperibilities().stream().forEach(pr -> pr.delete());
      type.delete();
      log.info("Rimosso servizio {}", type);
      flash.success("Servizio rimosso con successo");
    }
    activateServices(type.getOffice().id);
  }

  /**
   * metodo che ritorna la form di inserimento/modifica di un servizio.
   *
   * @param reperibilityTypeId l'id del servizio da editare
   */
  public static void editReperibility(Long reperibilityTypeId) {
    PersonReperibilityType type = reperibilityDao.getPersonReperibilityTypeById(reperibilityTypeId);
    Office office = type.getOffice();
    List<Office> linkedOffices = Lists.newArrayList();
    rules.checkIfPermitted(office);
    if (Security.getUser().get().isSystemUser()) {
      linkedOffices = officeDao.allEnabledOffices();
    } else {
      linkedOffices = officeDao.byInstitute(office.getInstitute());
    }   
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(linkedOffices),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    //TODO: caricare la lista delle competenze mensili
    List<MonthlyCompetenceType> types = monthlyDao.listTypes();

    render(type, officePeople, office, types);
  }


  /**
   * metodo che controlla e poi persiste la disabilitazione/abilitazione di un servizio.
   *
   * @param shiftCategoryId l'id del servizio da disabilitare/abilitare
   * @param confirmed il booleano per consentire la persistenza di una modifica
   */
  public static void evaluateShift(Long shiftCategoryId, boolean confirmed) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.getOffice());
    if (!confirmed) {
      confirmed = true;
      render(cat, confirmed);
    }
    if (cat.isDisabled()) {
      cat.setDisabled(false);
      cat.save();
      flash.success("Riabilitato servizio %s", cat.getDescription());
      activateServices(cat.getOffice().id);
    }
    List<ShiftType> shiftTypeList = shiftDao.getTypesByCategory(cat);
    List<ShiftType> toDelete = Lists.newArrayList();
    for (ShiftType type : shiftTypeList) {
      if (!type.getMonthsStatus().isEmpty()) {
        long count = type.getMonthsStatus().stream().filter(st -> st.isApproved()).count();
        if (count > 0) {
          cat.setDisabled(true);
          cat.save();          
        } else {
          toDelete.add(type);
          log.debug("Elinino i person_shift_days relativi a {}", type.getType());
          for (PersonShiftDay psd : type.getPersonShiftDays()) {
            psd.delete();
          }
          type.getMonthsStatus().stream().map(st -> st.delete());  
          log.debug("Elimino i gli shift_type_month relativi a {}", type.getType());
        }        
      } else {
        toDelete.add(type);
      }
      if (toDelete.contains(type)) {
        log.debug("Elimino le relazioni tra persone e shift_type");        
        for (PersonShiftShiftType psst : type.getPersonShiftShiftTypes()) {
          psst.delete();
        }
        log.debug("Elimino lo shift_type {}", type.getType());
        type.delete();
      }

    }
    if (toDelete.size() == shiftTypeList.size()) {
      cat.delete();
      flash.success("Servizio rimosso con successo");
    } else {
      flash.success("Il servizio è stato disabilitato e non rimosso perchè legato con informazioni "
          + "importanti presenti in altre tabelle");
    }   

    activateServices(cat.getOffice().id);
  }

  /**
   * metodo che ritorna la form di inserimento/modifica di un servizio.
   *
   * @param shiftCategoryId l'id del servizio da editare
   */
  public static void editShift(Long shiftCategoryId) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    List<Office> linkedOffices = Lists.newArrayList();
    Office office = cat.getOffice();    
    rules.checkIfPermitted(office);
    if (Security.getUser().get().isSystemUser()) {
      linkedOffices = officeDao.allEnabledOffices();
    } else {
      linkedOffices = officeDao.byInstitute(office.getInstitute());
    }    
    Map<ShiftType, List<PersonShiftShiftType>> map = Maps.newHashMap();
    List<Person> officePeople = personDao.getActivePersonInMonth(Sets.newHashSet(linkedOffices),
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    cat.getShiftTypes().forEach(item -> {
      List<PersonShiftShiftType> psstList = shiftDao
          .getAssociatedPeopleToShift(item, Optional.fromNullable(LocalDate.now()));
      map.put(item, psstList);
    });
    boolean nuovo = false;
    render(cat, officePeople, office, map, nuovo);
  }


  /**
   * metodo che ritorna la form di creazione di una nuova timetable.
   */
  public static void configureShiftTimeTable(int step, int slot, 
      boolean considerEverySlot, String name,
      CalculationType calculationType, Long officeId, List<OrganizationTimeTable> list) {
    if (step == 0) {
      step++;
      List<Office> officeList = officeDao.getAllOffices();    
      List<CalculationType> calculationTypes = Arrays.asList(CalculationType.values());
      render(officeList, calculationTypes, step);
    }

    if (step == 1) {
      step++;      
      slot = 2;
      render(list, step, officeId, calculationType, slot, name);
    }

    if (step == 2) {
      if (slot < 2) {
        Validation.addError("slot", "Non si può definire un turno con meno di due slot");
        render(list, step, officeId, calculationType, slot, name);
      }
      list = Lists.newArrayList();
      for (int i = 0; i < slot; i++) {
        OrganizationTimeTable ott = new OrganizationTimeTable();
        ott.isMealActive = false;
        list.add(ott);
      }
      List<PaymentType> paymentTypes = Arrays.asList(PaymentType.values());
      step++;
      render(officeId, calculationType, slot, step, list, name, paymentTypes, considerEverySlot);
    }
    Office office = officeDao.getOfficeById(officeId);
    if (office == null) {
      flash.error("La sede selezionata con id %s non esiste!", officeId);
      render();
    }
    String result = shiftOrganizationManager
        .generateTimeTableAndSlot(list, office, calculationType, name, considerEverySlot);
    if (Strings.isNullOrEmpty(result)) {
      flash.success("Inserita nuova timetable");
    } else {
      flash.error("Errore nella creazione della timetable: %s", result);
    }

    manageCompetenceCode();
  }


  /**
   * metodo che ritorna al template le informazioni per poter configurare correttamente il turno.
   *
   * @param shiftCategoryId l'id del servzio da configurare
   */
  @SuppressWarnings("unchecked")
  public static void configureShift(Long shiftCategoryId, int step, Long organizationShift,  
      @Valid ShiftType type, Long shift, boolean breakInRange, boolean enableExitTolerance) {
    ShiftCategories cat = shiftDao.getShiftCategoryById(shiftCategoryId);
    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.getOffice());
    final String key = SHIFT_TYPE_SERVICE_STEP 
        + cat.getDescription() + Security.getUser().get().getUsername();
    final String internal = TIME_TABLE_STEP 
        + cat.getDescription() + Security.getUser().get().getUsername();
    final String external = EXTERNAL_TIME_TABLE_STEP 
        + cat.getDescription() + Security.getUser().get().getUsername();
    if (step == 0) {
      // ritorno il dto per creare l'attività
      breakInRange = false;
      List<ShiftTimeTableDto>  dtoList = competenceManager
          .convertFromShiftTimeTable(shiftDao.getAllShifts(cat.getOffice()));
      List<OrganizationShiftTimeTable> timeTableList = 
          shiftTimeTableDao.getAllFromOffice(cat.getOffice());
      step++;
      render(dtoList, cat, type, step, breakInRange, enableExitTolerance, timeTableList);
    }
    if (step == 1) {

      if (shift == null && organizationShift == null) {
        flash.error("selezionare una timetable!");
        List<ShiftTimeTable> shiftList = shiftDao.getAllShifts(cat.getOffice());        
        List<ShiftTimeTableDto>  dtoList = competenceManager.convertFromShiftTimeTable(shiftList);

        render("@configureShift", dtoList, cat, type, step, 
            breakInRange, enableExitTolerance);
      }

      List<ShiftTimeTable> internalList = Lists.newArrayList();
      List<OrganizationShiftTimeTable> externalList = Lists.newArrayList();

      if (shift != null) {
        ShiftTimeTable stt = shiftDao.getShiftTimeTableById(shift);
        internalList.add(stt);
        Cache.safeAdd(internal, internalList, "10mn");
        if (Range.closed(stt.getStartMorning(), stt.getEndMorning())
            .encloses(Range.closed(stt.getStartMorningLunchTime(), stt.getEndMorningLunchTime()))) {
          //ritornare un'informazione per far visualizzare diversamente la costruzione della form
          breakInRange = true;
        }
      } else {
        Optional<OrganizationShiftTimeTable> ostt = shiftTimeTableDao.getById(organizationShift);
        if (ostt.isPresent()) {
          externalList.add(ostt.get());
          Cache.safeAdd(external, externalList, "10mn");     
        }        
      }  

      step++;      
      enableExitTolerance = false;
      render(step, type, cat, breakInRange, enableExitTolerance);

    }
    if (step == 2) {

      if (type.getBreakInShift() > type.getBreakMaxInShift() 
          || type.getEntranceTolerance() > type.getEntranceMaxTolerance() 
          || type.getExitTolerance() > type.getExitMaxTolerance()) {
        flash.error("Le soglie minime non possono essere superiori a quelle massime");

        render("@configureShift", step, cat, type, breakInRange, enableExitTolerance);
      }
      step++;
      //metto in cache la struttura dell'attività e ritorno il dto per creare la timetable
      List<ShiftType> list = Lists.newArrayList();
      list.add(type);

      Cache.safeAdd(key, list, "10mn");
      List<OrganizationShiftTimeTable> externalTimeTable = Cache.get(external, ArrayList.class);
      List<ShiftTimeTable> internalTimeTable = Cache.get(internal, List.class);
      if (internalTimeTable == null && externalTimeTable == null) {
        flash.error("Scaduta sessione di creazione dell'attività di turno.");
        step = 0;
        render(cat, type, step, breakInRange);
      }

      if (internalTimeTable != null) {
        ShiftTimeTable stt = internalTimeTable.get(0);
        Cache.safeAdd(internal, internalTimeTable, "10mn");
        render(cat, type, step, stt, breakInRange);
      } else {
        OrganizationShiftTimeTable ostt = externalTimeTable.get(0);
        Cache.safeAdd(external, externalTimeTable, "10mn");
        render(cat, type, step, ostt, breakInRange);
      }
    }
    if (step == 3) {
      //effettuo la creazione dell'attività 
      List<ShiftType> list = Cache.get(key, List.class);      
      List<ShiftTimeTable> internalList = Cache.get(internal, List.class);
      List<OrganizationShiftTimeTable> externalList = Cache.get(external, List.class);
      if (list == null && internalList == null && externalList == null) {
        flash.error("Scaduta sessione di creazione dell'attività di turno.");
        step = 0;
        List<ShiftTimeTableDto>  dtoList = competenceManager
            .convertFromShiftTimeTable(shiftDao.getAllShifts(cat.getOffice()));
        List<OrganizationShiftTimeTable> timeTableList = 
            shiftTimeTableDao.getAllFromOffice(cat.getOffice());
        render(cat, type, step, breakInRange, enableExitTolerance, dtoList, timeTableList);
      }
      ShiftType service = list.get(0);
      ShiftTimeTable stt = null;
      OrganizationShiftTimeTable ostt = null;
      if (internalList != null) {
        stt = internalList.get(0);
        competenceManager.persistShiftType(service, Optional.fromNullable(stt), 
            Optional.<OrganizationShiftTimeTable>absent(), cat);
      } else {
        ostt = externalList.get(0);
        competenceManager.persistShiftType(service, Optional.<ShiftTimeTable>absent(), 
            Optional.fromNullable(ostt), cat);
      }

      Cache.clear();
      flash.success("Attività salvata correttamente!");
      activateServices(cat.getOffice().id);
    }

  }

  /**
   * metodo che ritorna al template le informazioni sull'attività passata come parametro.
   *
   * @param shiftTypeId l'id dell'attività da configurare
   */
  public static void manageShiftType(Long shiftTypeId) {
    Optional<ShiftType> shiftType = shiftDao.getShiftTypeById(shiftTypeId);

    if (!shiftType.isPresent()) {
      flash.error("Si cerca di caricare un'attività inesistente! Verificare l'id");
      activateServices(Long.parseLong((session.get("officeSelected"))));
    } else {
      rules.checkIfPermitted(shiftType.get().getShiftCategories().getOffice());
      ShiftType type = shiftType.get();
      Office office = officeDao.getOfficeById(type.getShiftCategories().getOffice().id);
      List<PersonShift> peopleForShift = shiftDao.getPeopleForShift(office, LocalDate.now());

      List<PersonShiftShiftType> associatedPeopleShift = shiftDao
          .getAssociatedPeopleToShift(type, Optional.fromNullable(LocalDate.now()));
      LocalDate date = LocalDate.now();
      render(type, date, office, peopleForShift, associatedPeopleShift);
    }
  }

  /**
   * Ritorna la form di associazione tra persona e turno.
   *
   * @param id identificativo dell'associazione tra persona e turno
   */
  public static void handlePersonShiftShiftType(Long id) {
    PersonShiftShiftType psst = shiftDao.getById(id);
    notFoundIfNull(psst);
    rules.checkIfPermitted(psst.getPersonShift().getPerson().getOffice());    
    render(psst);
  }

  /**
   * metodo per il salvataggio dell'associazione.
   *
   * @param psst l'oggetto associazione tra persona e turno.
   */
  public static void updatePersonShiftShiftType(PersonShiftShiftType psst) {
    rules.checkIfPermitted(psst.getPersonShift().getPerson().getOffice());
    psst.save();
    flash.success("Informazioni salvate correttamente");
    manageShiftType(psst.getShiftType().id);
  }

  /**
   * modifica i parametri dell'attività passata tramite id.
   *
   * @param type l'attività di cui si vogliono modificare i parametri
   */
  public static void editActivity(ShiftType type, boolean considerEverySlot) {
    if (type.getOrganizaionShiftTimeTable() != null) {
      OrganizationShiftTimeTable ott = type.getOrganizaionShiftTimeTable();
      ott.setConsiderEverySlot(considerEverySlot);
      ott.save();
    }
    type.save();
    flash.success("Modificati parametri per l'attività: %s", type.getDescription());
    manageShiftType(type.id);
  }

  /**
   * Elimina l'attività di turno passata come parametro.
   *
   * @param type l'attovità di turno
   */
  public static void deleteActivity(ShiftType type) {

    rules.checkIfPermitted(type.getShiftCategories().getOffice());
    if (!type.getPersonShiftDays().isEmpty()) {
      flash.error("L'attività %s contiene dei giorni di turno configurati nei mesi precedenti. "
          + "Eliminare le giornate di turno prima di provvedere all'eliminazione dell'attività", 
          type.getType());
      manageShiftType(type.id);
    }
    if (!type.getPersonShiftShiftTypes().isEmpty()) {
      log.debug("L'attività {} è ancora referenziata a qualche persona");
      type.getPersonShiftShiftTypes().stream().forEach(psst -> psst.delete());

    }
    type.delete();
    log.info("Eliminata attività {}", type.getType());
    flash.success("Attività %s eliminata", type.getType());
    activateServices(type.getShiftCategories().getOffice().id);

  }


  /**
   * ritorna la form di inserimento del personale da assegnare all'attività passata come parametro.
   *
   * @param typeId l'id della attività a cui assegnare personale
   */
  public static void linkPeopleToShift(Long typeId) {
    Optional<ShiftType> type = shiftDao.getShiftTypeById(typeId);
    if (!type.isPresent()) {
      flash.error("Attività non presente. Verificare l'identificativo");
      activateServices(Long.parseLong((session.get("officeSelected"))));
    }

    rules.checkIfPermitted(type.get().getShiftCategories().getOffice());
    if (Validation.hasErrors()) {
      response.status = 400;
      List<PersonShift> peopleForShift = 
          shiftDao.getPeopleForShift(type.get().getShiftCategories().getOffice(), LocalDate.now());
      Office office = type.get().getShiftCategories().getOffice();     
      render("@manageShiftType", type, peopleForShift, office);
    }
    type.get().save();
    List<PersonShiftShiftType> psstList = shiftDao.getAssociatedPeopleToShift(type.get(), 
        Optional.fromNullable(LocalDate.now()));
    List<PersonShift> peopleForShift = 
        shiftDao.getPeopleForShift(type.get().getShiftCategories().getOffice(), LocalDate.now());

    List<PersonShift> available = peopleForShift.stream()
        .filter(e -> (psstList.stream()
            .filter(d -> d.getPersonShift().equals(e))
            .count()) < 1)
        .collect(Collectors.toList());
    ShiftType activity = type.get();
    render(available, activity);
  }


  /**
   * metodo che associa la persona all'attività.
   *
   * @param person la persona in turno che deve prendere parte a un'attività
   * @param activity l'attività in turno in cui inserire la persona
   * @param beginDate la data di inizio partecipazione della persona all'attività in turno
   * @param jolly true se la persona può partecipare ai diversi turni in attività 
   *     (mattina e pomeriggio di solito), false altrimenti
   */
  public static void saveActivityConfiguration(PersonShift person, ShiftType activity, 
      LocalDate beginDate, boolean jolly) {
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getPerson().getOffice());
    if (beginDate == null) {
      Validation.addError("beginDate", "inserire una data di inizio!");
    }

    if (!person.isPersistent()) {
      Validation.addError("person", "selezionare una persona!");
    }
    if (Validation.hasErrors()) {
      List<PersonShiftShiftType> psstList = shiftDao.getAssociatedPeopleToShift(activity, 
          Optional.fromNullable(LocalDate.now()));
      List<PersonShift> peopleForShift = 
          shiftDao.getPeopleForShift(activity.getShiftCategories().getOffice(), LocalDate.now());

      List<PersonShift> available = peopleForShift.stream()
          .filter(e -> (psstList.stream()
              .filter(d -> d.getPersonShift().equals(e))
              .count()) < 1)
          .collect(Collectors.toList());
      response.status = 400;
      render("@linkPeopleToShift", activity, available);
    }
    competenceManager.persistPersonShiftShiftType(person, beginDate, activity, jolly);
    flash.success("Aggiunto %s all'attività", person.getPerson().fullName());
    manageShiftType(activity.id);
  }

  /**
   * rimuove una persona da una attività applicando la data di terminazione al periodo.
   *
   * @param personShiftShiftTypeId l'id del personShiftShiftType da eliminare
   * @param confirmed booleano che determina se siamo alla prima chiamata del metodo o 
   *     alla conferma della rimozione
   */
  public static void deletePersonShiftShiftType(Long personShiftShiftTypeId, 
      @Valid LocalDate endDate, boolean confirmed) {
    final PersonShiftShiftType psst = shiftDao.getById(personShiftShiftTypeId);
    notFoundIfNull(psst);
    rules.checkIfPermitted(psst.getShiftType().getShiftCategories().getOffice());
    if (!confirmed) {
      confirmed = true;
      render("@deletePersonShiftShiftType", psst, confirmed);
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@deletePersonShiftShiftType", psst, confirmed);
    }
    psst.setEndDate(endDate);
    psst.save();

    flash.success("Terminata esperienza per %s nell'attività %s in data %s", 
        psst.getPersonShift().getPerson().fullName(), 
        psst.getShiftType().getDescription(), psst.getEndDate());
    manageShiftType(psst.getShiftType().id);
  }

  /**
   * assegna la timetable al turno.
   *
   * @param shift l'id del turno
   * @param cat il servizio a cui associare il turno
   * @param type l'attività di turno
   */
  public static void linkTimeTableToShift(Long shift, ShiftCategories cat, @Valid ShiftType type) {

    notFoundIfNull(cat);
    rules.checkIfPermitted(cat.getOffice());
    ShiftTimeTable timeTable = shiftDao.getShiftTimeTableById(shift);
    notFoundIfNull(timeTable);  

    if (Validation.hasErrors()) {
      response.status = 400;
      List<ShiftTimeTable> shiftList = shiftDao.getAllShifts(cat.getOffice());
      List<ShiftTimeTableDto> dtoList = competenceManager.convertFromShiftTimeTable(shiftList);
      render("@configureShift", shiftList, dtoList, cat, type);

    }
    type.setShiftCategories(cat);
    type.setShiftTimeTable(timeTable);
    type.save();

    flash.success("Configurato correttamente il servizio %s", cat.getDescription());
    activateServices(cat.getOffice().id);

  }

  /**
   * genera la form di assegnamento delle persone al servizio di reperibilità.
   *
   * @param reperibilityTypeId l'id del servizio di reperibilità
   */
  public static void manageReperibility(Long reperibilityTypeId) {
    PersonReperibilityType type = reperibilityDao.getPersonReperibilityTypeById(reperibilityTypeId);
    notFoundIfNull(type);
    rules.checkIfPermitted(type.getOffice());
    List<CompetenceCode> codesList = type.getMonthlyCompetenceType().getCodesForActivity();
    List<PersonCompetenceCodes> people = competenceCodeDao
        .listByCodesAndOffice(codesList, type.getOffice(), Optional.fromNullable(LocalDate.now()));
    List<PersonReperibility> linkedPeople = type.getPersonReperibilities().stream()
        .filter(pr -> pr.getStartDate() != null 
        && (pr.getEndDate() == null || pr.getEndDate().isAfter(LocalDate.now())))
        .collect(Collectors.toList());
    LocalDate date = LocalDate.now();
    render(people, type, date, linkedPeople);
  }

  /**
   * ritorna la form di gestione del personale afferente all'attività di reperibilità.
   *
   * @param reperibilityTypeId l'id dell'attività di reperibilità
   */
  public static void linkPeopleToReperibility(Long reperibilityTypeId) {
    PersonReperibilityType type = reperibilityDao.getPersonReperibilityTypeById(reperibilityTypeId);
    if (type == null) {
      flash.error("Attività non presente. Verificare l'identificativo");
      activateServices(Long.parseLong((session.get("officeSelected"))));
    }

    rules.checkIfPermitted(type.getOffice());

    List<CompetenceCode> codeList = type.getMonthlyCompetenceType().getCodesForActivity();
    List<PersonCompetenceCodes> people = competenceCodeDao
        .listByCodesAndOffice(codeList, type.getOffice(), Optional.fromNullable(LocalDate.now()));
    List<PersonReperibility> linkedPeople = type.getPersonReperibilities().stream()
        .filter(pr -> pr.getStartDate() != null 
        && (pr.getEndDate() == null || pr.getEndDate().isAfter(LocalDate.now())))
        .collect(Collectors.toList());

    if (Validation.hasErrors()) {

      response.status = 400;     
      LocalDate date = LocalDate.now();
      render("@manageReperibility", type, date, people, linkedPeople);
    }
    type.save();

    List<Person> available = people
        .stream().filter(e -> (linkedPeople.stream()
            .noneMatch(d -> d.getPerson().equals(e.getPerson()))))        
        .map(pcc -> pcc.getPerson()).distinct()
        .filter(p -> p.getOffice().equals(type.getOffice())).collect(Collectors.toList());

    render(available, type);
  }

  /**
   * impone una data di terminazione nell'associazione tra persona e attività.
   *
   * @param personReperibilityId l'id della persona associata all'attività
   * @param endDate data di fine esperienza
   * @param confirmed true se confermato, false se siamo alla prima fase di accesso al metodo
   */
  public static void deletePersonReperibility(Long personReperibilityId,
      @Valid LocalDate endDate, boolean confirmed) {
    final Optional<PersonReperibility> personReperibility = 
        reperibilityDao.getPersonReperibilityById(personReperibilityId);
    notFoundIfNull(personReperibility.get());
    rules.checkIfPermitted(personReperibility.get().getPersonReperibilityType().getOffice());
    PersonReperibility per = personReperibility.get();
    if (!confirmed) {
      confirmed = true;
      render("@deletePersonReperibility", per, confirmed);
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@deletePersonReperibility", per, confirmed);
    }
    per.setEndDate(endDate);
    per.save();

    flash.success("Terminata esperienza per %s nell'attività %s in data %s", 
        per.getPerson().fullName(), per.getPersonReperibilityType().getDescription(), 
        per.getEndDate());
    manageReperibility(per.getPersonReperibilityType().id);
  }

  /**
   * salva l'associazione persona-attività di reperibilità.
   *
   * @param type l'attività di reperibilità
   * @param person la persona da associare all'attività in reperibilità
   * @param beginDate la data di inizio appartenenza all'attività
   */
  public static void saveReperibilityConfiguration(PersonReperibilityType type, 
      Person person, LocalDate beginDate) {
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    notFoundIfNull(type);
    if (beginDate == null) {
      Validation.addError("beginDate", "inserire una data di inizio!");
    }
    if (!person.isPersistent()) {
      Validation.addError("person", "selezionare una persona!");
    }
    if (Validation.hasErrors()) {
      List<PersonReperibility> personAssociated = 
          reperibilityDao.byOffice(type.getOffice(), LocalDate.now());
      List<CompetenceCode> codeList = Lists.newArrayList();
      codeList.add(competenceCodeDao.getCompetenceCodeByCode("207"));
      codeList.add(competenceCodeDao.getCompetenceCodeByCode("208"));
      List<Person> available = competenceCodeDao
          .listByCodesAndOffice(codeList, type.getOffice(), Optional.fromNullable(LocalDate.now()))
          .stream().filter(e -> (personAssociated.stream()
              .noneMatch(d -> d.getPerson().equals(e.getPerson()))))        
          .map(pcc -> pcc.getPerson()).distinct()
          .filter(p -> p.getOffice().equals(type.getOffice())).collect(Collectors.toList());
      response.status = 400;
      render("@linkPeopleToReperibility", type, available);
    }
    competenceManager.persistPersonReperibilityType(person, beginDate, type);
    flash.success("Aggiunto %s all'attività", person.fullName());
    manageReperibility(type.id);
  }
}