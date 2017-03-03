package manager.charts;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import controllers.Charts.ExportFile;

import dao.CompetenceCodeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateUtility;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import jobs.ChartJob;

import lombok.Getter;

import manager.SecureManager;
import manager.recaps.charts.RenderResult;
import manager.recaps.charts.ResultFromFile;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.VacationsRecap;

import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.User;
import models.WorkingTimeType;
import models.absences.Absence;
import models.exports.PersonOvertime;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F;

public class ChartsManager {

  private static final Logger log = LoggerFactory.getLogger(ChartsManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final PersonDao personDao;
  private final IVacationsService vacationsService;
  private final IWrapperFactory wrapperFactory;
  private final PersonStampingRecapFactory stampingsRecapFactory;
  private final SecureManager secureManager;
  private final OfficeDao officeDao;

  /**
   * Costruttore.
   *
   * @param competenceCodeDao competenceCodeDao
   * @param personDao         personDao
   * @param vacationsService  vacationsService
   * @param wrapperFactory    wrapperFactory
   */
  @Inject
  public ChartsManager(CompetenceCodeDao competenceCodeDao,
      PersonDao personDao, IVacationsService vacationsService,
      IWrapperFactory wrapperFactory, PersonStampingRecapFactory stampingsRecapFactory, 
      SecureManager secureManager, OfficeDao officeDao) {
    this.competenceCodeDao = competenceCodeDao;
    this.personDao = personDao;
    this.vacationsService = vacationsService;
    this.wrapperFactory = wrapperFactory;
    this.stampingsRecapFactory = stampingsRecapFactory;
    this.secureManager = secureManager;
    this.officeDao = officeDao;
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
    List<Person> noOvertimePeople = Lists.newArrayList();
    for (Person p : personList) {
      if (p.surname.equals("Baesso")) {
        log.debug("eccoci");
      }
      //      PersonDay pd = personDayDao.getOrBuildPersonDay(p, new LocalDate(year, month, 1));
      //      int workingTime = wrapperFactory.create(pd).getWorkingTimeTypeDay().get().workingTime;
      PersonOvertime po = new PersonOvertime();
      List<Contract> monthContracts = wrapperFactory
          .create(p).orderedMonthContracts(year, month);
      for (Contract contract : monthContracts) {
        IWrapperContract wrContract = wrapperFactory.create(contract);
        Optional<ContractMonthRecap> recap =
            wrContract.getContractMonthRecap(new YearMonth(year, month));
        if (recap.isPresent() && recap.get().getStraordinarioMinuti() != 0) {
          po.overtimeHour = recap.get().getStraordinarioMinuti() 
              / DateTimeConstants.MINUTES_PER_HOUR;
          po.positiveHourForOvertime = 
              (recap.get().getPositiveResidualInMonth())
              / DateTimeConstants.MINUTES_PER_HOUR;
          po.month = month;
          po.year = year;
          po.name = p.name;
          po.surname = p.surname;      
          poList.add(po);
        } else {
          log.debug("{} pur avendo ore in più non ha usufruito di straordinari questo mese", 
              p.fullName());
          noOvertimePeople.add(p);
        }
      }

      log.debug("Aggiunto {} {} alla lista con i suoi dati", p.name, p.surname);

    }
    return poList;
  }


  /**
   * @return la lista dei personOvertime con i valori su base annuale.
   */ 
  public List<PersonOvertime> populatePersonOvertimeListInYear(
      List<Person> personList, List<CompetenceCode> codeList, int year) {

    List<PersonOvertime> poList = Lists.newArrayList();
    for (Person p : personList) {

      PersonOvertime po = new PersonOvertime();
      List<Contract> yearContracts = wrapperFactory
          .create(p).orderedYearContracts(year);
      for (Contract contract: yearContracts) {
        IWrapperContract wrContract = wrapperFactory.create(contract);
        for (int i = 1; i < 13; i++) {
          int month = i;
          Optional<ContractMonthRecap> recap =
              wrContract.getContractMonthRecap(new YearMonth(year, month));
          if (recap.isPresent() && recap.get().getStraordinarioMinuti() != 0) {
            po.overtimeHour = po.overtimeHour 
                + recap.get().getStraordinarioMinuti() / DateTimeConstants.MINUTES_PER_HOUR;
            po.positiveHourForOvertime = po.positiveHourForOvertime 
                + (recap.get().getPositiveResidualInMonth()) / DateTimeConstants.MINUTES_PER_HOUR;
            po.year = year;
            po.name = p.name;
            po.surname = p.surname;      

          }    
        }
        if (po.overtimeHour != 0) {          
          poList.add(po);
        } else {
          log.debug("Il dipendente {} non ha effettuato ore di straordinario "
              + "nell'anno pur avendo ore in più", p.fullName());
        }
      }
    }
    return poList;
  }


  // ******* Inizio parte di business logic *********/



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
          promises.add(new ChartJob(person, map.get(key)).now());
        }
      });

    } else {
      log.warn("Problemi nella costruzione della mappa che risulta nulla.");
    }
    return F.Promise.waitAll(promises);
  }

  /**
   * @return il file contenente la situazione di ore in più, ore di straordinario e riposi
   *     compensativi per ciascuna persona della lista passata come parametro 
   *     relativa all'anno year.
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
   *        residuo per la persona passata come parametro.
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
   *        tipo ResultFromFile che contengono l'assenza e la data in cui l'assenza è stata presa.
   */
  private Map<Integer, List<ResultFromFile>> createMap(File file) {
    if (file == null) {
      log.warn("file nullo nella chiamata della checkSituationPastYear");
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
        } catch (Exception ex) {
          log.debug("La linea {} del file non è nel formato corretto per essere parsata.", line);
          continue;
        }
      }
      in.close();
    } catch (Exception ex) {
      log.error("Errori durante il parsing del file delle timbrature {}", file.getName());
      return Maps.newHashMap();
    }
    return map;
  }

  /**
   * @param person la persona di cui si cercano le assenze.
   * @param list   la lista delle assenze con data che si vuol verificare.
   * @return una lista di RenderResult che contengono un riepilogo, assenza per assenza, della
   *     situazione di esse sul db locale.
   */
  //  private List<RenderResult> transformInRenderList(Person person, List<ResultFromFile> list,
  //      boolean alsoPastYear) {
  //
  //    Long start = System.nanoTime();
  //    List<RenderResult> resultList = Lists.newArrayList();
  //    LocalDate dateFrom = null;
  //    LocalDate dateTo = list.get(list.size() - 1).dataAssenza;
  //    if (alsoPastYear) {
  //      dateFrom = list.get(0).dataAssenza;
  //    } else {
  //      dateFrom = dateTo.withYear(dateTo.getYear())
  //          .withMonthOfYear(DateTimeConstants.JANUARY).withDayOfMonth(1);
  //    }
  //
  //    List<Absence> absences = absenceDao.findByPersonAndDate(person,
  //        dateFrom, Optional.fromNullable(dateTo), Optional.<AbsenceType>absent()).list();
  //
  //    list.forEach(item -> {
  //      RenderResult result = null;
  //      List<Absence> values = absences
  //          .stream()
  //          .filter(r -> r.personDay.date.isEqual(item.dataAssenza))
  //          .collect(Collectors.toList());
  //      if (!values.isEmpty()) {
  //        Predicate<Absence> a1 = a -> a.absenceType.code.equalsIgnoreCase(item.codice)
  //            || a.absenceType.certificateCode.equalsIgnoreCase(item.codice);
  //        if (values.stream().anyMatch(a1)) {
  //          result = new RenderResult(null, person.number, person.name,
  //              person.surname, item.codice, item.dataAssenza, true, "Ok",
  //              values.stream().filter(r1 -> r1.absenceType.code.equalsIgnoreCase(item.codice)
  //                  || r1.absenceType.certificateCode.equalsIgnoreCase(item.codice))
  //              .findFirst().get().absenceType.code, CheckType.SUCCESS);
  //        } else {
  //          result = new RenderResult(null, person.number, person.name,
  //              person.surname, item.codice, item.dataAssenza, false,
  //              "Mismatch tra assenza trovata e quella dello schedone",
  //              values.stream().findFirst().get().absenceType.code, CheckType.WARNING);
  //        }
  //      } else {
  //        result = new RenderResult(null, person.number, person.name,
  //            person.surname, item.codice, item.dataAssenza, false,
  //            "Nessuna assenza per il giorno", null, CheckType.DANGER);
  //      }
  //      resultList.add(result);
  //    });
  //    Long end = System.nanoTime();
  //    log.debug("TEMPO per la persona {}: {} ms", person, (end - start) / 1000000);
  //    return resultList;
  //  }

  @Getter
  public static final class RenderChart {
    public List<PersonOvertime> personOvertime;
    public List<Person> noOvertimePeople;

    public RenderChart(List<PersonOvertime> personOvertime, List<Person> noOvertimePeople) {
      this.noOvertimePeople = noOvertimePeople;
      this.personOvertime = personOvertime;
    }
  }

  /**
   * 
   * @param user se presente determina 
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param forAll se si richiede la stampa per tutti
   * @param person la persona (opzionale) se si richiede la situazione mensile solo per una persona
   * @param beginDate la data di inizio 
   * @param endDate la data di fine
   * @param exportFile il formato in cui esportare le informazioni
   * @return un file contenente tutte le informazioni sulle ore di lavoro rispetto 
   *     ai parametri passati.
   * @throws ArchiveException eccezione in creazione dell'archivio
   * @throws IOException eccezione durante le procedure di input/output
   */
  public File buildFile(Optional<User> user, int year, int month, boolean forAll, 
      Optional<Person> person, LocalDate beginDate, LocalDate endDate, 
      ExportFile exportFile) throws ArchiveException, IOException {

    Set<Office> offices = user.isPresent() ? secureManager.officesWriteAllowed(user.get())
        : Sets.newHashSet(officeDao.getAllOffices());
    List<Person> personList = Lists.newArrayList();
    boolean isExcel = false;
    File file = null;
    // controllo che tipo di esportazione devo fare...
    if (exportFile.equals(ExportFile.CSV)) {
      file = new File("situazioneMensile" 
          + DateUtility.fromIntToStringMonth(month) + year + ".csv");
    } else {
      isExcel = true;
      file = new File("situazioneMensile" 
          + DateUtility.fromIntToStringMonth(month) + year + ".xls");
    }    
    Workbook wb = new HSSFWorkbook();
    // verifico se devo fare l'esportazione per una persona o per tutti...
    if (person.isPresent()) {
      personList.add(person.get());
      //controllo se devo fare l'esportazione per un mese specifico o per un periodo...
      if (beginDate != null && endDate != null) {
        LocalDate tempDate = beginDate;
        while (!tempDate.isAfter(endDate)) {
          PersonStampingRecap psDto = stampingsRecapFactory.create(person.get(), 
              tempDate.getYear(), tempDate.getMonthOfYear(), false);
          if (isExcel) {
            file = createFileXLSToExport(psDto, file, wb);
          } else {
            file = createFileCSVToExport(psDto);
          }          
          tempDate = tempDate.plusMonths(1);
        }
      } else {
        PersonStampingRecap psDto = stampingsRecapFactory.create(person.get(), year, month, false);
        if (isExcel) {
          file = createFileXLSToExport(psDto, file, wb);
        } else {
          file = createFileCSVToExport(psDto);
        }        
      }
    } else {
      // preparo per l'esportazione in formato zip di file .csv
      File destination = new File("situazioneMensile.zip");
      OutputStream archiveStream = new FileOutputStream(destination);;
      
      ArchiveOutputStream archive = new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);     

      personList = personDao.list(Optional.<String>absent(), offices, false, LocalDate.now(),
          LocalDate.now(), true).list();
      for (Person p : personList) {
        PersonStampingRecap psDto = stampingsRecapFactory.create(p, year, month, false);
        // controllo se devo esportare in excel...
        if (isExcel) {
          file = createFileXLSToExport(psDto, file, wb);
        } else {
          // esporto in .csv: formo il .zip da ritornare
          ZipArchiveEntry entry = new ZipArchiveEntry(psDto.person.fullName());
          file = createFileCSVToExport(psDto);
          BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
          
          try {
            archive.putArchiveEntry(entry);
            IOUtils.copy(input, archive);
            input.close();
            archive.closeArchiveEntry();
          } catch (IOException ex) {            
            ex.printStackTrace();
          }          
        }        
      }
      archive.finish();
      archiveStream.close();
      return destination;
    }    

    return file;
  }


  /**
   * 
   * @param psDto il personStampingDayRecap da cui partire per prendere le informazioni
   * @return un file di tipo csv contenente la situazione mensile.
   */
  private File createFileCSVToExport(PersonStampingRecap psDto) {
    final String newLineseparator = "\n";
    final Object [] fileHeader = {"Giorno","Ore di lavoro (hh:mm)","Assenza"};
    FileWriter fileWriter = null;
    CSVPrinter csvFilePrinter = null;
    File file = new File("situazione mensile" + psDto.person.fullName());
    try {

      fileWriter = new FileWriter(file.getName());

      CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(newLineseparator);
      csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
      csvFilePrinter.printRecord(fileHeader);

      for (PersonStampingDayRecap day : psDto.daysRecap) {
        List<String> record = Lists.newArrayList();
        record.add(day.personDay.date.toString());
        record.add(DateUtility.fromMinuteToHourMinute(day.personDay.getTimeAtWork()));
        if (!day.personDay.absences.isEmpty()) {
          String code = "";
          for (Absence abs : day.personDay.absences) {
            code = code + " " + abs.absenceType.code;                  
          }
          record.add(code);                
        } else {
          record.add(" ");
        }   
        csvFilePrinter.printRecord(record);
      }

    } catch (Exception ex) {      
      System.out.println("Error in CsvFileWriter !!!");
      ex.printStackTrace();

    } finally {
      try {
        fileWriter.flush();
        fileWriter.close();
        csvFilePrinter.close();
      } catch (IOException ex) {
        System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
        ex.printStackTrace();
      }
    }
    return file;
  }

  /**
   * 
   * @param psDto il personStampingRecap contenente le info sul mese trascorso dalla persona
   * @param file il file in cui caricare le informazioni
   * @return il file contenente la situazione mensile della persona a cui fa riferimento
   *     il personStampingRecap passato come parametro.
   */
  private File createFileXLSToExport(PersonStampingRecap psDto, File file, Workbook wb) {
    try {
      FileOutputStream out = new FileOutputStream(file);

      Sheet sheet = wb.createSheet(psDto.person.fullName());

      CellStyle cs = createHeader(wb);
      Row row = null;
      Cell cell = null;

      row = sheet.createRow(0);
      row.setHeightInPoints(30);
      for (int i = 0; i < 3; i++) {
        sheet.setColumnWidth((short) (i), (short) ((50 * 8) / ((double) 1 / 20)));
        cell = row.createCell(i);
        cell.setCellStyle(cs);
        switch (i) {
          case 0:            
            cell.setCellValue("Giorno");
            break;
          case 1:
            cell.setCellValue("Ore di lavoro (hh:mm)");
            break;
          case 2:
            cell.setCellValue("Assenza");
            break;
          default:
            break;
        }
      }
      int rownum = 1;
      CellStyle cellHoliday = createHoliday(wb);
      CellStyle cellWorkingDay = createWorkingday(wb);
      for (PersonStampingDayRecap day : psDto.daysRecap) {
        row = sheet.createRow(rownum);

        for (int cellnum = 0; cellnum < 3; cellnum++) {
          cell = row.createCell(cellnum);
          if (day.personDay.isHoliday) {
            cell.setCellStyle(cellHoliday);
          } else {
            cell.setCellStyle(cellWorkingDay);
          }
          switch (cellnum) {
            case 0:              
              cell.setCellValue(day.personDay.date.toString());              
              break;
            case 1:
              cell.setCellValue(DateUtility.fromMinuteToHourMinute(day.personDay.getTimeAtWork()));
              break;
            case 2:
              if (!day.personDay.absences.isEmpty()) {
                String code = "";
                for (Absence abs : day.personDay.absences) {
                  code = code + " " + abs.absenceType.code;                  
                }
                cell.setCellValue(code);                
              } else {
                cell.setCellValue(" ");
              }              
              break;
            default:
              break;
          }          
        }
        rownum++;
      }

      try {
        wb.write(out);
        wb.close();
        out.close();
      } catch (IOException ex) {
        log.debug("problema in chiusura stream");
        ex.printStackTrace();
      }
    } catch (FileNotFoundException ex) {
      log.debug("Problema in riconoscimento file");
      ex.printStackTrace();
    }
    return file;
  }

  /**
   * 
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createHeader(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    font.setColor( (short)0xc );
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }

  /**
   * 
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella che identifica un giorno di vacanza.
   */
  private static final CellStyle createHoliday(Workbook wb) {
    CellStyle cs = wb.createCellStyle();
    cs.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
    Font font = wb.createFont();
    font.setColor(Font.COLOR_RED);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    cs.setFont(font);    
    return cs;
  }

  /**
   * 
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella che identifica un giorno lavorativo.
   */
  private static final CellStyle createWorkingday(Workbook wb) {
    CellStyle cs = wb.createCellStyle();
    Font font = wb.createFont();
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    cs.setFont(font);
    return cs;
  }
}



