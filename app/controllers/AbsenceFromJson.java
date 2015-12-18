package controllers;

import dao.AbsenceDao;

import it.cnr.iit.epas.JsonPersonEmailBinder;

import manager.AbsenceFromJsonManager;

import models.exports.FrequentAbsenceCode;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


/**
 * @author dario
 * @author arianna curl -H "Content-Type: application/json" -X POST -d '{"emails" : [{"email" :
 *         "cristian.lucchesi@iit.cnr.it"},{"email" : "stefano.ruberti@iit.cnr.it"}]}'
 *         http://localhost:8888/absenceFromJson/absenceInPeriod
 */
public class AbsenceFromJson extends Controller {

  @Inject
  private static AbsenceFromJsonManager absenceFromJsonManager;
  @Inject
  private static AbsenceDao absenceDao;

  public static void absenceInPeriod(
      Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo,
      Integer dayTo, @As(binder = JsonPersonEmailBinder.class) PersonEmailFromJson body) {

    Logger.debug("Received personEmailFromJson %s", body);
    if (body == null) {
      badRequest();
    }
    Logger.debug("Entrato nel metodo getAbsenceInPeriod...");
    List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();

    LocalDate dateFrom = null;
    LocalDate dateTo = null;
    if (yearFrom != null && monthFrom != null && dayFrom != null)
      dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
    else
      dateFrom = new LocalDate(params.get("yearFrom", Integer.class), params.get("monthFrom", Integer.class), params.get("dayFrom", Integer.class));

    if (yearTo != null && monthTo != null && dayTo != null)
      dateTo = new LocalDate(yearTo, monthTo, dayTo);
    else
      dateTo = new LocalDate(params.get("yearTo", Integer.class), params.get("monthTo", Integer.class), params.get("dayTo", Integer.class));
    personsToRender = absenceFromJsonManager.getPersonForAbsenceFromJson(body, dateFrom, dateTo);

    renderJSON(personsToRender);
  }

  /**
   * Metodo esposto per ritornare la lista dei codici di assenza presi.
   */
  public static void frequentAbsence(Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo, Integer dayTo) {

    List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();

    LocalDate dateFrom = new LocalDate(params.get("yearFrom", Integer.class), params.get("monthFrom", Integer.class), params.get("dayFrom", Integer.class));
    LocalDate dateTo = new LocalDate(params.get("yearTo", Integer.class), params.get("monthTo", Integer.class), params.get("dayTo", Integer.class));

    frequentAbsenceCodeList = absenceDao.getFrequentAbsenceCodeForAbsenceFromJson(dateFrom, dateTo);

    renderJSON(frequentAbsenceCodeList);
  }

}
