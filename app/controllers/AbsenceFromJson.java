package controllers;

import dao.AbsenceDao;

import it.cnr.iit.epas.JsonPersonEmailBinder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceFromJsonManager;

import models.exports.FrequentAbsenceCode;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;

import org.joda.time.LocalDate;

import play.data.binding.As;
import play.mvc.Controller;


/**
 * curl -H "Content-Type: application/json" -X POST -d '{"emails" : [{"email" :
 *     "cristian.lucchesi@iit.cnr.it"},{"email" : "stefano.ruberti@iit.cnr.it"}]}'
 *     http://localhost:8888/absenceFromJson/absenceInPeriod .
 *
 * @author dario
 * @author arianna
 */
@Slf4j
public class AbsenceFromJson extends Controller {

  @Inject
  private static AbsenceFromJsonManager absenceFromJsonManager;
  @Inject
  private static AbsenceDao absenceDao;

  /**
   * Restuisce un json con la lista delle assenze nel periodo indicato.
   *
   * @param yearFrom anno di inizio del periodo
   * @param monthFrom mese di inizio del periodo
   * @param dayFrom giorno di inizio del periodo
   * @param yearTo anno di fine del periodo
   * @param monthTo mese di fine del periodo
   * @param dayTo giorno di fine del periodo
   * @param body il json con la lista delle email delle persone di cui prelevare le assenze
   */
  public static void absenceInPeriod(
      Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo,
      Integer dayTo, @As(binder = JsonPersonEmailBinder.class) PersonEmailFromJson body) {

    log.debug("Received personEmailFromJson {}", body);
    if (body == null) {
      badRequest();
    }
    log.debug("Entrato nel metodo getAbsenceInPeriod...");
    List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();

    LocalDate dateFrom = null;
    LocalDate dateTo = null;
    if (yearFrom != null && monthFrom != null && dayFrom != null) {
      dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
    } else {
      dateFrom = new LocalDate(yearFrom, monthFrom , dayFrom);
    }
    if (yearTo != null && monthTo != null && dayTo != null) {
      dateTo = new LocalDate(yearTo, monthTo, dayTo);
    } else {
      dateTo = new LocalDate(yearTo, monthTo, dayTo);
    }
    personsToRender = absenceFromJsonManager.getPersonForAbsenceFromJson(body, dateFrom, dateTo);

    renderJSON(personsToRender);
  }

  /**
   * Metodo esposto per ritornare la lista dei codici di assenza presi.
   */
  public static void frequentAbsence(Integer yearFrom, Integer monthFrom, Integer dayFrom,
      Integer yearTo, Integer monthTo, Integer dayTo) {

    List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();

    LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
    LocalDate dateTo = new LocalDate(yearTo, monthTo, dayTo);

    frequentAbsenceCodeList = absenceDao.getFrequentAbsenceCodeForAbsenceFromJson(dateFrom, dateTo);

    renderJSON(frequentAbsenceCodeList);
  }

}
