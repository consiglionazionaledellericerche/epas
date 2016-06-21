package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.CompetenceDao;
import dao.PersonDao;

import manager.ChartsManager;
import manager.ChartsManager.Month;
import manager.ChartsManager.RenderList;
import manager.ChartsManager.RenderResult;
import manager.ChartsManager.Year;
import manager.SecureManager;

import models.CompetenceCode;
import models.Office;
import models.Person;
import models.exports.PersonOvertime;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

//import play.Logger;

@With({Resecure.class})
public class Charts extends Controller {

  //private final static Logger log = LoggerFactory.getLogger(Charts.class);

  @Inject
  private static SecurityRules rules;
  @Inject
  private static ChartsManager chartsManager;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static CompetenceDao competenceDao;

  public static void overtimeOnPositiveResidual(Integer year, Integer month) {

    rules.checkIfPermitted(Security.getUser().get().person.office);

    List<Year> annoList = chartsManager.populateYearList(Security.getUser().get().person.office);
    List<Month> meseList = chartsManager.populateMonthList();

    if (params.get("yearChart") == null || params.get("monthChart") == null) {

      Logger.info("Params year: %s", params.get("yearChart", Integer.class));
      Logger.info("Chiamato metodo con anno e mese nulli");
      render(annoList, meseList);
    }

    year = params.get("yearChart", Integer.class);
    month = params.get("monthChart", Integer.class);

    List<Person> personeProva = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        true, new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true).list();

    List<CompetenceCode> codeList = chartsManager.populateOvertimeCodeList();
    List<PersonOvertime> poList =
        chartsManager.populatePersonOvertimeList(personeProva, codeList, year, month);

    render(poList, year, month, annoList, meseList);
  }


  public static void indexCharts() {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    render();
  }


  /**
   * metodo che ritorna il template degli straordinari calcolati sulle ore positive di residuo
   * nell'anno.
   *
   * @param year l'anno di riferimento
   */
  public static void overtimeOnPositiveResidualInYear(Integer year) {

    rules.checkIfPermitted(Security.getUser().get().person.office);
    List<Year> annoList = chartsManager.populateYearList(Security.getUser().get().person.office);

    if (params.get("yearChart") == null && year == null) {
      Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
      Logger.debug("Chiamato metodo con anno e mese nulli");
      render(annoList);
    }
    year = params.get("yearChart", Integer.class);
    Logger.debug("Anno preso dai params: %d", year);

    List<CompetenceCode> codeList = chartsManager.populateOvertimeCodeList();
    Long val = null;
    Optional<Integer> result =
        competenceDao.valueOvertimeApprovedByMonthAndYear(
            year, Optional.<Integer>absent(), Optional.<Person>absent(), codeList);
    if (result.isPresent()) {
      val = result.get().longValue();
    }
    List<Person> personeProva = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        true, new LocalDate(year, 1, 1), new LocalDate(year, 12, 31), true).list();
    int totaleOreResidue = chartsManager.calculateTotalResidualHour(personeProva, year);

    render(annoList, val, totaleOreResidue);

  }

  public static void whichAbsenceInYear(Integer year) {

    rules.checkIfPermitted(Security.getUser().get().person.office);
    List<Year> annoList = chartsManager.populateYearList(Security.getUser().get().person.office);


    if (params.get("yearChart") == null && year == null) {
      Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
      Logger.debug("Chiamato metodo con anno e mese nulli");
      render(annoList);
    }

    year = params.get("yearChart", Integer.class);
    Logger.debug("Anno preso dai params: %d", year);


    List<String> absenceCode = Lists.newArrayList();
    absenceCode.add("92");
    absenceCode.add("91");
    absenceCode.add("111");
    //LocalDate beginYear = new LocalDate(year, 1,1);
    //LocalDate endYear = beginYear.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()
    // FIXME da rifattorizzare tutta questa parte e renderla funzione dell'office
    long missioniSize = 0; //absenceDao.howManyAbsenceInPeriod(beginYear, endYear, "92")
    long riposiCompensativiSize = 0; //absenceDao.howManyAbsenceInPeriod(beginYear, endYear, "91")
    long malattiaSize = 0; //absenceDao.howManyAbsenceInPeriod(beginYear, endYear, "111")
    long altreSize = 0; //absenceDao.howManyAbsenceInPeriodNotInList(beginYear, endYear,absenceCode)

    render(annoList, missioniSize, riposiCompensativiSize, malattiaSize, altreSize);

  }


  public static void checkLastYearAbsences() {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    render();
  }


  /**
   * restituisce la lista delle assenze che combaciano e no rispetto allo schedone.
   *
   * @param file il file dello schedone
   */
  public static void processLastYearAbsences(Blob file) {

    rules.checkIfPermitted(Security.getUser().get().person.office);

    RenderList render = chartsManager.checkSituationPastYear(file);
    List<RenderResult> listTrueFalse = render.getListTrueFalse();
    List<RenderResult> listNull = render.getListNull();

    render(listTrueFalse, listNull);
  }

  /**
   * esporta le ore e gli straordinari.
   */
  public static void exportHourAndOvertime() {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    List<Year> annoList = chartsManager.populateYearList(Security.getUser().get().person.office);

    render(annoList);
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
}
