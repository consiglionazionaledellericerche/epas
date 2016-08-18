package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import play.i18n.Messages;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import helpers.jpa.ModelQuery.SimpleResults;

import it.cnr.iit.epas.DateUtility;

import manager.AbsenceManager.AbsenceToDate;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Competence;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TotalOvertime;
import models.enumerate.LimitType;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.AssertionInfo;
import org.assertj.core.description.Description;
import org.assertj.core.internal.Strings;
import org.assertj.core.presentation.Representation;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class CompetenceManager {


  private static final Logger log = LoggerFactory.getLogger(CompetenceManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final OfficeDao officeDao;
  private final PersonDayDao personDayDao;
  private final CompetenceDao competenceDao;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayManager personDayManager;
  private final PersonReperibilityDayDao reperibilityDao;
  private static PersonStampingRecapFactory stampingsRecapFactory;

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
      PersonStampingRecapFactory stampingsRecapFactory) {
    this.competenceCodeDao = competenceCodeDao;
    this.officeDao = officeDao;
    this.competenceDao = competenceDao;
    this.personDayDao = personDayDao;
    this.wrapperFactory = wrapperFactory;
    this.personDayManager = personDayManager;
    this.reperibilityDao = reperibilityDao;
    this.stampingsRecapFactory = stampingsRecapFactory;    
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
    } catch (Exception e) {
      return false;
    }
    total.save();
    return true;

  }

  /**
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
   * @return true se avviene correttamente il cambiamento della lista di competenze attive per la
   *     persona Person passata come parametro.
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
//      if (!value) {
//        if (person.competenceCode.contains(competenceCodeDao.getCompetenceCodeById(code.id))) {
//          person.competenceCode.remove(competenceCodeDao.getCompetenceCodeById(code.id));
//        } else {
//          continue;
//        }
//      } else {
//        if (person.competenceCode.contains(competenceCodeDao.getCompetenceCodeById(code.id))) {
//          continue;
//        } else {
//          person.competenceCode.add(competenceCodeDao.getCompetenceCodeById(code.id));
//        }
//      }

    }
    person.save();
    return true;
  }

  /**
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
        .create(person).getMonthContracts(year, month);
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
   * 
   * @param comp la competenza da aggiornare
   * @param value il quantitativo per quella competenza da aggiornare
   * @return La stringa contenente il messaggio da far visualizzare come errore, se riscontrato.
   *     Stringa vuota altrimenti.
   */
  public String canAddCompetence(Competence comp, Integer value) {
    
    String result = "";
    switch(comp.competenceCode.limitType) {
      case monthly:        
        
        if (comp.valueApproved + value > comp.competenceCode.limitValue) {
          List<CompetenceCode> group = competenceCodeDao
              .getCodeWithGroup(comp.competenceCode.competenceCodeGroup);
          List<Competence> compList = competenceDao.getCompetences(Optional.fromNullable(comp.person), comp.year, 
              Optional.fromNullable(comp.month), group);
          int sum = compList.stream().mapToInt(i -> i.valueApproved).sum();
          if (sum + value > comp.competenceCode.competenceCodeGroup.limitValue) {
            result = Messages.get("CompManager.overGroupLimit");          
          }
          //Caso Reperibilità:
          if (StringUtils.containsIgnoreCase(comp.competenceCode.competenceCodeGroup.label, "reperibili")) {
            if (!handlerReperibility(comp, value, group)) {
              result = Messages.get("CompManager.overServiceLimit");
            }
          }
        }        
        break;
      case yearly:
        List<Competence> compList = competenceDao.getCompetences(Optional.fromNullable(comp.person), comp.year, 
            Optional.<Integer>absent(), Lists.newArrayList(comp.competenceCode));
        int sum = compList.stream().mapToInt(i -> i.valueApproved).sum();      
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
      case noLimit:
        break;
      default:
        throw new IllegalArgumentException();
    }
    return result;
  }



  /**
   * persiste la competenza aggiornando il valore approvato per essa.
   * @param competence la competenza da aggiornare
   * @param value il valore con cui aggiornare la competenza
   */
  public void saveCompetence(Competence competence, Integer value) {
    competence.valueApproved = value;
    competence.save();
    log.debug("Salvata la competenza {} con il nuovo valore {}", competence, value);
  }


 /**
  * 
  * @param yearMonth l'anno/mese di riferimento
  * @param office la sede per cui si cercano i servizi per reperibilità abilitati
  * @return il numero di giorni di reperibilità disponibili sulla base di quanti servizi per reperibilità
  *     sono stati abilitati sulla sede.
  */
  private Integer countDaysForReperibility(YearMonth yearMonth, Office office) {
    int numbers = reperibilityDao.getReperibilityTypeByOffice(office) != null ? 
        reperibilityDao.getReperibilityTypeByOffice(office).size() : 0;
    return numbers * (new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(),1)
        .monthOfYear().getMaximumValue());
  }
  
  /**
   * 
   * @param comp
   * @param value
   * @param group
   * @return false se si supera il limite previsto per i servizi di reperibilità attivi.
   *     true altrimenti.
   */
  private boolean handlerReperibility(Competence comp, Integer value, List<CompetenceCode> group) {
    
    int maxDays = countDaysForReperibility(new YearMonth(comp.year, comp.month), comp.person.office);
    List<Competence> peopleMonthList = competenceDao
        .getCompetences(Optional.<Person>absent(), comp.year, Optional.fromNullable(comp.month), group);
    int peopleSum = peopleMonthList.stream().mapToInt(i -> i.valueApproved).sum();
    if (peopleSum + value > maxDays) {
      return false;
    }
    return true;
  }
  
}
