package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import com.beust.jcommander.internal.Maps;

import controllers.Security;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import helpers.jpa.ModelQuery.SimpleResults;

import it.cnr.iit.epas.DateUtility;

import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.Absence;
import models.AbsenceType;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.WorkingTimeType;
import models.enumerate.CheckType;
import models.exports.PersonOvertime;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.Blob;
import play.db.jpa.JPAPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

public class ChartsManager {

  private static final Logger log = LoggerFactory.getLogger(ChartsManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final CompetenceManager competenceManager;
  private final PersonDao personDao;
  private final AbsenceDao absenceDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final IVacationsService vacationsService;
  private final IWrapperFactory wrapperFactory;
  private final PersonDayDao personDayDao;

  /**
   * Costruttore.
   * @param competenceCodeDao competenceCodeDao
   * @param competenceDao competenceDao
   * @param competenceManager competenceManager
   * @param personDao personDao
   * @param vacationsService vacationsService
   * @param absenceDao absenceDao
   * @param wrapperFactory wrapperFactory
   */
  @Inject
  public ChartsManager(CompetenceCodeDao competenceCodeDao,
      CompetenceDao competenceDao, CompetenceManager competenceManager,
      PersonDao personDao, IVacationsService vacationsService,
      AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao, 
      IWrapperFactory wrapperFactory, PersonDayDao personDayDao) {
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.personDao = personDao;
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.vacationsService = vacationsService;
    this.wrapperFactory = wrapperFactory;
    this.personDayDao = personDayDao;
  }

  /**
   * @return la lista di oggetti Year a partire dall'inizio di utilizzo del programma a oggi.
   */
  public List<Year> populateYearList(Office office) {
    List<Year> annoList = Lists.newArrayList();
    Integer yearBegin = null;
    int counter = 0;

    counter++;
    LocalDate date = office.getBeginDate();
    yearBegin = date.getYear();
    annoList.add(new Year(counter, yearBegin));

    if (yearBegin != null) {
      for (int i = yearBegin; i <= LocalDate.now().getYear(); i++) {
        counter++;
        annoList.add(new Year(counter, i));
      }
    }

    return annoList;
  }

  /**
   * @return la lista degli oggetti Month.
   */
  public List<Month> populateMonthList() {
    List<Month> meseList = Lists.newArrayList();
    meseList.add(new Month(1, "Gennaio"));
    meseList.add(new Month(2, "Febbraio"));
    meseList.add(new Month(3, "Marzo"));
    meseList.add(new Month(4, "Aprile"));
    meseList.add(new Month(5, "Maggio"));
    meseList.add(new Month(6, "Giugno"));
    meseList.add(new Month(7, "Luglio"));
    meseList.add(new Month(8, "Agosto"));
    meseList.add(new Month(9, "Settembre"));
    meseList.add(new Month(10, "Ottobre"));
    meseList.add(new Month(11, "Novembre"));
    meseList.add(new Month(12, "Dicembre"));
    return meseList;
  }

  /**
   * @return la lista dei competenceCode che comprende tutti i codici di straordinario presenti in
   *     anagrafica.
   */
  public List<CompetenceCode> populateOvertimeCodeList() {
    List<CompetenceCode> codeList = Lists.newArrayList();
    CompetenceCode c1 = competenceCodeDao.getCompetenceCodeByCode("S1");
    CompetenceCode c2 = competenceCodeDao.getCompetenceCodeByCode("S2");
    CompetenceCode c3 = competenceCodeDao.getCompetenceCodeByCode("S3");
    codeList.add(c1);
    codeList.add(c2);
    codeList.add(c3);
    return codeList;
  }

  /**
   * @return la lista dei personOvertime.
   */
  public List<PersonOvertime> populatePersonOvertimeList(
      List<Person> personList, List<CompetenceCode> codeList, int year, int month) {
    List<PersonOvertime> poList = Lists.newArrayList();
    for (Person p : personList) {
      if (p.office.equals(Security.getUser().get().person.office)) {
        PersonOvertime po = new PersonOvertime();
        Long val = null;
        Optional<Integer> result =
            competenceDao
            .valueOvertimeApprovedByMonthAndYear(
                year, Optional.fromNullable(month), Optional.fromNullable(p), codeList);
        if (result.isPresent()) {
          val = result.get().longValue();
        }

        po.month = month;
        po.year = year;
        po.overtimeHour = val;
        po.name = p.name;
        po.surname = p.surname;
        po.positiveHourForOvertime = competenceManager.positiveResidualInMonth(p, year, month) / 60;
        poList.add(po);
        log.info("Aggiunto {} {} alla lista con i suoi dati", p.name, p.surname);
      }

    }
    return poList;
  }


  // ******* Inizio parte di business logic *********/

  /**
   * @return il totale delle ore residue per anno totali sommando quelle che ha ciascuna persona
   *     della lista personeProva.
   */
  public int calculateTotalResidualHour(List<Person> personeProva, int year) {
    int totaleOreResidue = 0;
    for (Person p : personeProva) {
      if (p.office.equals(Security.getUser().get().person.office)) {
        for (int month = 1; month < 13; month++) {
          totaleOreResidue =
              totaleOreResidue + (competenceManager.positiveResidualInMonth(p, year, month) / 60);
        }
        log.debug("Ore in più per {} nell'anno {}: {}",
            new Object[]{p.getFullname(), year, totaleOreResidue});
      }

    }
    return totaleOreResidue;
  }

  /**
   * Javadoc da scrivere
   * @param file file 
   * @return la situazione dopo il check del file.
   */
  public RenderList checkSituationPastYear(File file) {
    if (file == null) {
      log.error("file nullo nella chiamata della checkSituationPastYear");
    }
    log.debug("Passato il file {}", file.getName());
    List<RenderResult> listTrueFalse = new ArrayList<RenderResult>();
    List<RenderResult> listNull = new ArrayList<RenderResult>();
    final Map<Integer, List<ResultFromFile>> map = createMap(file);
    if (map != null) {
      map.forEach((key,value)-> {
        Person person = personDao.getPersonByNumber(key);
        List<RenderResult> listForPerson = transformInRenderList(person, map.get(key));
        listForPerson.forEach(item-> {
          listTrueFalse.add(item);
        }); 
      });
    } else {
      log.warn("Problemi nella costruzione della mappa che risulta nulla.");
    }

    return new RenderList(listNull, listTrueFalse);
  }

  /**
   * @return il file contenente la situazione di ore in più, ore di straordinario e riposi
   *     compensativi per ciascuna persona della lista passata come parametro relativa all'anno
   *     year.
   */
  public FileInputStream export(Integer year, List<Person> personList) throws IOException {
    final File tempFile = File.createTempFile("straordinari" + year, ".csv");
    final FileInputStream inputStream = new FileInputStream(tempFile);
    final FileWriter writer = new FileWriter(tempFile, true);
    final BufferedWriter out = new BufferedWriter(writer);
    Integer month = null;
    LocalDate endDate = null;
    LocalDate beginDate = null;
    if (year == new LocalDate().getYear()) {
      month = new LocalDate().getMonthOfYear();
      endDate = new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue();
      beginDate = new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue();
    } else {
      month = 12;
      endDate = new LocalDate(year, 12, 31);
      beginDate = new LocalDate(year, 1, 1);
    }
    out.write("Cognome Nome,");
    for (int i = 1; i <= month; i++) {
      out.append("ore straordinari " + DateUtility.fromIntToStringMonth(i)
          + ',' + "ore riposi compensativi " + DateUtility.fromIntToStringMonth(i)
          + ',' + "ore in più " + DateUtility.fromIntToStringMonth(i) + ',');
    }

    out.append("ore straordinari TOTALI,ore riposi compensativi TOTALI, ore in più TOTALI");
    out.newLine();

    int totalOvertime = 0;
    int totalCompensatoryRest = 0;
    int totalPlusHours = 0;

    for (Person p : personList) {
      log.debug("Scrivo i dati per {}", p.getFullname());

      out.append(p.surname + ' ' + p.name + ',');
      String situazione = "";
      List<Contract> contractList = personDao.getContractList(p, beginDate, endDate);

      LocalDate beginDateaux = null;
      if (contractList.isEmpty()) {
        contractList.addAll(p.contracts);
      }

      for (Contract contract : contractList) {
        if (beginDateaux != null && beginDateaux.equals(contract.beginDate)) {
          log.error("Due contratti uguali nella stessa lista di contratti per {} : "
              + "come è possibile!?!?", p.getFullname());

        } else {
          IWrapperContract contr = wrapperFactory.create(contract);
          beginDateaux = contract.beginDate;
          YearMonth actual = new YearMonth(year, 1);
          YearMonth last = new YearMonth(year, 12);
          while (!actual.isAfter(last)) {
            Optional<ContractMonthRecap> recap = contr.getContractMonthRecap(actual);
            if (recap.isPresent()) {
              situazione = situazione
                  + (new Integer(recap.get().straordinariMinuti / 60).toString())
                  + ',' + (new Integer(recap.get().riposiCompensativiMinuti / 60).toString())
                  + ',' + (new Integer((recap.get().getPositiveResidualInMonth()
                      + recap.get().straordinariMinuti) / 60).toString())
                  + ',';
              totalOvertime = totalOvertime + new Integer(recap.get().straordinariMinuti / 60);
              totalCompensatoryRest =
                  totalCompensatoryRest + new Integer(recap.get().riposiCompensativiMinuti / 60);
              totalPlusHours =
                  totalPlusHours + new Integer((recap.get().getPositiveResidualInMonth()
                      + recap.get().straordinariMinuti) / 60);
            } else {
              situazione = situazione + ("0" + ',' + "0" + ',' + "0");
            }
            actual = actual.plusMonths(1);
          }

          out.append(situazione);
          out.append(new Integer(totalOvertime).toString() + ',');
          out.append(new Integer(totalCompensatoryRest).toString() + ',');
          out.append(new Integer(totalPlusHours).toString() + ',');
        }

      }
      totalCompensatoryRest = 0;
      totalOvertime = 0;
      totalPlusHours = 0;
      out.newLine();
    }
    out.close();
    return inputStream;
  }

  /**
   * @return la situazione in termini di ferie usate anno corrente e passato, permessi usati e
   *     residuo per la persona passata come parametro.
   */
  public FileInputStream exportDataSituation(Person person) throws IOException {
    File tempFile = File.createTempFile("esportazioneSituazioneFinale" + person.surname, ".csv");
    FileInputStream inputStream = new FileInputStream(tempFile);
    FileWriter writer = new FileWriter(tempFile, true);
    BufferedWriter out = new BufferedWriter(writer);

    out.write("Cognome Nome,Ferie usate anno corrente,Ferie usate anno passato,Permessi usati "
        + "anno corrente,Residuo anno corrente (minuti), Residuo anno passato (minuti),"
        + "Riposi compensativi anno corrente");
    out.newLine();

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    Optional<Contract> contract = wrPerson.getCurrentContract();

    Preconditions.checkState(contract.isPresent());

    Optional<VacationsRecap> vr = vacationsService.create(LocalDate.now().getYear(), 
        contract.get());

    Preconditions.checkState(vr.isPresent());

    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
        .getContractMonthRecap(new YearMonth(LocalDate.now()));

    if (!recap.isPresent()) {
      out.close();
      return inputStream;
    }

    Optional<WorkingTimeType> wtt = wrPerson.getCurrentWorkingTimeType();

    Preconditions.checkState(wtt.isPresent());

    out.append(person.surname + ' ' + person.name + ',');

    out.append(new Integer(vr.get().getVacationsCurrentYear().getUsed()).toString() + ',' 
        + new Integer(vr.get().getVacationsLastYear().getUsed()).toString() + ',' 
        + new Integer(vr.get().getPermissions().getUsed()).toString() + ',' 
        + new Integer(recap.get().remainingMinutesCurrentYear).toString() + ',' 
        + new Integer(recap.get().remainingMinutesLastYear).toString() + ',');

    int workingTime = wtt.get().workingTimeTypeDays.get(0).workingTime;

    int month = LocalDate.now().getMonthOfYear();
    int riposiCompensativiMinuti = 0;
    for (int i = 1; i <= month; i++) {
      recap = wrapperFactory.create(contract.get())
          .getContractMonthRecap(new YearMonth(LocalDate.now().getYear(), i));
      if (recap.isPresent()) {
        riposiCompensativiMinuti += recap.get().riposiCompensativiMinuti;
      }
    }
    out.append(new Integer(riposiCompensativiMinuti / workingTime).toString());

    out.close();
    return inputStream;
  }

  /**
   * Metodi privati per il calcolo da utilizzare per la restituzione al controller del dato
   * richiesto.
   **/
  private String removeApice(String token) {
    if (token.startsWith("\"")) {
      token = token.substring(1);
    }
    if (token.endsWith("\"")) {
      token = token.substring(0, token.length() - 1);
    }
    return token;
  }

  /**
   * 
   * @param token una stringa contenente i campi per costruire la data
   * @return la data.
   */
  private LocalDate buildDate(String token) {
    token = removeApice(token);
    token = token.substring(0, 10);
    String[] elements = token.split("/");
    LocalDate date =
        new LocalDate(
            Integer.parseInt(elements[2]), Integer.parseInt(elements[1]),
            Integer.parseInt(elements[0]));
    return date;
  }

  /**
   * 
   * @param line la linea del file da splittare
   * @return una lista di stringhe che sono le stringhe separate dal separatore.
   */
  private List<String> splitter(String line) {
    line = removeApice(line);
    List<String> list = new ArrayList<String>();
    boolean hasNext = true;
    while (hasNext) {
      if (line.contains("\",\"")) {
        int index = line.indexOf("\",\"");
        String aux = removeApice(line.substring(0, index));
        list.add(aux);
        line = line.substring(index + 2, line.length() - 1);
      } else {
        hasNext = false;
      }
    }
    return list;
  }

  /**
   * 
   * @param File file da cui estrapolare le informazioni sulle assenze
   * @return una mappa con chiave le matricole dei dipendenti e con valori le liste di oggetti
   *     di tipo ResultFromFile che contengono l'assenza e la data in cui l'assenza è stata presa.
   */
  @SuppressWarnings("deprecation")
  private Map<Integer, List<ResultFromFile>> createMap(File file) {
    if (file == null) {
      log.error("file nullo nella chiamata della checkSituationPastYear");
    }    

    Map<Integer, List<ResultFromFile>> map = Maps.newHashMap();
    try {
      InputStream targetStream = new FileInputStream(file);
      BufferedReader in = new BufferedReader(new InputStreamReader(targetStream));
      String line = null;

      int indexMatricola = 0;
      int indexAssenza = 0;
      int indexDataAssenza = 0;

      while ((line = in.readLine()) != null) {

        if (line.contains("Query 3")) {
          continue;
        }
        if (line.contains("Query")) {
          String[] tokens = line.split(",");
          for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith("Matricola")) {
              indexMatricola = i;
            }
            if (tokens[i].startsWith("Codice Assenza")) {
              indexAssenza = i;
            }
            if (tokens[i].startsWith("Data Assenza")) {
              indexDataAssenza = i;
            }
          }
          continue;
        }

        // condizionato sulla base di quale schedone si sta cercando di analizzare
        if (file.getName().contains("2015")) {
          line.replaceAll("\"\",\"\"", "\",\"");
          int pos = line.indexOf(",");
          line = new StringBuilder(line).insert(pos, "\"").toString();
        }        
        List<String> tokenList = splitter(line);

        try {
          int matricola = Integer.parseInt(removeApice(tokenList.get(indexMatricola)));
          String assenza = removeApice(tokenList.get(indexAssenza));
          LocalDate dataAssenza = buildDate(tokenList.get(indexDataAssenza));

          ResultFromFile result = new ResultFromFile(assenza, dataAssenza);
          List<ResultFromFile> list = map.get(matricola);
          if (list == null) {
            list = Lists.newArrayList();            
          }
          java.util.Optional<ResultFromFile> value = list
              .stream()
              .filter(r -> r.codice.equals(result.codice) 
                  && r.dataAssenza.isEqual(result.dataAssenza))              
              .findFirst();
          if (!value.isPresent()) {
            list.add(result);
            map.put(matricola, list);
          }                    

          JPAPlugin.closeTx(false);
          JPAPlugin.startTx(false);

        } catch (Exception e) {
          log.warn("La linea {} del file non è nel formato corretto per essere parsata.", line);
          continue;         
        }        
      }      
      in.close();
    } catch (Exception e) {
      log.warn("C'è del casino...");
      return null;
    }
    return map;
  }

  /**
   * 
   * @param person la persona di cui si cercano le assenze.
   * @param list la lista delle assenze con data che si vuol verificare.
   * @return una lista di RenderResult che contengono un riepilogo, assenza per assenza, 
   *     della situazione di esse sul db locale.
   */
  private List<RenderResult> transformInRenderList(Person person, List<ResultFromFile> list) {
    Map<String, AbsenceType> mappaCodiciTipi = Maps.newHashMap();
    List<RenderResult> resultList = Lists.newArrayList();
    list.forEach(item-> {

      AbsenceType abt = mappaCodiciTipi.get(item.codice);
      if (abt == null) {
        Optional<AbsenceType> absenceType = absenceTypeDao.getAbsenceTypeByCode(item.codice);
        if (absenceType.isPresent()) {
          abt = absenceType.get();          
        } else {
          abt = new AbsenceType();
          abt.code = item.codice;          
        }
        mappaCodiciTipi.put(item.codice, abt);
      }
      RenderResult result = null;
      PersonDay pd = personDayDao.getOrBuildPersonDay(person, item.dataAssenza);

      if (pd != null) {
        if (pd.absences.size() > 1) {
          final String codice = abt.code;
          java.util.Optional<Absence> value = pd.absences
              .stream()
              .filter(r ->codice.equals(r.absenceType.code)).findFirst();
          if (value.isPresent()) {
            result = new RenderResult(null, person.number, person.name, 
                person.surname, item.codice, item.dataAssenza, true, "Ok", 
                codice, CheckType.SUCCESS);
          } else {
            result = new RenderResult(null, person.number, person.name, 
                person.surname, item.codice, item.dataAssenza, false, 
                "Nessuna assenza per il giorno", null, CheckType.DANGER);
          }
        } else {
          Absence ab = absenceDao.checkAbsence(pd);
          if (ab == null) {
            result = new RenderResult(null, person.number, person.name, 
                person.surname, item.codice, item.dataAssenza, false, 
                "Nessuna assenza per il giorno", null, CheckType.DANGER);
          } else {

            if (abt.code.equalsIgnoreCase(ab.absenceType.code) 
                || abt.code.equalsIgnoreCase(ab.absenceType.certificateCode)) {
              result = new RenderResult(null, person.number, person.name, 
                  person.surname, item.codice, item.dataAssenza, true, "Ok", 
                  ab.absenceType.code, CheckType.SUCCESS);
            } else {
              result = new RenderResult(null, person.number, person.name, 
                  person.surname, item.codice, item.dataAssenza, false, 
                  "Mismatch tra assenza trovata e quella dello schedone", 
                  ab.absenceType.code, CheckType.WARNING);
            }          
          }
        }        

      } else {
        log.warn("Non esiste il personday per il giorno {}", item.dataAssenza);
      }      
      resultList.add(result);      
    });
    return resultList;
  }

  /**
   * Classi innestate che servono per la restituzione delle liste di anni e mesi per i grafici.
   **/
  public static final class Month {
    public int id;
    public String mese;

    private Month(int id, String mese) {
      this.id = id;
      this.mese = mese;
    }
  }

  public static final class Year {
    public int id;
    public int anno;

    private Year(int id, int anno) {
      this.id = id;
      this.anno = anno;
    }
  }

  /**
   * Classe per la restituzione di un oggetto al controller che contenga le liste per la verifica di
   * quanto trovato all'interno del file dello schedone.
   **/

  public static final class RenderList {
    private List<RenderResult> listNull;
    private List<RenderResult> listTrueFalse;

    private RenderList(List<RenderResult> listNull, List<RenderResult> listTrueFalse) {
      this.listNull = listNull;
      this.listTrueFalse = listTrueFalse;
    }

    public List<RenderResult> getListNull() {
      return this.listNull;
    }

    public List<RenderResult> getListTrueFalse() {
      return this.listTrueFalse;

    }
  }

  /**
   * classe privata per la restituzione del risultato relativo al processo di controllo sulle
   * assenze dell'anno passato.
   **/
  public class RenderResult {
    public String line;
    public Integer matricola;
    public String nome;
    public String cognome;
    public String codice;
    public LocalDate data;
    public boolean check;
    public String message;
    public String codiceInAnagrafica;
    public CheckType type;

    /**
     * Costruttore.
     */
    public RenderResult(
        String line, Integer matricola, String nome, String cognome, String codice,
        LocalDate data, boolean check, String message, String codiceInAnagrafica, CheckType type) {
      this.line = line;
      this.matricola = matricola;
      this.nome = nome;
      this.codice = codice;
      this.cognome = cognome;
      this.data = data;
      this.check = check;
      this.message = message;
      this.codiceInAnagrafica = codiceInAnagrafica;
      this.type = type;

    }
  }

  public class ResultFromFile {

    public String codice;
    public LocalDate dataAssenza;

    public ResultFromFile(String codice, LocalDate dataAssenza) {
      this.codice = codice;
      this.dataAssenza = dataAssenza;
    }
  }

  // *********** Fine parte di business logic ****************/

}
