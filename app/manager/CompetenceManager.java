package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import helpers.jpa.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import manager.competences.ShiftTimeTableDto;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.OrganizationShiftTimeTable;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftShiftType;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.TotalOvertime;
import models.dto.TimeTableDto;
import models.enumerate.CalculationType;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.jobs.Job;
import play.libs.F.Promise;

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

  private final PersonDao personDao;


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
      SecureManager secureManager, PersonDao personDao) {

    this.competenceCodeDao = competenceCodeDao;
    this.officeDao = officeDao;
    this.competenceDao = competenceDao;
    this.personDayDao = personDayDao;
    this.wrapperFactory = wrapperFactory;
    this.personDayManager = personDayManager;
    this.reperibilityDao = reperibilityDao;
    this.stampingsRecapFactory = stampingsRecapFactory;   
    this.personShiftDayDao = personshiftDayDao;    
    this.personDao = personDao;
  }

  public static Predicate<CompetenceCode> isReperibility() {
    return p -> p.code.equalsIgnoreCase("207") || p.code.equalsIgnoreCase("208");
  }


  /**
   * Metodo che genera la lista di stringhe contenente i codici per straordinari.
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
   * Metodo che conteggia il monte ore di straordinari.
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
   * Metodo che conteggia il quantitativo annuale degli straordinari.
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
   * Metodo che conteggia il quantitativo mensile degli straordinari.
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
   * Metodo che genera la tabella per la visualizzazione degli straordinari.
   * @return la tabella formata da persone, dato e valore intero relativi ai quantitativi orari su
   *     orario di lavoro, straordinario, riposi compensativi per l'anno year e il mese month per le
   *     persone dell'ufficio office.
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
   * Metodo per la creazione del file per l'esportazione degli straordinari.
   * @return il file contenente tutti gli straordinari effettuati dalle persone presenti nella lista
   *     personList nell'anno year.
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

      log.debug("Totale per {} vale {}", p.getFullname(), totale);
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
   * Metodo che verifica la possibilità di aggiungere la competenza.
   * @param comp  la competenza da aggiornare
   * @param value il quantitativo per quella competenza da aggiornare
   * @return La stringa contenente il messaggio da far visualizzare come errore, se riscontrato.
   *     Stringa vuota altrimenti.
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
        PersonStampingRecap psDto = 
            stampingsRecapFactory.create(comp.person, comp.year, comp.month, true);
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
    log.debug("Id competenza {}", competence.id);
  }


  /**
   * Metodo per il conteggio dei giorni di reperibilità massimi in un mese/anno.
   * @param yearMonth l'anno/mese di riferimento
   * @param office    la sede per cui si cercano i servizi per reperibilità abilitati
   * @return il numero di giorni di reperibilità disponibili sulla base di quanti servizi per
   *     reperibilità sono stati abilitati sulla sede.
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
   * Metodo per la verifica sui servizi abilitati.
   * @param office la sede su cui cercare.
   * @return true se ci sono servizi attivi per la reperibilità. False altrimenti.
   */
  private boolean servicesActivated(Office office) {
    List<PersonReperibilityType> prtList = 
        reperibilityDao.getReperibilityTypeByOffice(office, Optional.fromNullable(false));
    if (prtList.isEmpty()) {
      return false;
    }
    return true;
  }


  /**
   * Metodo che verifica la pertinenza della quantità di giorni di reperibilità assegnati 
   *    in base ai limiti previsti.
   * @param comp  la competenza
   * @param value il quantitativo per la competenza
   * @param group il gruppo di codici di competenza
   * @return false se si supera il limite previsto per i servizi di reperibilità attivi. 
   *     true altrimenti.
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
    int peopleSum = peopleMonthList.stream()
        .filter(competence -> competence.id != comp.id).mapToInt(i -> i.valueApproved).sum();
    if (peopleSum + value > maxDays) {
      return false;
    }
    return true;
  }

  /**
   * Metodo per il controllo dell'abilitazione di una competenza a una persona.
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
   * Metodo per il controllo sull'esistenza di un servizio di reperibilità.
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
   * Metodo che ritorna la lista dei codici di competenza da salvare.
   * @param pccList     la lista di PersonCompetenceCodes di partenza
   * @param codeListIds la lista di id di codici competenza da confrontare
   * @return la lista dei codici di assenza da aggiungere alla configurazione dei
   *     PersonCompetenceCodes.
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
   * Metodo che ritorna la lista dei codici di competenza da rimuovere.
   * @param pccList     la lista di personcompetencecode
   * @param codeListIds la lista di id che rappresentano i codici di assenza
   * @return la lista dei codici di competenza da rimuovere da quelli associati alla persona a cui
   *     fanno riferimento i personcompetencecode passati come parametro.
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

      } else {
        PersonCompetenceCodes temp = null;
        int counter = 0;

        boolean found = false;
        while (counter < pccList.size() && found == false) {
          DateInterval interval = null;
          if (pccList.get(counter).endDate != null) {            
            interval = new DateInterval(pccList.get(counter).beginDate, 
                pccList.get(counter).endDate);
          } else {
            interval = DateInterval.withBegin(pccList.get(counter).beginDate, 
                Optional.<LocalDate>absent());
          }

          if (DateUtility.isDateIntoInterval(date, interval)) {
            if (temp == null) {
              pccList.get(counter).endDate = null;
              pccList.get(counter).beginDate = date;
              pccList.get(counter).save();
            } else {
              pccList.get(counter).beginDate = date;
              pccList.get(counter).endDate = temp.beginDate.minusDays(1);
              pccList.get(counter).save();
            }

            found = true;
          }
          counter++;
        }
        if (!found) {
          PersonCompetenceCodes pccRecent = pccList.get(0);
          PersonCompetenceCodes pccAncient = pccList.get(pccList.size() - 1);
          if (pccRecent != pccAncient) {            
            if (!pccAncient.beginDate.isBefore(date)) {
              createPersonCompetenceCode(person, date, 
                  Optional.fromNullable(pccAncient.beginDate.minusDays(1)), item);
            } else if (!pccRecent.beginDate.isAfter(date)) {
              createPersonCompetenceCode(person, date, Optional.<LocalDate>absent(), item);
            } else {
              Optional<PersonCompetenceCodes> pcc = 
                  competenceCodeDao.getNearFuture(person, item, date);
              if (pcc.isPresent()) {
                createPersonCompetenceCode(person, date, 
                    Optional.fromNullable(pcc.get().beginDate.minusDays(1)), item);
              }
            }            
          } else {
            // esiste un solo personcompetencecodes 
            if (!pccRecent.beginDate.isAfter(date) 
                && (pccRecent.endDate == null || pccRecent.endDate.isAfter(date))) {
              log.info("Si intende creare un personCompetenceCode sovrascrivendo "
                  + "la data di inizio di uno già esistente.");
            } else if (pccRecent.beginDate.isAfter(date)) {
              updatePersonCompetenceCode(pccRecent, Optional.fromNullable(date), 
                  Optional.<LocalDate>absent());
            } else if (pccRecent.endDate != null && !pccRecent.endDate.isAfter(date)) {
              createPersonCompetenceCode(person, date, Optional.<LocalDate>absent(), item);
            }
          }                    
          found = true;          
        }
      }
      if (item.code.equals("T1") || item.code.equals("T2") || item.code.equals("T3")) {
        createPersonShift(person, date);
      }
    });
    codeToRemove.forEach(item -> {

      LocalDate endMonth = date.dayOfMonth().withMaximumValue();
      Optional<PersonCompetenceCodes> pcc = 
          competenceCodeDao.getByPersonAndCodeAndDate(person, item, date);
      if (pcc.isPresent()) {

        if (pcc.get().beginDate.monthOfYear().equals(date.monthOfYear())) {
          pcc.get().delete();
        } else {
          pcc.get().endDate = endMonth;
          pcc.get().save();
        }

        if (item.code.equals("T1") || item.code.equals("T2") || item.code.equals("T3")) {
          PersonShift personShift = 
              personShiftDayDao.getPersonShiftByPerson(pcc.get().person, pcc.get().beginDate);
          if (personShift != null) {
            personShift.endDate = endMonth;
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
   * persiste il personShiftShiftType con i parametri passati al metodo.
   * @param person la persona in turno da associare all'attività
   * @param beginDate la data di inizio partecipazione all'attività in turno
   * @param type l'attività su cui far aderire la persona
   * @param jolly true se la persona è jolly e può fare più turni 
   *     sull'attività (di solito mattina e pomeriggio), false altrimenti
   */
  public void persistPersonShiftShiftType(PersonShift person, LocalDate beginDate, 
      ShiftType type, boolean jolly) {
    PersonShiftShiftType psst = new PersonShiftShiftType();
    psst.beginDate = beginDate;
    psst.shiftType = type;
    psst.jolly = jolly;
    psst.personShift = person;
    psst.endDate = null;
    psst.save();
  }

  /**
   * persiste il personReperibility con i parametri passati al metodo.
   * @param person la persona in reperibilità da associare all'attività
   * @param beginDate la data di inizio partecipazione all'attività in reperibilità
   * @param type l'attività su cui far aderire la persona
   */
  public void persistPersonReperibilityType(Person person, LocalDate beginDate, 
      PersonReperibilityType type) {
    PersonReperibility rep = new PersonReperibility();
    rep.person = person;
    rep.startDate = beginDate;
    rep.personReperibilityType = type;
    rep.save();

  }

  /**
   * Metodo per la creazione della lista di competenze.
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
        compList.add(competence);
      }

    }
    return compList;
  }

  /**
   * Metodo per creazione della mappa che associa la persona alle competenze assegnate.
   * @param competenceList la lista delle competenze assegnate nell'anno/mese a una persona
   * @return una mappa già formata per la visualizzazione della situazione mensile delle competenze
   *     della singola persona.
   */
  public Map<CompetenceCode, String> createMapForCompetences(List<Competence> competenceList) {
    Map<CompetenceCode, String> map = Maps.newHashMap();
    competenceList.forEach(item -> {
      if (item.competenceCode.limitUnit != null) {
        map.put(item.competenceCode, item.valueApproved + " " 
            + item.competenceCode.limitUnit.getDescription());
      } else {
        map.put(item.competenceCode, item.valueApproved + "");
      }

    });
    return map;

  }

  /**
   * Metodo che ritorna la lista dei dto modellati per esigenza di template.
   * @param list la lista contenente tutte le timetable dei turni disponibili
   * @return una lista di dto modellati per esigenze di template.
   */
  public List<ShiftTimeTableDto> convertFromShiftTimeTable(List<ShiftTimeTable> list) {
    final String stamping_format = "HH:mm";
    List<ShiftTimeTableDto> dtoList = list.stream().map(shiftTimeTable -> {
      ShiftTimeTableDto dto = new ShiftTimeTableDto();
      if (shiftTimeTable.office != null) {
        dto.isOfficeTimeTable = true;
      } else {
        dto.isOfficeTimeTable = false;
      }
      dto.id = shiftTimeTable.id;
      dto.calculationType = shiftTimeTable.calculationType.getName();
      dto.endAfternoon = shiftTimeTable.endAfternoon.toString(stamping_format);
      dto.endAfternoonLunchTime = shiftTimeTable.endAfternoonLunchTime.toString(stamping_format);
      dto.endMorning = shiftTimeTable.endMorning.toString(stamping_format);
      dto.endMorningLunchTime = shiftTimeTable.endMorningLunchTime.toString(stamping_format);
      dto.startAfternoon = shiftTimeTable.startAfternoon.toString(stamping_format);
      dto.startAfternoonLunchTime = shiftTimeTable
          .startAfternoonLunchTime.toString(stamping_format);
      dto.startMorning = shiftTimeTable.startMorning.toString(stamping_format);
      dto.startMorningLunchTime = shiftTimeTable.startMorningLunchTime.toString(stamping_format);
      dto.startEvening = shiftTimeTable.startEvening != null 
          ? shiftTimeTable.startEvening.toString(stamping_format) : "";
      dto.endEvening = shiftTimeTable.endEvening != null 
              ? shiftTimeTable.endEvening.toString(stamping_format) : "";
      dto.startEveningLunchTime = shiftTimeTable.startEveningLunchTime != null 
              ? shiftTimeTable.startEveningLunchTime.toString(stamping_format) : "";
      dto.endEveningLunchTime = shiftTimeTable.endEveningLunchTime != null 
              ? shiftTimeTable.endEveningLunchTime.toString(stamping_format) : "";
      return dto;
    }).collect(Collectors.toList());
    return dtoList;
  }

  /**
   * persiste l'attività di turno con tutte le info corredate.
   * @param service il dto da cui estrarre le informazioni per il salvataggio dell'attività di turno
   * @param stt la shifttimetable associata all'attività di turno
   * @param cat il turno a cui associare l'attività
   */
  public void persistShiftType(ShiftType service, Optional<ShiftTimeTable> stt, 
      Optional<OrganizationShiftTimeTable> ostt, ShiftCategories cat) {
    ShiftType st = new ShiftType();    

    st.description = service.description;
    st.type = service.type;
    st.shiftCategories = cat;
    if (stt.isPresent()) {
      st.shiftTimeTable = stt.get();
      if (Range.closed(stt.get().startMorning, stt.get().endMorning)
          .encloses(Range.closed(stt.get().startMorningLunchTime, stt.get().endMorningLunchTime))) {
        st.breakInShift = service.breakInShift;
        st.breakMaxInShift = service.breakMaxInShift;      
        st.exitTolerance = service.exitTolerance;
        st.exitMaxTolerance = service.exitMaxTolerance;      
        st.entranceMaxTolerance = service.entranceMaxTolerance;
        st.entranceTolerance = service.entranceTolerance;
        st.maxToleranceAllowed = service.maxToleranceAllowed;

      } else {

        if (service.exitTolerance != 0 || service.exitMaxTolerance != 0) {
          st.exitMaxTolerance = service.exitMaxTolerance;
          st.exitTolerance = service.exitTolerance;
          st.maxToleranceAllowed = 2;
        } else {
          st.exitTolerance = 0;
          st.exitMaxTolerance = 0;
          st.maxToleranceAllowed = 1;
        }      
        st.breakInShift = service.breakMaxInShift;
        st.entranceTolerance = service.entranceMaxTolerance;
        st.entranceMaxTolerance = service.entranceMaxTolerance;
      } 
    }
    if (ostt.isPresent()) {      
      st.organizaionShiftTimeTable = ostt.get();   
      st.breakInShift = service.breakInShift;
      st.breakMaxInShift = service.breakMaxInShift;      
      st.exitTolerance = service.exitTolerance;
      st.exitMaxTolerance = service.exitMaxTolerance;      
      st.entranceMaxTolerance = service.entranceMaxTolerance;
      st.entranceTolerance = service.entranceTolerance;
      st.maxToleranceAllowed = service.maxToleranceAllowed;
    }
  
    st.save();
  }

  /**
   * crea la timetable da associare al turno.
   * @param timeTable il dto da cui creare la ShiftTimeTable
   * @param office la sede a cui associare la timeTable
   */
  public void createShiftTimeTable(TimeTableDto timeTable, Office office, 
      CalculationType calculationType) {

    ShiftTimeTable stt = new ShiftTimeTable();
    stt.office = office;
    stt.calculationType = calculationType;
    stt.paidMinutes = timeTable.paidMinutes;
    stt.totalWorkMinutes = timeTable.totalWorkMinutes;
    stt.startMorning = normalize(timeTable.startMorning);
    stt.endMorning = normalize(timeTable.endMorning);
    stt.startAfternoon = normalize(timeTable.startAfternoon);
    stt.endAfternoon = normalize(timeTable.endAfternoon);
    if (timeTable.startEvening != null && !timeTable.startEvening.equals("")) {
      stt.startEvening = normalize(timeTable.startEvening);
    } else {
      stt.startEvening = null;
    }
    if (timeTable.endEvening != null && !timeTable.endEvening.equals("")) {
      stt.endEvening = normalize(timeTable.endEvening);
    } else {
      stt.endEvening = null;
    }

    stt.startMorningLunchTime = normalize(timeTable.startMorningLunchTime);
    stt.endMorningLunchTime = normalize(timeTable.endMorningLunchTime);
    stt.startAfternoonLunchTime = normalize(timeTable.startAfternoonLunchTime);
    stt.endAfternoonLunchTime = normalize(timeTable.endAfternoonLunchTime);
    if (timeTable.startEveningLunchTime != null && !timeTable.startEveningLunchTime.equals("")) {
      stt.startEveningLunchTime = normalize(timeTable.startEveningLunchTime);
    } else {
      stt.startEveningLunchTime = null;
    }
    if (timeTable.endEveningLunchTime != null && !timeTable.endEveningLunchTime.equals("")) {
      stt.endEveningLunchTime = normalize(timeTable.endEveningLunchTime); 
    } else {
      stt.endEveningLunchTime = null; 
    }

    stt.save();
  }


  /**
   * chiama il metodo su ciascuna persona della sede per cui fare i conteggi del codice di
   *     competenza a presenza mensile.
   * @param office la sede opzionale per cui fare i conteggi 
   * @param code il codice di competenza a presenza mensile da conteggiare
   * @param yearMonth l'anno/mese per cui fare i conteggi
   */
  public void applyBonus(Optional<Office> office,  
      CompetenceCode code, YearMonth yearMonth) {

    Set<Office> offices = office.isPresent() ? Sets.newHashSet(office.get())
        : Sets.newHashSet(officeDao.getAllOffices());

    List<Person> personList = Lists.newArrayList();

    final List<Promise<Void>> results = new ArrayList<>();
    for (Office o : offices) {

      personList = personDao.listForCompetence(Sets.newHashSet(o), yearMonth, code);
      for (Person p : personList) {
        results.add(new Job<Void>() {

          @Override
          public void doJob() {
            final Person person = Person.findById(p.id);

            applyBonusPerPerson(person, yearMonth, code);
            log.debug("Assegnata la competenza {} alla persona ... {}", code, person);
          }
        }.now());
      }     

    }
    Promise.waitAll(results);
  }

  /**
   * effettua automaticamente l'aggiornamento del valore per la competenza a presenza mensile 
   *     passata come parametro.
   * @param person la persona su cui fare i conteggi
   * @param yearMonth l'anno/mese in cui fare i conteggi
   * @param code il codice di competenza da riconteggiare
   */
  public void applyBonusPerPerson(Person person, YearMonth yearMonth, CompetenceCode code) {
    LocalDate date = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(),1);
    Optional<PersonCompetenceCodes> pcc = competenceCodeDao
        .getByPersonAndCodeAndDate(person, code, date);
    if (pcc.isPresent()) {

      switch (code.limitType) {
        case onMonthlyPresence:
          PersonStampingRecap psDto = stampingsRecapFactory
              .create(person, yearMonth.getYear(), yearMonth.getMonthOfYear(), true);
          addSpecialCompetence(person, yearMonth, code, Optional.fromNullable(psDto));
          break;
        case entireMonth:
          addSpecialCompetence(person, yearMonth, code, Optional.<PersonStampingRecap>absent());
          break;
        default:
          break;
      }
    } else {
      log.warn("La competenza {} non risulta abilitata per il dipendente {} nel mese "
          + "e nell'anno selezionati", code, person.fullName());
    }
  }

  /**
   * assegna le competenze speciali (su presenza mensile o assegnano interamente un mese).
   * @param person la persona a cui assegnare la competenza
   * @param yearMonth l'anno mese per cui assegnare la competenza
   * @param code il codice competenza da assegnare
   * @param psDto (opzionale) se presente serve al calcolo dei giorni di presenza
   */
  private void addSpecialCompetence(Person person, YearMonth yearMonth, CompetenceCode code, 
      Optional<PersonStampingRecap> psDto) {
    int value = 0;
    if (psDto.isPresent()) {
      value = psDto.get().basedWorkingDays;
    } else {
      value = code.limitValue;
    }
    Optional<Competence> competence = competenceDao
        .getCompetence(person, yearMonth.getYear(), yearMonth.getMonthOfYear(), code);
    if (competence.isPresent()) {
      competence.get().valueApproved = value;
      competence.get().save();
    } else {
      Competence comp = new Competence();
      comp.competenceCode = code;
      comp.person = person;
      comp.year = yearMonth.getYear();
      comp.month = yearMonth.getMonthOfYear();
      comp.valueApproved = value;
      comp.save();
    }
    log.debug("Assegnata competenza a {}", person.fullName());
  }


  /**
   * crea un personShift a partire dalla persona passata come parametro.
   * @param person la persona di cui si vuole creare l'istanza di personShift
   */
  private void createPersonShift(Person person, LocalDate date) {
    PersonShift personShift = null;
    personShift = personShiftDayDao.getPersonShiftByPerson(person, date);
    if (personShift != null) {
      log.info("L'utente {} è già presente in tabella person_shift", person.fullName());

    } else {
      personShift = new PersonShift();
      personShift.person = person;
      personShift.description = "Turni di " + person.fullName();
      personShift.disabled = false;
      personShift.beginDate = date;
      personShift.save();
      //TODO: capire come gestire eventuali buchi nel tempo...
      //es.: personShift abilitato a gennaio, non presente a febbraio, abilitato a marzo
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

  /**
   * modifica il personcompetencecode con le date passate come parametro.
   * @param pcc il personcompetencecode da modificare
   * @param beginDate la data di inizio con cui modificare il pcc
   * @param endDate l'eventuale data fine con cui modificare il pcc
   */
  private void updatePersonCompetenceCode(PersonCompetenceCodes pcc, 
      Optional<LocalDate> beginDate, Optional<LocalDate> endDate) {
    if (beginDate.isPresent()) {
      pcc.beginDate = beginDate.get();
    }
    if (endDate.isPresent()) {
      pcc.endDate = endDate.get();
    }
    pcc.save();
  }


  private LocalTime normalize(String time) {
    time = time.replaceAll(":", "");
    Integer hour = Integer.parseInt(time.substring(0, 2));
    Integer minute = Integer.parseInt(time.substring(2, 4));

    return new LocalTime(hour, minute, 0);
  }
}
