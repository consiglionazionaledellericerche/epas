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

package manager.charts;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import controllers.Charts.ExportFile;
import dao.CompetenceCodeDao;
import dao.PersonDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateUtility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import jobs.ChartJob;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import manager.PersonDayManager;
import manager.recaps.charts.RenderResult;
import manager.recaps.charts.ResultFromFile;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.PairStamping;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeType;
import models.absences.Absence;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.exports.PersonOvertime;
import org.apache.commons.compress.archivers.ArchiveException;
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

/**
 * Manager per i riepiloghi.
 */
public class ChartsManager {

  private static final Logger log = LoggerFactory.getLogger(ChartsManager.class);
  private final CompetenceCodeDao competenceCodeDao;
  private final PersonDao personDao;
  private final IWrapperFactory wrapperFactory;
  private final PersonStampingRecapFactory stampingsRecapFactory;
  private final PersonDayManager personDayManager;
  private final AbsenceService absenceService;
  private final AbsenceComponentDao absenceComponentDao;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  public ChartsManager(CompetenceCodeDao competenceCodeDao, PersonDao personDao,
      IWrapperFactory wrapperFactory, PersonStampingRecapFactory stampingsRecapFactory,
      PersonDayManager personDayManager, AbsenceService absenceService, 
      AbsenceComponentDao absenceComponentDao) {
    this.competenceCodeDao = competenceCodeDao;
    this.personDao = personDao;
    this.wrapperFactory = wrapperFactory;
    this.stampingsRecapFactory = stampingsRecapFactory;
    this.personDayManager = personDayManager;
    this.absenceService = absenceService;
    this.absenceComponentDao = absenceComponentDao;
  }


  /**
   * La lista dei competenceCode che comprende tutti i codici di straordinario presenti in
   * anagrafica.
   *
   * @return la lista dei competenceCode.
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
   * Costruisce la lista dei PersonOvertime corrispondenti ai parametri passati.
   *
   * @return la lista dei personOvertime.
   */
  public List<PersonOvertime> populatePersonOvertimeList(List<Person> personList,
      List<CompetenceCode> codeList, int year, int month) {
    List<PersonOvertime> poList = Lists.newArrayList();
    List<Person> noOvertimePeople = Lists.newArrayList();
    for (Person p : personList) {
      // PersonDay pd = personDayDao.getOrBuildPersonDay(p, new LocalDate(year, month, 1));
      // int workingTime = wrapperFactory.create(pd).getWorkingTimeTypeDay().get().workingTime;
      PersonOvertime po = new PersonOvertime();
      List<Contract> monthContracts = wrapperFactory.create(p).orderedMonthContracts(year, month);
      for (Contract contract : monthContracts) {
        IWrapperContract wrContract = wrapperFactory.create(contract);
        Optional<ContractMonthRecap> recap =
            wrContract.getContractMonthRecap(new YearMonth(year, month));
        if (recap.isPresent() && recap.get().getStraordinarioMinuti() != 0) {
          po.overtimeHour =
              recap.get().getStraordinarioMinuti() / DateTimeConstants.MINUTES_PER_HOUR;
          po.positiveHourForOvertime =
              recap.get().getPositiveResidualInMonth() / DateTimeConstants.MINUTES_PER_HOUR;
          po.month = month;
          po.year = year;
          po.name = p.getName();
          po.surname = p.getSurname();
          poList.add(po);
        } else {
          log.debug("{} pur avendo ore in più non ha usufruito di straordinari questo mese",
              p.fullName());
          noOvertimePeople.add(p);
        }
      }

      log.debug("Aggiunto {} {} alla lista con i suoi dati", p.getName(), p.getSurname());

    }
    return poList;
  }


  /**
   * La lista dei personOvertime con i valori su base annuale.
   *
   * @return la lista dei personOvertime con i valori su base annuale.
   */
  public List<PersonOvertime> populatePersonOvertimeListInYear(List<Person> personList,
      List<CompetenceCode> codeList, int year) {

    List<PersonOvertime> poList = Lists.newArrayList();
    for (Person p : personList) {

      PersonOvertime po = new PersonOvertime();
      List<Contract> yearContracts = wrapperFactory.create(p).orderedYearContracts(year);
      for (Contract contract : yearContracts) {
        IWrapperContract wrContract = wrapperFactory.create(contract);
        for (int i = 1; i < 13; i++) {
          Optional<ContractMonthRecap> recap =
              wrContract.getContractMonthRecap(new YearMonth(year, i));
          if (recap.isPresent() && recap.get().getStraordinarioMinuti() != 0) {
            po.overtimeHour +=
                recap.get().getStraordinarioMinuti() / DateTimeConstants.MINUTES_PER_HOUR;
            po.positiveHourForOvertime +=
                recap.get().getPositiveResidualInMonth() / DateTimeConstants.MINUTES_PER_HOUR;
            po.year = year;
            po.name = p.getName();
            po.surname = p.getSurname();

          }
        }
        if (po.overtimeHour == 0) {
          log.debug("Il dipendente {} non ha effettuato ore di straordinario "
              + "nell'anno pur avendo ore in più", p.fullName());
        } else {
          poList.add(po);
        }
      }
    }
    return poList;
  }


  // ******* Inizio parte di business logic *********/


  /**
   * Javadoc da scrivere.
   *
   * @param file file
   * @return la situazione dopo il check del file.
   */
  public F.Promise<List<List<RenderResult>>> checkSituationPastYear(File file) {

    if (file == null) {
      log.error("file nullo nella chiamata della checkSituationPastYear");
    }
    log.debug("Passato il file {}", file.getName());

    final Map<String, List<ResultFromFile>> map = createMap(file);

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
   * Esporta in un file la situazione di ore in più, ore di straordinario e riposi
   * compensativi delle persone passate per parametro.
   *
   * @return il file contenente la situazione di ore in più, ore di straordinario e riposi
   *         compensativi per ciascuna persona della lista passata come parametro relativa all'anno
   *         year.
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
      out.append("ore straordinari " + DateUtility.fromIntToStringMonth(i) + ','
          + "ore riposi compensativi " + DateUtility.fromIntToStringMonth(i) + ',' + "ore in più "
          + DateUtility.fromIntToStringMonth(i) + ',');
    }

    out.append("ore straordinari TOTALI,ore riposi compensativi TOTALI, ore in più TOTALI");
    out.newLine();

    int totalOvertime = 0;
    int totalCompensatoryRest = 0;
    int totalPlusHours = 0;

    for (Person p : personList) {
      log.debug("Scrivo i dati per {}", p.getFullname());

      out.append(p.getSurname() + ' ' + p.getName() + ',');
      String situazione = "";
      List<Contract> contractList = personDao.getContractList(p, beginDate, endDate);

      LocalDate beginDateaux = null;
      if (contractList.isEmpty()) {
        contractList.addAll(p.getContracts());
      }

      for (Contract contract : contractList) {
        if (beginDateaux != null && beginDateaux.equals(contract.getBeginDate())) {
          log.error("Due contratti uguali nella stessa lista di contratti per {} : "
              + "come è possibile!?!?", p.getFullname());

        } else {
          IWrapperContract contr = wrapperFactory.create(contract);
          beginDateaux = contract.getBeginDate();
          YearMonth actual = new YearMonth(year, 1);
          YearMonth last = new YearMonth(year, 12);
          while (!actual.isAfter(last)) {
            Optional<ContractMonthRecap> recap = contr.getContractMonthRecap(actual);
            if (recap.isPresent()) {
              situazione = situazione
                  + recap.get().getStraordinariMinuti() / 60 + ','
                  + recap.get().getRiposiCompensativiMinuti() / 60 + ','
                  + (recap.get().getPositiveResidualInMonth() + recap.get().getStraordinariMinuti())
                  / 60
                  + ',';
              totalOvertime += recap.get().getStraordinariMinuti() / 60;
              totalCompensatoryRest += recap.get().getRiposiCompensativiMinuti() / 60;
              totalPlusHours +=
                  (recap.get().getPositiveResidualInMonth() 
                      + recap.get().getStraordinariMinuti()) / 60;
            } else {
              situazione += ("0" + ',' + "0" + ',' + "0");
            }
            actual = actual.plusMonths(1);
          }

          out.append(situazione);
          out.append(Integer.toString(totalOvertime) + ',');
          out.append(Integer.toString(totalCompensatoryRest) + ',');
          out.append(Integer.toString(totalPlusHours) + ',');
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
   * Esporta in un file la situazione in termini di ferie usate anno corrente e passato, permessi 
   * usati e residuo di una persona.
   *
   * @return la situazione in termini di ferie usate anno corrente e passato, permessi usati e
   *         residuo per la persona passata come parametro.
   */
  public FileInputStream exportDataSituation(Person person) throws IOException {
    File tempFile = 
        File.createTempFile("esportazioneSituazioneFinale" + person.getSurname(), ".csv");
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

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    Optional<ContractMonthRecap> recap =
        wrapperFactory.create(contract.get()).getContractMonthRecap(new YearMonth(LocalDate.now()));

    if (!recap.isPresent()) {
      out.close();
      return inputStream;
    }

    Optional<WorkingTimeType> wtt = wrPerson.getCurrentWorkingTimeType();

    Preconditions.checkState(wtt.isPresent());

    VacationSituation vacationSituation = absenceService.buildVacationSituation(contract.get(), 
        LocalDate.now().getYear(), vacationGroup, Optional.absent(), false);

    out.append(person.getSurname() + ' ' + person.getName() + ',');

    out.append(Integer.toString(vacationSituation.currentYear.used()) + ','
        + (vacationSituation.lastYear != null ? vacationSituation.lastYear.used() : 0) + ','
        + vacationSituation.currentYear.used() + ','
        + recap.get().getRemainingMinutesCurrentYear() + ','
        + recap.get().getRemainingMinutesLastYear() + ',');

    int workingTime = wtt.get().getWorkingTimeTypeDays().get(0).getWorkingTime();

    int month = LocalDate.now().getMonthOfYear();
    int riposiCompensativiMinuti = 0;
    for (int i = 1; i <= month; i++) {
      recap = wrapperFactory.create(contract.get())
          .getContractMonthRecap(new YearMonth(LocalDate.now().getYear(), i));
      if (recap.isPresent()) {
        riposiCompensativiMinuti += recap.get().getRiposiCompensativiMinuti();
      }
    }
    out.append(Integer.toString(riposiCompensativiMinuti / workingTime));

    out.close();
    return inputStream;
  }

  /**
   * Genera la mappa matricole-risultati dal file contenente le informazioni su assenze
   * e date in cui sono state prese.
   *
   * @param file file da parsare per il recupero delle informazioni sulle assenze
   * @return una mappa con chiave le matricole dei dipendenti e con valori le liste di oggetti di
   *         tipo ResultFromFile che contengono l'assenza e la data in cui l'assenza è stata presa.
   */
  private Map<String, List<ResultFromFile>> createMap(File file) {
    if (file == null) {
      log.warn("file nullo nella chiamata della checkSituationPastYear");
    }
    final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY H.mm.ss");
    Map<String, List<ResultFromFile>> map = Maps.newHashMap();
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

          String matricola = elements.get(indexMatricola);
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
   * DTO utilizzato nel template per mostrare il resoconto.
   */
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
   * Genera un file contenente tutte le informazioni sulle ore di lavoro rispetto ai parametri
   * passati.
   *
   * @param forAll se si richiede la stampa per tutti
   * @param peopleIds la lista degli id delle persone selezionate per essere esportate
   * @param beginDate la data di inizio
   * @param endDate la data di fine
   * @param exportFile il formato in cui esportare le informazioni
   * @return un file contenente tutte le informazioni sulle ore di lavoro rispetto ai parametri
   *         passati.
   * @throws ArchiveException eccezione in creazione dell'archivio
   * @throws IOException eccezione durante le procedure di input/output
   */
  public InputStream buildFile(Office office, boolean forAll, boolean onlyMission,
      List<Long> peopleIds, LocalDate beginDate, LocalDate endDate, ExportFile exportFile)
      throws ArchiveException, IOException {

    Set<Office> offices = Sets.newHashSet(office);
    List<Person> personList;
    if (!forAll) {
      personList = peopleIds.stream().map(item -> personDao.getPersonById(item))
          .collect(Collectors.toList());
    } else {
      personList = personDao.list(
          Optional.<String>absent(), offices, false, beginDate, 
          endDate, true).list();
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];

    File file = null;
    // controllo che tipo di esportazione devo fare...
    if (exportFile == ExportFile.CSV) {

      for (Person person : personList) {
        LocalDate tempDate = beginDate;
        while (!tempDate.isAfter(endDate)) {
          PersonStampingRecap psDto = stampingsRecapFactory.create(person, tempDate.getYear(),
              tempDate.getMonthOfYear(), false);
          file = createFileCsvToExport(psDto, onlyMission);
          // preparo lo stream da inviare al chiamante...
          FileInputStream in = new FileInputStream(file);
          try {
            zos.putNextEntry(new ZipEntry(file.getName()));
            int length;
            while ((length = in.read(buffer)) > 0) {
              zos.write(buffer, 0, length);
            }
          } catch (IOException ex) {
            ex.printStackTrace();
          }
          in.close();
          file.delete();
          tempDate = tempDate.plusMonths(1);
        }
      }
      zos.closeEntry();
      zos.close();
    } else {
      // genero il file excel...
      file = File.createTempFile(
          "situazioneMensileDa" + DateUtility.fromIntToStringMonth(beginDate.getMonthOfYear())
              + beginDate.getYear() + "A"
              + DateUtility.fromIntToStringMonth(endDate.getMonthOfYear()) + endDate.getYear(),
          ".xls");

      Workbook wb = new HSSFWorkbook();
      // scorro la lista delle persone per cui devo fare l'esportazione...
      for (Person person : personList) {
        LocalDate tempDate = beginDate;
        while (!tempDate.isAfter(endDate)) {
          PersonStampingRecap psDto = stampingsRecapFactory.create(person, tempDate.getYear(),
              tempDate.getMonthOfYear(), false);
          // aggiorno il file aggiungendo un nuovo foglio per ogni persona...
          file = createFileXlsToExport(psDto, file, wb, onlyMission);
          tempDate = tempDate.plusMonths(1);
        }
      }
      // faccio lo stream da inviare al chiamante...
      FileInputStream in = new FileInputStream(file);
      try {
        zos.putNextEntry(new ZipEntry(file.getName()));
        int length;
        while ((length = in.read(buffer)) > 0) {
          zos.write(buffer, 0, length);
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      in.close();
      file.delete();
      zos.closeEntry();
      zos.close();

    }
    return new ByteArrayInputStream(out.toByteArray());
  }


  /**
   * Intestazioni per il report con i riepiloghi mensili ore e assenze.
   */
  @RequiredArgsConstructor
  public enum PersonStampingDayRecapHeader {
    Data("Data"), 
    Lavoro_da_timbrature("Lavoro da timbrature (hh:mm)"), 
    Lavoro_fuori_sede("Lavoro fuori sede (hh:mm)"), 
    Lavoro_effettivo("Lavoro effettivo (hh:mm)"), 
    Ore_giustificate_da_assenza("Ore giustificate da assenza"), 
    Codici_di_assenza_che_giustificano_ore("Codici di assenza che giustificano ore"),
    Codici_di_assenza("Tutti i codici di assenza");

    @Getter
    private final String description;

    /**
     * Lista delle label delle intestazioni.
     */
    public static List<String> getLabels() {
      return Stream.of(values()).map(v -> v.description).collect(Collectors.toList());
    }
  }

  /**
   * Crea il file csv con la situazione mensile.
   *
   * @param psDto il personStampingDayRecap da cui partire per prendere le informazioni
   * @return un file di tipo csv contenente la situazione mensile.
   */
  private File createFileCsvToExport(PersonStampingRecap psDto, boolean onlyMission)
      throws IOException {
    final String newLineseparator = "\n";
    // final Object [] fileHeader = {"Giorno","Ore di lavoro (hh:mm)","Assenza"};
    FileWriter fileWriter = null;
    CSVPrinter csvFilePrinter = null;
    File file = new File("situazione_mensile" + psDto.person.getSurname() + '_' 
        + psDto.person.getName() + '_'
        + DateUtility.fromIntToStringMonth(psDto.month) + ".csv");
    try {

      fileWriter = new FileWriter(file.getName());

      CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(newLineseparator);
      csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
      csvFilePrinter.printRecord(PersonStampingDayRecapHeader.getLabels());

      for (PersonStampingDayRecap day : psDto.daysRecap) {
        List<String> record = Lists.newArrayList();
        record.add(day.personDay.getDate().toString());
        record.add(DateUtility.fromMinuteToHourMinute(day.personDay.getStampingsTime()));
        record.add(DateUtility.fromMinuteToHourMinute(getOutOfOfficeTime(day.personDay)));

        java.util.Optional<Absence> mission =
            day.personDay.getAbsences().stream().filter(a -> a.getAbsenceType() != null
                && a.getAbsenceType().getCode() != null 
                && a.getAbsenceType().getCode().equals("92")).findFirst();

        if (onlyMission && mission.isPresent()) {
          record.add(DateUtility.fromMinuteToHourMinute(day.wttd.get().getWorkingTime()));
        } else {
          record.add(DateUtility.fromMinuteToHourMinute(day.personDay.getTimeAtWork()));
        }

        int justifiedTime = 0;
        for (Absence abs : day.personDay.getAbsences()) {
          justifiedTime += abs.justifiedTime();
        }
        record.add(DateUtility.fromMinuteToHourMinute(justifiedTime));
        // Lista dei codici che giustificano ore al lavoro, concatenati da ;
        record.add(
            Joiner.on(";")
              .join(day.personDay.getAbsences().stream().filter(a -> a.justifiedTime() > 0)
              .map(a -> a.getAbsenceType().getCode()).collect(Collectors.toList())));

        // Lista di tutti i codici di assenza ;
        record.add(
            Joiner.on(";").join(day.personDay.getAbsences().stream()
                .map(a -> a.getAbsenceType().getCode()).collect(Collectors.toList())));

        csvFilePrinter.printRecord(record);
      }

    } catch (Exception ex) {
      log.error("Error in CsvFileWriter !!!");
      ex.printStackTrace();

    } finally {
      try {
        fileWriter.flush();
        fileWriter.close();
        csvFilePrinter.close();
      } catch (IOException ex) {
        log.error("Error while flushing/closing fileWriter/csvPrinter !!!");
        ex.printStackTrace();
      }
    }
    return file;
  }

  /**
   * Genera il file contenente la situazione mensile della persona a cui fa riferimento il
   * personStampingRecap passato come parametro.
   *
   * @param psDto il personStampingRecap contenente le info sul mese trascorso dalla persona
   * @param file il file in cui caricare le informazioni
   * @return il file contenente la situazione mensile della persona a cui fa riferimento il
   *         personStampingRecap passato come parametro.
   */
  private File createFileXlsToExport(PersonStampingRecap psDto, File file, Workbook wb,
      boolean onlyMission) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      Sheet sheet = null;
      String fullname = "";
      if (psDto.person.getName().contains(" ")) {
        String[] names = psDto.person.getName().split(" ");
        fullname = psDto.person.getSurname() + " " + names[0];
      } else {
        fullname = psDto.person.fullName();
      }
      String sheetname = fullname + "_"
          + DateUtility.fromIntToStringMonth(psDto.month) + psDto.year;
      
      if (sheetname != null && sheetname.length() > 31) {
        sheetname = sheetname.substring(0, 31);
      }
      sheet = wb.createSheet(sheetname); 

      CellStyle cs = createHeader(wb);
      Row row = null;
      Cell cell = null;

      row = sheet.createRow(0);
      row.setHeightInPoints(30);
      for (int i = 0; i < 7; i++) {
        sheet.setColumnWidth((short) i, (short) ((50 * 8) / ((double) 1 / 20)));
        cell = row.createCell(i);
        cell.setCellStyle(cs);
        switch (i) {
          case 0:
            cell.setCellValue(PersonStampingDayRecapHeader.Data.getDescription());
            break;
          case 1:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_da_timbrature.getDescription());
            break;
          case 2:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_fuori_sede.getDescription());
            break;
          case 3:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_effettivo.getDescription());
            break;
          case 4:
            cell.setCellValue(
                PersonStampingDayRecapHeader.Ore_giustificate_da_assenza.getDescription());
            break;
          case 5:
            cell.setCellValue(PersonStampingDayRecapHeader.Codici_di_assenza_che_giustificano_ore
                .getDescription());
            break;
          case 6:
            cell.setCellValue(PersonStampingDayRecapHeader.Codici_di_assenza
                .getDescription());            
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

        for (int cellnum = 0; cellnum < 7; cellnum++) {
          cell = row.createCell(cellnum);
          if (day.personDay.isHoliday()) {
            cell.setCellStyle(cellHoliday);
          } else {
            cell.setCellStyle(cellWorkingDay);
          }
          switch (cellnum) {
            case 0:
              cell.setCellValue(day.personDay.getDate().toString());
              break;
            case 1:
              cell.setCellValue(
                  DateUtility.fromMinuteToHourMinute(day.personDay.getStampingsTime()));
              break;
            case 2:
              cell.setCellValue(
                  DateUtility.fromMinuteToHourMinute(getOutOfOfficeTime(day.personDay)));
              break;
            case 3:
              cell.setCellValue(DateUtility.fromMinuteToHourMinute(day.personDay.getTimeAtWork()));
              break;
            case 4:
              if (!day.personDay.getAbsences().isEmpty()) {
                String code = "";
                int justifiedTime = 0;
                for (Absence abs : day.personDay.getAbsences()) {
                  code = code + " " + abs.getAbsenceType().getCode();
                  justifiedTime += abs.justifiedTime();
                }
                cell.setCellValue(DateUtility.fromMinuteToHourMinute(justifiedTime));

                if (onlyMission && code != null 
                    && (code.trim().equals("92") || code.trim().equals("92E"))) {
                  cell = row.getCell(3);
                  cell.setCellValue(DateUtility.fromMinuteToHourMinute(day.wttd.get()
                      .getWorkingTime()));
                }
              } else {
                cell.setCellValue("00:00");
              }
              break;
            case 5:
              cell.setCellValue(Joiner.on(";")
                  .join(day.personDay.getAbsences().stream().filter(a -> a.justifiedTime() > 0)
                      .map(a -> a.getAbsenceType().getCode()).collect(Collectors.toList())));
              break;
            case 6:
              cell.setCellValue(Joiner.on(";")
                  .join(day.personDay.getAbsences().stream()
                      .map(a -> a.getAbsenceType().getCode()).collect(Collectors.toList())));
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
        log.error("problema in chiusura stream");
        ex.printStackTrace();
      }
    } catch (IllegalArgumentException | FileNotFoundException ex) {
      log.error("Problema in riconoscimento file");
      ex.printStackTrace();
    }
    return file;
  }

  /**
   * Genera lo stile delle celle di intestazione.
   *
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createHeader(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    font.setColor((short) 0xc);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }

  /**
   * Genera lo stile per una cella di giorno di vacanza.
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
   * Genera lo stile per una cella di un giorno lavorativo.
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

  /**
   * Genera il tempo derivante da timbrature per lavoro fuori sede.
   *
   * @param pd il personday
   * @return il tempo a lavoro derivante dalle timbrature identificate come lavoro fuori sede.
   */
  private int getOutOfOfficeTime(PersonDay pd) {
    List<Stamping> stampings = pd.getStampings().stream()
        .filter(st -> st.getStampType() != null && (st.getStampType().getIdentifier().equals("sf")
            || st.getStampType().getIdentifier().equals("lfs")))
        .collect(Collectors.toList());
    if (stampings.isEmpty()) {
      return 0;
    } else {
      List<PairStamping> valid = personDayManager.getValidPairStampings(stampings);
      return valid.stream().mapToInt(
          pair -> DateUtility.toMinute(pair.second.getDate()) 
          - DateUtility.toMinute(pair.first.getDate()))
          .sum();
    }

  }
}