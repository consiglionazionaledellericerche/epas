package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;

import helpers.jpa.ModelQuery.SimpleResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.SecureManager;
import manager.charts.ChartsManager;
import manager.recaps.charts.RenderResult;

import models.CompetenceCode;
import models.Office;
import models.Person;
import models.exports.PersonOvertime;

import org.apache.commons.compress.archivers.ArchiveException;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;



@With({Resecure.class})
@Slf4j
public class Charts extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static ChartsManager chartsManager;
  @Inject
  static SecureManager secureManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceDao competenceDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  public static void overtimeOnPositiveResidual(Integer year, Integer month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> set = Sets.newHashSet();
    set.add(office);
    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    SimpleResults<Person> people = personDao.listForCompetence(code, Optional.<String>absent(), 
        set, true, beginMonth, endMonth, Optional.<Person>absent());
    List<Person> peopleActive = people.list();

    log.debug("Dimensione attivi per straordinario: {}", peopleActive.size());

    List<CompetenceCode> codeList = chartsManager.populateOvertimeCodeList();

    List<PersonOvertime> poList = 
        chartsManager.populatePersonOvertimeList(peopleActive, codeList, year, month);
    if (poList.isEmpty()) {
      flash.error("Nel mese selezionato non sono ancora stati assegnati gli straordinari");
      render(poList);
    }

    render(poList, year, month);
  }


  public static void indexCharts() {

    render();
  }


  /**
   * metodo che ritorna il template degli straordinari calcolati sulle ore positive di residuo
   * nell'anno.
   *
   * @param year l'anno di riferimento
   */
  public static void overtimeOnPositiveResidualInYear(Integer year, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> set = Sets.newHashSet();
    set.add(office);
    LocalDate begin = new LocalDate(year, 1, 1);
    LocalDate end = begin.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue();

    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    SimpleResults<Person> people = personDao.listForCompetence(code, Optional.<String>absent(), 
        set, true, begin, end, Optional.<Person>absent());
    List<Person> peopleActive = people.list();
    List<CompetenceCode> codeList = chartsManager.populateOvertimeCodeList();

    List<PersonOvertime> poList = 
        chartsManager.populatePersonOvertimeListInYear(peopleActive, codeList, year);

    if (poList.isEmpty()) {
      flash.error("Nel mese selezionato non sono ancora stati assegnati gli straordinari");
      render(poList);
    }
    render(poList, year);

  }

  /**
   * esporta le ore e gli straordinari.
   */
  public static void exportHourAndOvertime() {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    //  List<Year> annoList = 
    //      chartsManager.populateYearList(Security.getUser().get().person.office);

    render();
  }

  public static void checkLastYearAbsences(File file, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    boolean process = false;
    if (file == null) {
      render(process, office);
    }
    process = true;
    long start = System.nanoTime();

    List<List<RenderResult>> results = await(chartsManager.checkSituationPastYear(file));
    List<RenderResult> listTrueFalse = results.stream().flatMap(Collection::stream)
        .collect(Collectors.toList());
    long end = System.nanoTime();
    log.debug("Tempo di esecuzione elaborazione schedone: {} secondi", (end - start) / 1000000000);
    render(listTrueFalse, process, office);
  }


  /**
   * metodo che compone il file .csv contenente, per ogni persona, le ore in più, gli straordinari e
   * i riposi compensativi suddivisi per mese nell'anno passato come parametro.
   *
   * @param year l'anno di riferimento
   * @throws IOException eccezione in formazione del file
   */
  public static void export(Integer year) throws IOException {
    rules.checkIfPermitted(Security.getUser().get().person.office);

    List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        true, new LocalDate(year, 1, 1), LocalDate.now(), true).list();
    Logger.debug("Esporto dati per %s persone", personList.size());
    FileInputStream inputStream = chartsManager.export(year, personList);

    renderBinary(inputStream, "straordinariOreInPiuERiposiCompensativi" + year + ".csv");
  }

  /**
   * metodo che renderizza il template per la lista delle persone di cui si può chiedere
   * l'esportazione della situazione finale in termini di residuo/assenze...
   */
  public static void exportFinalSituation() {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    Set<Office> offices = Sets.newHashSet();
    offices.add(Security.getUser().get().person.office);
    String name = null;
    List<Person> personList = personDao.list(
        Optional.fromNullable(name),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, LocalDate.now(), LocalDate.now(), true).list();
    render(personList);
  }

  /**
   * genera il file .csv contenente le informazioni finali della persona richiesta.
   *
   * @param personId l'id della persona di cui si vogliono le informazioni
   * @throws IOException eventuale eccezione generata dalla creazione del file
   */
  public static void exportDataSituation(Long personId) throws IOException {
    rules.checkIfPermitted(Security.getUser().get().person.office);

    Person person = personDao.getPersonById(personId);

    FileInputStream inputStream = chartsManager.exportDataSituation(person);
    renderBinary(inputStream, "exportDataSituation" + person.surname + ".csv");

  }

  /**
   * ritorna la lista delle persone attive nell'anno e nel mese.
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void listForExcelFile(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> set = Sets.newHashSet();
    set.add(office);
    LocalDate date = new LocalDate(year, month, 1);
    boolean forAll = true;
    List<Person> personList = personDao.list(
        Optional.<String>absent(), set, false, date, 
        date.dayOfMonth().withMaximumValue(), true).list();
    

    render(personList, date, office, forAll);
  }


  /**
   * ritorna l'esportazione dei dati per rendicontazione secondo i parametri passati.
   * @param peopleIds l'eventuale lista di id dei dipendenti di cui fare l'esportazione
   * @param exportFile il formato dell'esportazione
   * @param forAll se per tutti i dipendenti o no
   * @param beginDate l'eventuale data inizio
   * @param endDate l'eventuale data fine
   * @param officeId l'id della sede
   */
  public static void exportTimesheetSituation(List<Long> peopleIds, 
      @Required ExportFile exportFile, boolean forAll, @Required LocalDate beginDate, 
      @Required LocalDate endDate, Long officeId) {   
    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted(office);
    
    if (beginDate != null && endDate != null && !beginDate.isBefore(endDate)) {
      Validation.addError("endDate","La data di fine non può precedere la data di inizio!");      
    }
    if (Validation.hasErrors()) {      
      
      Set<Office> set = Sets.newHashSet(office);
      LocalDate date = LocalDate.now();
      List<Person> personList = personDao.list(
          Optional.<String>absent(), set, false, beginDate, 
          endDate, true).list();
      
      render("@listForExcelFile", office, exportFile,
          date, personList, forAll, beginDate, endDate);
    }
    InputStream file = null;
    try {
      file = chartsManager.buildFile(office, forAll, peopleIds, beginDate, endDate, exportFile);
    } catch ( ArchiveException | IOException ex ) {
      flash.error("Errore durante l'esportazione del tempo al lavoro");
      listForExcelFile(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear(), officeId);
      log.error("Errore durante l'esportazione del tempo al lavoro", ex);
    }
    
    renderBinary(file, "export.zip", false);
    
  }

  public enum ExportFile {
    CSV,XLS;
  }
}
