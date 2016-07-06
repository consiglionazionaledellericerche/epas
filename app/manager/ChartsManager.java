package manager;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdata.util.common.base.Preconditions;

import controllers.Security;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateUtility;

import jobs.chartJob;

import manager.recaps.charts.RenderResult;
import manager.recaps.charts.ResultFromFile;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.Absence;
import models.AbsenceType;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.enumerate.CheckType;
import models.exports.PersonOvertime;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class ChartsManager {

  private static final Logger log = LoggerFactory.getLogger(ChartsManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final CompetenceManager competenceManager;
  private final PersonDao personDao;
  private final AbsenceDao absenceDao;

  private final IVacationsService vacationsService;
  private final IWrapperFactory wrapperFactory;


  /**
   * Costruttore.
   *
   * @param competenceCodeDao competenceCodeDao
   * @param competenceDao     competenceDao
   * @param competenceManager competenceManager
   * @param personDao         personDao
   * @param vacationsService  vacationsService
   * @param absenceDao        absenceDao
   * @param wrapperFactory    wrapperFactory
   */
  @Inject
  public ChartsManager(CompetenceCodeDao competenceCodeDao,
      CompetenceDao competenceDao, CompetenceManager competenceManager,
      PersonDao personDao, IVacationsService vacationsService,
      AbsenceDao absenceDao, IWrapperFactory wrapperFactory) {
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.personDao = personDao;
    this.absenceDao = absenceDao;
    this.vacationsService = vacationsService;
    this.wrapperFactory = wrapperFactory;
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
   * anagrafica.
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
   * della lista personeProva.
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
   *
   * @param file file
   * @return la situazione dopo il check del file.
   */
  public F.Promise<List<List<RenderResult>>> checkSituationPastYear(File file) {

    if (file == null) {
      log.error("file nullo nella chiamata della checkSituationPastYear");
    }
    log.debug("Passato il file {}", file.getName());

    final Map<Integer, List<ResultFromFile>> map = createMap(file);

    final List<F.Promise<List<RenderResult>>> promises = Lists.newArrayList();
    if (map != null && !map.isEmpty()) {
      map.forEach((key, value) -> {
        final Person person = personDao.getPersonByNumber(key);
        if (person != null) {
          promises.add(new chartJob(person, map.get(key)).now());
        }
      });

    } else {
      log.warn("Problemi nella costruzione della mappa che risulta nulla.");
    }
    return F.Promise.waitAll(promises);
  }

  /**
   * @return il file contenente la situazione di ore in più, ore di straordinario e riposi
   * compensativi per ciascuna persona della lista passata come parametro relativa all'anno year.
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
   * residuo per la persona passata come parametro.
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
   * @param file file daparsare per il recupero delle informazioni sulle assenze
   * @return una mappa con chiave le matricole dei dipendenti e con valori le liste di oggetti di
   * tipo ResultFromFile che contengono l'assenza e la data in cui l'assenza è stata presa.
   */
  private Map<Integer, List<ResultFromFile>> createMap(File file) {
    if (file == null) {
      log.error("file nullo nella chiamata della checkSituationPastYear");
    }
    final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY H.mm.ss");
    Map<Integer, List<ResultFromFile>> map = Maps.newHashMap();
    try {
      InputStream targetStream = new FileInputStream(file);
      BufferedReader in = new BufferedReader(new InputStreamReader(targetStream));
      String line;

      int indexMatricola = 0;
      int indexAssenza = 0;
      int indexDataAssenza = 0;

      while ((line = in.readLine()) != null) {

        if (line.isEmpty() || line.contains("Query 3")) {
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

        List<String> elements = Splitter.on(",").trimResults(CharMatcher.is('"')).splitToList(line);

        try {

          int matricola = Integer.parseInt(elements.get(indexMatricola));
          String assenza = elements.get(indexAssenza);
          LocalDate dataAssenza = LocalDate.parse(elements.get(indexDataAssenza), dtf);

          ResultFromFile result = new ResultFromFile(assenza, dataAssenza);
          List<ResultFromFile> list = map.get(matricola);
          if (list == null) {
            list = Lists.newArrayList();
          }
          list.add(result);
          map.put(matricola, list);
        } catch (Exception e) {
          log.debug("La linea {} del file non è nel formato corretto per essere parsata.", line);
          continue;
        }
      }
      in.close();
    } catch (Exception e) {
      log.error("Errori durante il parsing del file delle timbrature {}", file.getName());
      return Maps.newHashMap();
    }
    return map;
  }

  /**
   * @param person la persona di cui si cercano le assenze.
   * @param list   la lista delle assenze con data che si vuol verificare.
   * @return una lista di RenderResult che contengono un riepilogo, assenza per assenza, della
   * situazione di esse sul db locale.
   */
  private List<RenderResult> transformInRenderList(Person person, List<ResultFromFile> list,
      boolean alsoPastYear) {

    Long start = System.nanoTime();
    List<RenderResult> resultList = Lists.newArrayList();
    LocalDate dateFrom = null;
    LocalDate dateTo = list.get(list.size() - 1).dataAssenza;
    if (alsoPastYear) {
      dateFrom = list.get(0).dataAssenza;
    } else {
      dateFrom = dateTo.withYear(dateTo.getYear())
          .withMonthOfYear(DateTimeConstants.JANUARY).withDayOfMonth(1);
    }

    List<Absence> absences = absenceDao.findByPersonAndDate(person,
        dateFrom, Optional.fromNullable(dateTo), Optional.<AbsenceType>absent()).list();

    list.forEach(item -> {
      RenderResult result = null;
      List<Absence> values = absences
          .stream()
          .filter(r -> r.personDay.date.isEqual(item.dataAssenza))
          .collect(Collectors.toList());
      if (!values.isEmpty()) {
        Predicate<Absence> a1 = a -> a.absenceType.code.equalsIgnoreCase(item.codice)
            || a.absenceType.certificateCode.equalsIgnoreCase(item.codice);
        if (values.stream().anyMatch(a1)) {
          result = new RenderResult(null, person.number, person.name,
              person.surname, item.codice, item.dataAssenza, true, "Ok",
              values.stream().filter(r1 -> r1.absenceType.code.equalsIgnoreCase(item.codice)
                  || r1.absenceType.certificateCode.equalsIgnoreCase(item.codice))
                  .findFirst().get().absenceType.code, CheckType.SUCCESS);
        } else {
          result = new RenderResult(null, person.number, person.name,
              person.surname, item.codice, item.dataAssenza, false,
              "Mismatch tra assenza trovata e quella dello schedone",
              values.stream().findFirst().get().absenceType.code, CheckType.WARNING);
        }
      } else {
        result = new RenderResult(null, person.number, person.name,
            person.surname, item.codice, item.dataAssenza, false,
            "Nessuna assenza per il giorno", null, CheckType.DANGER);
      }
      resultList.add(result);
    });
    Long end = System.nanoTime();
    log.debug("TEMPO per la persona {}: {} ms", person, (end - start) / 1000000);
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

}

// *********** Fine parte di business logic ****************/

