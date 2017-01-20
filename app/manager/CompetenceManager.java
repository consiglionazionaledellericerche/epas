package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import helpers.jpa.ModelQuery.SimpleResults;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.competences.CompetenceCodeDTO;
import manager.competences.ShiftTimeTableDto;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftShiftType;
import models.ShiftTimeTable;
import models.ShiftType;
import models.TotalOvertime;
import models.enumerate.LimitUnit;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.i18n.Messages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class CompetenceManager {


  private static final Logger log = LoggerFactory.getLogger(CompetenceManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final OfficeDao officeDao;
  private final PersonDayDao personDayDao;
  private final CompetenceDao competenceDao;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayManager personDayManager;
  private final PersonReperibilityDayDao reperibilityDao;
  private final PersonStampingRecapFactory stampingsRecapFactory;
  private final PersonShiftDayDao personShiftDayDao;
  private final ShiftDao shiftDao;

  /**
   * Costruttore.
   *
   * @param competenceCodeDao competenceCodeDao
   * @param officeDao         officeDao
   * @param competenceDao     competenceDao
   * @param personDayDao      personDayDao
   * @param wrapperFactory    wrapperFactory
   * @param personDayManager  personDayManager
   */
  @Inject
  public CompetenceManager(CompetenceCodeDao competenceCodeDao,
      OfficeDao officeDao, CompetenceDao competenceDao,
      PersonDayDao personDayDao, IWrapperFactory wrapperFactory,
      PersonDayManager personDayManager, PersonReperibilityDayDao reperibilityDao,
      PersonStampingRecapFactory stampingsRecapFactory, PersonShiftDayDao personshiftDayDao,
      ShiftDao shiftDao) {
    this.competenceCodeDao = competenceCodeDao;
    this.officeDao = officeDao;
    this.competenceDao = competenceDao;
    this.personDayDao = personDayDao;
    this.wrapperFactory = wrapperFactory;
    this.personDayManager = personDayManager;
    this.reperibilityDao = reperibilityDao;

    this.stampingsRecapFactory = stampingsRecapFactory;   
    this.personShiftDayDao = personshiftDayDao;
    this.shiftDao = shiftDao;



  }

  public static Predicate<CompetenceCode> isReperibility() {
    return p -> p.code.equalsIgnoreCase("207") || p.code.equalsIgnoreCase("208");
  }


  /**
   * @return la lista di stringhe popolata con i codici dei vari tipi di straordinario prendibili.
   */
  public List<String> populateListWithOvertimeCodes() {
    List<String> list = Lists.newArrayList();
    list.add("S1");
    list.add("S2");
    list.add("S3");
    return list;
  }

  /**
   * @return il quantitativo di straordinari totali.
   */
  public Integer getTotalOvertime(List<TotalOvertime> total) {
    Integer totaleMonteOre = 0;
    for (TotalOvertime tot : total) {
      totaleMonteOre = totaleMonteOre + tot.numberOfHours;
    }
    return totaleMonteOre;
  }

  /**
   * @return il quantitativo su base annuale di straordinari.
   */
  public int getTotalYearlyOvertime(List<Competence> competenceYearList) {
    int totaleOreStraordinarioAnnuale = 0;
    for (Competence comp : competenceYearList) {

      totaleOreStraordinarioAnnuale = totaleOreStraordinarioAnnuale + comp.valueApproved;
    }
    return totaleOreStraordinarioAnnuale;
  }

  /**
   * @return il quantitativo su base mensile di straordinari.
   */
  public int getTotalMonthlyOvertime(List<Competence> competenceMonthList) {
    int totaleOreStraordinarioMensile = 0;
    for (Competence comp : competenceMonthList) {

      totaleOreStraordinarioMensile = totaleOreStraordinarioMensile + comp.valueApproved;
    }
    return totaleOreStraordinarioMensile;
  }

  /**
   * Salva gli straordinari.
   *
   * @param year      anno
   * @param numeroOre numeroOre
   * @param officeId  sede
   * @return esito
   */
  public boolean saveOvertime(Integer year, String numeroOre, Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    TotalOvertime total = new TotalOvertime();
    LocalDate data = new LocalDate();
    total.date = data;
    total.year = data.getYear();
    total.office = office;

    try {
      if (numeroOre.startsWith("-")) {

        total.numberOfHours = -new Integer(numeroOre.substring(1, numeroOre.length()));
      } else if (numeroOre.startsWith("+")) {

        total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
      } else {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }
    total.save();
    return true;

  }

  /**
   * @return la tabella formata da persone, dato e valore intero relativi ai quantitativi orari su
   * orario di lavoro, straordinario, riposi compensativi per l'anno year e il mese month per le
   * persone dell'ufficio office.
   */
  public Table<Person, String, Integer> composeTableForOvertime(
      int year, int month, Integer page,
      String name, Office office, LocalDate beginMonth,
      SimpleResults<Person> simpleResults, CompetenceCode code) {

    ImmutableTable.Builder<Person, String, Integer> builder = ImmutableTable.builder();
    Table<Person, String, Integer> tableFeature = null;
    List<Person> activePersons = simpleResults.list();

    for (Person p : activePersons) {
      Integer daysAtWork = 0;
      Integer timeAtWork = 0;
      Integer difference = 0;
      Integer overtime = 0;

      List<PersonDay> personDayList = personDayDao.getPersonDayInPeriod(p,
          beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()));
      for (PersonDay pd : personDayList) {
        if (pd.stampings.size() > 0) {
          daysAtWork = daysAtWork + 1;
        }
        timeAtWork = timeAtWork + pd.timeAtWork;
        difference = difference + pd.difference;
      }
      Optional<Competence> comp = competenceDao
          .getCompetence(p, year, month, code);
      if (comp.isPresent()) {
        overtime = comp.get().valueApproved;
      } else {
        overtime = 0;
      }
      builder.put(p, "Giorni di Presenza", daysAtWork);
      builder.put(p, "Tempo Lavorato (HH:MM)", timeAtWork);
      builder.put(p, "Tempo di lavoro in eccesso (HH:MM)", difference);
      builder.put(p, "Ore straordinario pagate", overtime);


    }
    tableFeature = builder.build();
    return tableFeature;

  }

  /**
   * @return true se avviene correttamente il cambiamento della lista di competenze attive per la
   * persona Person passata come parametro.
   */
  public boolean saveNewCompetenceEnabledConfiguration(
      Map<String, Boolean> competence,
      List<CompetenceCode> competenceCode, Person person) {
    for (CompetenceCode code : competenceCode) {
      boolean value = false;
      if (competence.containsKey(code.code)) {
        value = competence.get(code.code);
        log.info("competence {} is {}", code.code, value);
      }

    }
    person.save();
    return true;
  }

  /**
   * @return il file contenente tutti gli straordinari effettuati dalle persone presenti nella lista
   * personList nell'anno year.
   */
  public FileInputStream getOvertimeInYear(int year, List<Person> personList) throws IOException {
    FileInputStream inputStream = null;
    File tempFile = File.createTempFile("straordinari" + year, ".csv");
    inputStream = new FileInputStream(tempFile);
    FileWriter writer = new FileWriter(tempFile, true);
    BufferedWriter out = new BufferedWriter(writer);
    out.write("Cognome Nome,Totale straordinari" + ' ' + year);
    out.newLine();
    List<CompetenceCode> codeList = Lists.newArrayList();
    codeList.add(competenceCodeDao.getCompetenceCodeByCode("S1"));
    for (Person p : personList) {
      Long totale = null;
      Optional<Integer> result =
          competenceDao.valueOvertimeApprovedByMonthAndYear(
              year, Optional.<Integer>absent(), Optional.fromNullable(p), codeList);
      if (result.isPresent()) {
        totale = result.get().longValue();
      }

      log.debug("Totale per {} vale %d", p.getFullname(), totale);
      out.write(p.surname + ' ' + p.name + ',');
      if (totale != null) {
        out.append(totale.toString());
      } else {
        out.append("0");
      }
      out.newLine();
    }
    out.close();
    return inputStream;
  }

  /**
   * Ritorna il numero di ore disponibili per straordinari per la persona nel mese. Calcola il
   * residuo positivo del mese per straordinari inerente il contratto attivo nel mese. Nel caso di
   * due contratti attivi nel mese viene ritornato il valore per il contratto più recente. Nel caso
   * di nessun contratto attivo nel mese viene ritornato il valore 0.
   */
  public Integer positiveResidualInMonth(Person person, int year, int month) {

    List<Contract> monthContracts = wrapperFactory
        .create(person).orderedMonthContracts(year, month);
    int differenceForShift = 0;
    List<PersonDay> pdList = personDayDao.getPersonDayInMonth(person, new YearMonth(year, month));
    for (Contract contract : monthContracts) {

      IWrapperContract wrContract = wrapperFactory.create(contract);

      if (wrContract.isLastInMonth(month, year)) {

        Optional<ContractMonthRecap> recap =
            wrContract.getContractMonthRecap(new YearMonth(year, month));
        if (recap.isPresent()) {
          /**
           * FIXME: in realtà bisogna controllare che la persona nell'arco
           * del mese non sia stata in turno. In quel caso nei giorni
           * in cui la persona è in turno e fa un tempo di lavoro
           * superiore al tempo per i turni, tutto l'eccesso non deve essere
           * conteggiato nel computo del tempo disponibile per straordinari
           */
          for (PersonDay pd : pdList) {
            differenceForShift = differenceForShift + personDayManager.getExceedInShift(pd);
          }
          return recap.get().getPositiveResidualInMonth() - differenceForShift;
        }
      }
    }
    return 0;
  }

  /**
   * La lista dei codici competenza attivi per le persone nell'anno.
   */
  public List<CompetenceCode> activeCompetence(int year) {

    List<CompetenceCode> competenceCodeList = Lists.newArrayList();

    List<Competence> competenceList =
        competenceDao.getCompetenceInYear(year, Optional.<Office>absent());

    for (Competence comp : competenceList) {
      if (!competenceCodeList.contains(comp.competenceCode)) {
        competenceCodeList.add(comp.competenceCode);
      }
    }
    return competenceCodeList;
  }

  /**
   * @param comp  la competenza da aggiornare
   * @param value il quantitativo per quella competenza da aggiornare
   * @return La stringa contenente il messaggio da far visualizzare come errore, se riscontrato.
   * Stringa vuota altrimenti.
   */
  public String canAddCompetence(Competence comp, Integer value) {

    String result = "";
    if (!isCompetenceEnabled(comp)) {
      result = Messages.get("CompManager.notEnabled");
      return result;
    }
    List<CompetenceCode> group = Lists.newArrayList();
    List<Competence> compList = Lists.newArrayList();
    int sum = 0;
    switch (comp.competenceCode.limitType) {
      case monthly:
        group = competenceCodeDao
        .getCodeWithGroup(comp.competenceCode.competenceCodeGroup,
            Optional.fromNullable(comp.competenceCode));
        compList = competenceDao
            .getCompetences(Optional.fromNullable(comp.person), comp.year,
                Optional.fromNullable(comp.month), group);
        sum = compList.stream().mapToInt(i -> i.valueApproved).sum();
        //Caso Reperibilità:
        if (StringUtils.containsIgnoreCase(comp.competenceCode.competenceCodeGroup.label,
            "reperibili")) {
          if (!servicesActivated(comp.person.office)) {
            result = Messages.get("CompManager.notConfigured");
            return result;
          }
          group = competenceCodeDao.getCodeWithGroup(comp.competenceCode.competenceCodeGroup,
              Optional.<CompetenceCode>absent());
          if (!handlerReperibility(comp, value, group)) {
            result = Messages.get("CompManager.overServiceLimit");
            return result;
          }
        }
        if (sum + value > comp.competenceCode.competenceCodeGroup.limitValue) {
          result = Messages.get("CompManager.overGroupLimit");
          return result;
        }
        if (value > comp.competenceCode.limitValue) {
          result = Messages.get("CompManager.overMonthLimit");
          return result;
        }
        break;
      case yearly:
        group = competenceCodeDao
        .getCodeWithGroup(comp.competenceCode.competenceCodeGroup,
            Optional.fromNullable(comp.competenceCode));
        compList = competenceDao
            .getCompetences(Optional.fromNullable(comp.person), comp.year,
                Optional.<Integer>absent(), group);
        sum = compList.stream().mapToInt(i -> i.valueApproved).sum();
        if (sum + value > comp.competenceCode.competenceCodeGroup.limitValue) {
          result = Messages.get("CompManager.overYearLimit");
        }
        break;
      case onMonthlyPresence:
        PersonStampingRecap psDto = stampingsRecapFactory
        .create(comp.person, comp.year, comp.month, true);
        if (psDto.basedWorkingDays != value) {
          result = Messages.get("CompManager.diffBasedWorkingDay");
        }
        break;
      case entireMonth:
        /**
         * in questo caso il valore deve essere per forza = 1 perchè rappresenta l'intero mese 
         * assegnato come competenza (caso tipico: cod. 303 Ind.ta' Risc. Rad. Ion. Com.1)
         */
        if (value != comp.competenceCode.limitValue) {
          result = Messages.get("CompManager.overEntireMonth");
        }
        break;
      case noLimit:
        break;
      default:
        throw new IllegalArgumentException();
    }
    return result;
  }


  /**
   * persiste la competenza aggiornando il valore approvato per essa.
   *
   * @param competence la competenza da aggiornare
   * @param value      il valore con cui aggiornare la competenza
   */
  public void saveCompetence(Competence competence, Integer value) {
    competence.valueApproved = value;
    competence.save();
    log.debug("Salvata la competenza {} con il nuovo valore {}", competence, value);
  }


  /**
   * @param yearMonth l'anno/mese di riferimento
   * @param office    la sede per cui si cercano i servizi per reperibilità abilitati
   * @return il numero di giorni di reperibilità disponibili sulla base di quanti servizi per
   * reperibilità sono stati abilitati sulla sede.
   */
  private Integer countDaysForReperibility(YearMonth yearMonth, Office office) {
    int numbers =
        reperibilityDao.getReperibilityTypeByOffice(office, Optional.fromNullable(false)) != null
        ? reperibilityDao.getReperibilityTypeByOffice(
            office, Optional.fromNullable(false)).size()
            : 0;
            return numbers * (new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1)
                .dayOfMonth().getMaximumValue());
  }

  /**
   * @param office la sede su cui cercare.
   * @return true se ci sono servizi attivi per la reperibilità. False altrimenti.
   */
  private boolean servicesActivated(Office office) {
    List<PersonReperibilityType> prtList = reperibilityDao
        .getReperibilityTypeByOffice(office, Optional.fromNullable(false));
    if (prtList.isEmpty()) {
      return false;
    }
    return true;
  }


  /**
   * @param comp  la competenza
   * @param value il quantitativo per la competenza
   * @param group il gruppo di codici di competenza
   * @return false se si supera il limite previsto per i servizi di reperibilità attivi. true
   * altrimenti.
   */
  private boolean handlerReperibility(Competence comp, Integer value, List<CompetenceCode> group) {

    int maxDays = countDaysForReperibility(new YearMonth(comp.year, comp.month),
        comp.person.office);

    List<String> groupCodes = group.stream().map(objA -> {
      String objB = new String();
      objB = objA.code;
      return objB;
    }).collect(Collectors.toList());
    List<Competence> peopleMonthList = competenceDao.getCompetencesInOffice(comp.year,
        comp.month, groupCodes, comp.person.office, false);
    int peopleSum = peopleMonthList.stream().mapToInt(i -> i.valueApproved).sum();
    if (peopleSum + value > maxDays) {
      return false;
    }
    return true;
  }

  /**
   * @param comp la competenza
   * @return true se la competenza è abilitata per la persona. False altrimenti.
   */
  private boolean isCompetenceEnabled(Competence comp) {
    LocalDate date = new LocalDate(comp.year, comp.month, 1);
    Optional<PersonCompetenceCodes> pcc = competenceCodeDao
        .getByPersonAndCodeAndDate(comp.person, comp.competenceCode, date);
    if (pcc.isPresent()) {      
      return true;     
    }
    return false;
  }

  /**
   * @return true se esiste almeno un servizio per reperibilità inizializzato, false altrimenti.
   */
  public boolean isServiceForReperibilityInitialized(
      Office office, List<CompetenceCode> competenceCodeList) {
    boolean servicesInitialized = true;
    if (competenceCodeList.stream().anyMatch(isReperibility())) {
      List<PersonReperibilityType> prtList = reperibilityDao
          .getReperibilityTypeByOffice(office, Optional.fromNullable(new Boolean(false)));
      if (prtList.isEmpty()) {
        servicesInitialized = false;
      }
    }
    return servicesInitialized;
  }

  /**
   * @param personList la lista di persone
   * @param year       l'anno
   * @param month      il mese
   * @return la mappa contenente per ogni persona la propria situazione in termini di codici di
   * competenza abilitati.
   */
  public Map<Person, List<CompetenceCodeDTO>> createMap(
      List<Person> personList, int year, int month) {
    LocalDate date = new LocalDate(year, month, 1);
    Map<Person, List<CompetenceCodeDTO>> map = Maps.newHashMap();
    for (Person person : personList) {
      List<CompetenceCodeDTO> codeList = Lists.newArrayList();
      List<PersonCompetenceCodes> pmrList = competenceCodeDao
          .listByPerson(person, Optional.fromNullable(date));
      if (!pmrList.isEmpty()) {
        for (PersonCompetenceCodes pcc : pmrList) {
          CompetenceCodeDTO dto = new CompetenceCodeDTO();
          dto.code = pcc.competenceCode;
          dto.beginDate = pcc.beginDate;
          dto.endDate = pcc.endDate;
          codeList.add(dto);
        }
      }
      map.put(person, codeList);
    }
    return map;
  }

  /**
   * @param pccList     la lista di PersonCompetenceCodes di partenza
   * @param codeListIds la lista di id di codici competenza da confrontare
   * @return la lista dei codici di assenza da aggiungere alla configurazione dei
   * PersonCompetenceCodes.
   */
  public List<CompetenceCode> codeToSave(List<PersonCompetenceCodes> pccList,
      List<Long> codeListIds) {
    List<CompetenceCode> codeToAdd = Lists.newArrayList();
    if (codeListIds == null || codeListIds.isEmpty()) {
      return codeToAdd;
    }
    for (Long id : codeListIds) {
      CompetenceCode code = competenceCodeDao.getCompetenceCodeById(id);
      if (pccList.isEmpty()) {
        codeToAdd.add(code);
      } else {
        boolean found = false;
        for (PersonCompetenceCodes pcc : pccList) {
          if (pcc.competenceCode.code.equals(code.code)) {
            found = true;
          }
        }
        if (!found) {
          codeToAdd.add(code);
        }
      }
    }
    return codeToAdd;
  }

  /**
   * @param pccList     la lista di personcompetencecode
   * @param codeListIds la lista di id che rappresentano i codici di assenza
   * @return la lista dei codici di competenza da rimuovere da quelli associati alla persona a cui
   * fanno riferimento i personcompetencecode passati come parametro.
   */
  public List<CompetenceCode> codeToDelete(List<PersonCompetenceCodes> pccList,
      List<Long> codeListIds) {
    List<CompetenceCode> codeToRemove = Lists.newArrayList();
    if (codeListIds == null || codeListIds.isEmpty()) {
      pccList.forEach(item -> {
        codeToRemove.add(item.competenceCode);
      });
    } else {
      pccList.forEach(item -> {
        if (!codeListIds.contains(item.competenceCode.id)) {
          codeToRemove.add(item.competenceCode);
        }
      });
    }

    return codeToRemove;
  }

  /**
   * 
   * @param psstList la lista dei personShiftShiftType
   * @param peopleIds la lista degli id dei personShift
   * @return la lista dei personShift da aggiungere alla tabella 
   *     dei PersonShiftShiftType.
   */
  public List<PersonShift> peopleToAdd(List<PersonShiftShiftType> psstList, List<Long> peopleIds) {
    List<PersonShift> peopleToAdd = Lists.newArrayList();
    if (peopleIds == null || peopleIds.isEmpty()) {
      return peopleToAdd;
    }
    for (Long id : peopleIds) {
      PersonShift ps = shiftDao.gerPersonShiftById(id);
      if (psstList.isEmpty()) {
        peopleToAdd.add(ps);
      } else {
        boolean found = false;
        for (PersonShiftShiftType psst : psstList) {
          if (psst.personShift.equals(ps)) {
            found = true;
          }
        }
        if (!found) {
          peopleToAdd.add(ps);
        }
      }      
    }
    return peopleToAdd;
  }

  /**
   * 
   * @param psstList la lista dei personShiftShiftType da controllare
   * @param peopleIds la lista degli id dei personShift
   * @return la lista dei personShift da rimuovere.
   */
  public List<PersonShift> peopleToDelete(List<PersonShiftShiftType> psstList, 
      List<Long> peopleIds) {
    List<PersonShift> peopleToRemove = Lists.newArrayList();
    if (peopleIds == null || peopleIds.isEmpty()) {
      psstList.forEach(item -> {
        peopleToRemove.add(item.personShift);
      });
    } else {
      psstList.forEach(item -> {
        if (!peopleIds.contains(item.personShift.id)) {
          peopleToRemove.add(item.personShift);
        }
      });
    }
    return peopleToRemove;
  }

  /**
   * il metodo che persiste la situazione di codici di competenza per la persona.
   *
   * @param person       la persona per cui persistere la situazione delle competenze
   * @param codeToAdd    la lista dei codici di competenza da aggiungere
   * @param codeToRemove la lista dei codici di competenza da rimuovere
   * @param date         la data della fine dei codici da rimuovere o dell'inizio dei codici da
   *                     aggiungere
   */
  public void persistChanges(Person person, List<CompetenceCode> codeToAdd,
      List<CompetenceCode> codeToRemove, LocalDate date) {

    codeToAdd.forEach(item -> {
      List<PersonCompetenceCodes> pccList = competenceCodeDao.listByPersonAndCode(person, item);
      if (pccList.isEmpty()) {
        createPersonCompetenceCode(person, date, Optional.<LocalDate>absent(), item);
        if (item.code.equals("T1") || item.code.equals("T2") || item.code.equals("T3")) {
          createPersonShift(person);
        }
      } else {
        PersonCompetenceCodes temp = null;
        // conviene crearsi un PCC di appoggio dove mettere il pcc precedente o successivo rispetto a quello che si
        //intende creare così da poterlo poi utilizzare fuori dal ciclo per creare i corretti intervalli temporali
        //dei personcompetencecodes.
        Iterator<PersonCompetenceCodes> iter = pccList.iterator();
        boolean found = false;
        while(iter.hasNext() || !found) {

          DateInterval interval = new DateInterval(iter.next().beginDate, iter.next().endDate);
          if (DateUtility.isDateIntoInterval(date, interval)) {
            if (temp == null) {
              iter.next().endDate = null;
              iter.next().beginDate = date;
              iter.next().save();
            } else {
              iter.next().beginDate = date;
              iter.next().endDate = temp.beginDate.minusDays(1);
              iter.next().save();
            }
            temp = iter.next();
            found = true;
          } else {
            //bene ma non benissimo...
            if (pccList.get(0).endDate != null && pccList.get(0).endDate.isBefore(date)) {
              createPersonCompetenceCode(person, date, Optional.<LocalDate>absent(),item);
            } else {
              createPersonCompetenceCode(person, date, Optional.fromNullable(pccList.get(0).beginDate.minusDays(1)), item);
            }
            found = true;
          }

        }

      }

    });
    codeToRemove.forEach(item -> {

      LocalDate endMonth = date.dayOfMonth().withMaximumValue();
      Optional<PersonCompetenceCodes> pcc = competenceCodeDao.getByPersonAndCodeAndDate(person, item, date);
      if (pcc.isPresent()) {
        pcc.get().endDate = endMonth;
        pcc.get().save();
        if (item.code.equals("T1") || item.code.equals("T2") || item.code.equals("T3")) {
          PersonShift personShift = personShiftDayDao.getPersonShiftByPerson(pcc.get().person);
          if (personShift != null) {
            personShift.disabled = true;
            personShift.save();
          } else {
            log.warn("Non è presente in tabella person_shift l'utente {}", person.fullName());
          }
        }
      } else {
        throw new RuntimeException(Messages.get("errorCompetenceCodeException"));
      }
    });
  }

  /**
   * 
   * @param peopleToAdd
   * @param shiftType
   * @param peopleToRemove
   * @param beginDate
   * @param endDate
   */
  public void persistPersonShiftShiftType(List<PersonShift> peopleToAdd, ShiftType shiftType,
      List<PersonShift> peopleToRemove) {

    peopleToAdd.forEach(item -> {
      PersonShiftShiftType psst = new PersonShiftShiftType();
      psst.personShift = item;
      psst.beginDate = LocalDate.now();
      psst.shiftType = shiftType;
      psst.save();
    });

    peopleToRemove.forEach(item -> {
      Optional<PersonShiftShiftType> psst = shiftDao.getByPersonShiftAndShiftType(item, shiftType);
      if (psst.isPresent()) {
        psst.get().endDate = LocalDate.now();
        psst.get().save();
      }
    });
  }
  /**
   * @param personList la lista di persone attive
   * @param date       la data in cui si richiedono le competenze
   * @return la creazione della lista di competenze per il mese/anno.
   */
  public List<Competence> createCompetenceList(List<Person> personList, LocalDate date,
      CompetenceCode code) {
    List<Competence> compList = Lists.newArrayList();
    for (Person person : personList) {
      Optional<Competence> comp = competenceDao.getCompetence(person, date.getYear(),
          date.getMonthOfYear(), code);
      if (comp.isPresent()) {
        compList.add(comp.get());
      } else {
        Competence competence = new Competence();
        competence.person = person;
        competence.competenceCode = code;
        competence.month = date.getMonthOfYear();
        competence.year = date.getYear();
        competence.save();
        compList.add(competence);
      }

    }
    return compList;
  }

  /**
   *
   * @param competenceList la lista delle competenze assegnate nell'anno/mese a una persona
   * @return una mappa già formata per la visualizzazione della situazione mensile delle competenze
   *     della singola persona.
   */
  public Map<CompetenceCode, String> createMapForCompetences(List<Competence> competenceList) {
    Map<CompetenceCode, String> map = Maps.newHashMap();
    competenceList.forEach(item -> {
      if (item.competenceCode.limitUnit != null) {
        map.put(item.competenceCode, item.valueApproved+" "+item.competenceCode.limitUnit.getDescription());
      } else {
        map.put(item.competenceCode, item.valueApproved+"");
      }

    });
    return map;

  }

  /**
   * 
   * @param list la lista contenente tutte le timetable dei turni disponibili
   * @return una lista di dto modellati per esigenze di template.
   */
  public List<ShiftTimeTableDto> convertFromShiftTimeTable(List<ShiftTimeTable> list) {
    final String stamping_format = "HH:mm";
    List<ShiftTimeTableDto> dtoList = list.stream().map(shiftTimeTable -> {
      ShiftTimeTableDto dto = new ShiftTimeTableDto();
      dto.id = shiftTimeTable.id;
      dto.endAfternoon = shiftTimeTable.endAfternoon.toString(stamping_format);
      dto.endAfternoonLunchTime = shiftTimeTable.endAfternoonLunchTime.toString(stamping_format);
      dto.endMorning = shiftTimeTable.endMorning.toString(stamping_format);
      dto.endMorningLunchTime = shiftTimeTable.endMorningLunchTime.toString(stamping_format);
      dto.startAfternoon = shiftTimeTable.startAfternoon.toString(stamping_format);
      dto.startAfternoonLunchTime = shiftTimeTable
          .startAfternoonLunchTime.toString(stamping_format);
      dto.startMorning = shiftTimeTable.startMorning.toString(stamping_format);
      dto.startMorningLunchTime = shiftTimeTable.startMorningLunchTime.toString(stamping_format);
      return dto;
    }).collect(Collectors.toList());
    return dtoList;
  }

  /**
   * crea un personShift a partire dalla persona passata come parametro.
   * @param person la persona di cui si vuole creare l'istanza di personShift
   */
  private void createPersonShift(Person person) {
    PersonShift personShift = null;
    personShift = personShiftDayDao.getPersonShiftByPerson(person);
    if (personShift != null) {
      log.info("L'utente {} è già presente in tabella person_shift", person.fullName());
    } else {
      personShift = new PersonShift();
      personShift.person = person;
      personShift.description = "Turni di " +person.fullName();
      personShift.jolly = false;
      personShift.disabled = false;
      personShift.save();
    }
  }

  /**
   * persiste sul db un personcompetencecode.
   * @param person la persona che ha associata la competenza
   * @param date la data da cui è valida quella competenza
   * @param code la competenza da abilitare
   */
  private void createPersonCompetenceCode(Person person, LocalDate dateBegin, 
      Optional<LocalDate> dateEnd, CompetenceCode code) {
    PersonCompetenceCodes newPcc = new PersonCompetenceCodes();
    newPcc.competenceCode = code;
    newPcc.person = person;
    newPcc.beginDate = dateBegin;
    if (dateEnd.isPresent()) {
      newPcc.endDate = dateEnd.get();
    }
    newPcc.save();
  }
}
