package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gdata.util.common.base.Preconditions;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.JsonRequestedOvertimeBinder;
import it.cnr.iit.epas.JsonRequestedPersonsBinder;

import manager.OvertimesManager;

import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonHourForOvertime;
import models.exports.OvertimesData;
import models.exports.PersonsCompetences;
import models.exports.PersonsList;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

import javax.inject.Inject;


/*
 * @autor arianna
 * 
 * Implements methods used by sist-org in order
 * to keep overtime information
 */

public class Overtimes extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static CompetenceDao competenceDao;
  @Inject
  private static OvertimesManager overtimesManager;
  @Inject
  private static CompetenceCodeDao competenceCodeDao;



  /**
   * (residuo del mese, totale residuo anno precedente, tempo disponibile x straordinario).
   */
  public static void getPersonOvertimes() {
    response.accessControl("*");

    String email = params.get("email");
    int year = Integer.parseInt(params.get("year"));
    int month = Integer.parseInt(params.get("month"));

    Logger.debug("chiamata la getPersonOvertimes() con email=%s, year=%d, month=%d", 
        email, year, month);

    // get the person with the given email
    Person person = personDao.byEmail(email).orNull();

    if (person == null) {
      notFound(String.format("Person with email = %s doesn't exist", email));
    }
    Logger.debug("Find persons %s with email %s", person.name, email);

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();

    Preconditions.checkState(contract.isPresent());

    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
            .getContractMonthRecap(new YearMonth(year, month));

    if (!recap.isPresent()) {
      // TODO:
    }

    int totaleResiduoAnnoCorrenteAFineMese = recap.get().remainingMinutesCurrentYear;
    int residuoDelMese = recap.get().progressivoFinaleMese;
    int tempoDisponibilePerStraordinari = recap.get().getPositiveResidualInMonth();
    OvertimesData personOvertimesData =
            new OvertimesData(totaleResiduoAnnoCorrenteAFineMese,
                    residuoDelMese, tempoDisponibilePerStraordinari);

    render(personOvertimesData);

  }

  /**
   * Get the amount of overtimes the supervisor has for personel distribution.
   */
  public static void getSupervisorTotalOvertimes() {
    response.accessControl("*");

    String email = params.get("email");

    Logger.debug("chiamata la getSupervisorTotalOvertimes() con email=%s", email);

    // get the person with the given email
    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      notFound(String.format("Person with email = %s doesn't exist", email));
    }
    Logger.debug("Find persons %s with email %s", person.name, email);


    PersonHourForOvertime personHourForOvertime = competenceDao.getPersonHourForOvertime(person);
    if (personHourForOvertime == null) {
      personHourForOvertime = new PersonHourForOvertime(person, 0);
    }

    Logger.debug("Trovato personHourForOvertime con person=%s, numberOfHourForOvertime=%s", 
        personHourForOvertime.person, personHourForOvertime.numberOfHourForOvertime);

    render(personHourForOvertime);
  }


  /**
   * Set the overtimes requested by the responsible.
   * @param year l'anno
   * @param month il mese
   * @param body l'oggetto in cui serializzare quel che recupero dal binder
   */
  public static void setRequestOvertime(Integer year, Integer month, 
      @As(binder = JsonRequestedOvertimeBinder.class) PersonsCompetences body) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    Logger.debug("update: Received PersonsCompetences %s", body);
    if (body == null) {
      badRequest();
    }

    overtimesManager.setRequestedOvertime(body, year, month);
  }


  /**
   * Set personnel overtimes requested by the supervisor.
   * @param hours
   * @param email
   * @throws Exception
   */
  public static void setSupervisorTotalOvertimes(Integer hours, String email) throws Exception {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
    try {
      Person person = personDao.byEmail(email).orNull();
      if (person == null) {
        throw new IllegalArgumentException(
            String.format("Person with email = %s doesn't exist", email));
      }
      Logger.debug("Find persons %s with email %s", person.name, email);

      overtimesManager.setSupervisorOvertime(person, hours);
    } catch (Exception e) {
      Logger.error(e, "Problem during findjing person with email.");
      throw e;
    }
  }

  /**
   * @author arianna crea il file PDF con il resoconto mensile delle ore di straordinario di una
   *     lista di persone identificate con l'email (portale sistorg)
   *
   *     curl -H "Content-Type: application/json" -X POST -d '[ {"email" :
   *     "stefano.ruberti@iit.cnr.it"}, { "email" : "andrea.vivaldi@iit.cnr.it"} , { "email" :
   *     "lorenzo.luconi@iit.cnr.it" } ]' http://scorpio.nic.it:9001/overtimes/exportMonthAsPDF/2013/05
   */
  public static void exportMonthAsPDF(Integer year, Integer month, 
      @As(binder = JsonRequestedPersonsBinder.class) PersonsList body) {
    response.accessControl("*");
    //response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

    Logger.debug("update: Received PersonsCompetences %s", body);
    if (body == null) {
      badRequest();
    }

    Table<String, String, Integer> overtimesMonth = 
        TreeBasedTable.<String, String, Integer>create();

    CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode("S1");
    Logger.debug("find  CompetenceCode %s con CompetenceCode.code=%s", 
        competenceCode, competenceCode.code);

    overtimesMonth = overtimesManager.buildMonthForExport(body, competenceCode, year, month);

    LocalDate today = new LocalDate();
    LocalDate firstOfMonth = new LocalDate(year, month, 1);

    renderPDF(today, firstOfMonth, overtimesMonth);
  }

}
