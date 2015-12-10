/**
 *
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;
import injection.StaticInject;
import models.Person;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * @author cristian
 */
@Global
@StaticInject
public class JsonReperibilityChangePeriodsBinder implements TypeBinder<ReperibilityPeriods> {

  @Inject
  private static PersonDao personDao;

  /**
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type) "mail_req" : "ruberti@iit.cnr.it",
   * "mail_sub" : "lorenzo.rossi@iit.cnr.it", "req_start_date" : "2012-12-10", "req_end_date" :
   * "2012-12-10", "sub_start_date" : "2012-12-10", "sub_end_date" : "2012-12-10"
   *
   * - mail_req: mail richiedente - mail_sub: mail sostituto - req_start_date: data inizio del
   * periodo del richiedente - req_end_date: data fine del periodo del richiedente - sub_start_date:
   * data inizio del periodo del sostituto - sub_end_date: data fine del periodo del sostituto
   */
  @Override
  public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {

    Logger.debug("binding ReperibilityChangePeriods: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
    try {

      List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
      Logger.debug("jsonArray = %s", jsonArray);

      JsonObject jsonObject = null;
      Person reqPerson = null;
      Person subPerson = null;

      //PersonReperibilityType reperibilityType = null;

      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        Logger.debug("jsonObject = %s", jsonObject);

        String reqPersonEmail = jsonObject.get("mail_req").getAsString();
        String subPersonEmail = jsonObject.get("mail_sub").getAsString();

        // get the reperibility period for the requester
        reqPerson = personDao.byEmail(reqPersonEmail).orNull();
        //reqPerson = Person.find("SELECT p FROM Person p WHERE p.email = ?", reqPersonEmail).first();
        Logger.debug("reqPerson = %s", reqPerson);
        if (reqPerson == null) {
          throw new IllegalArgumentException(
                  String.format("Person with email %s not exist in the ePAS database", reqPersonEmail));
        }


        LocalDate reqStartDate = new LocalDate(jsonObject.get("req_start_date").getAsString());
        LocalDate reqEndDate = new LocalDate(jsonObject.get("req_end_date").getAsString());

        Logger.debug("reqPersonEmail = %s, reqStartDate = %s, reqEndDate=%s ", reqPersonEmail, reqStartDate, reqEndDate);

        ReperibilityPeriod reperibilityPeriod = new ReperibilityPeriod(reqPerson, reqStartDate, reqEndDate);
        reperibilityPeriods.add(reperibilityPeriod);

        // get the reperibility period for the subtitute
        subPerson = personDao.byEmail(subPersonEmail).orNull();
        if (subPerson == null) {
          throw new IllegalArgumentException(
                  String.format("Person with email %s not exist in the ePAS database", subPersonEmail));
        }
        LocalDate subStartDate = new LocalDate(jsonObject.get("sub_start_date").getAsString());
        LocalDate subEndDate = new LocalDate(jsonObject.get("sub_end_date").getAsString());
        Logger.debug("subPersonEmail = %s, subStartDate = %s, subEndDate=%s ", subPersonEmail, subStartDate, subEndDate);

        ReperibilityPeriod subReperibilityPeriod = new ReperibilityPeriod(subPerson, subStartDate, subEndDate);

        reperibilityPeriods.add(subReperibilityPeriod);
      }

      Logger.debug("reperibilityPeriods = %s", reperibilityPeriods);

      return new ReperibilityPeriods(reperibilityPeriods);

    } catch (Exception e) {
      Logger.error(e, "Problem during binding List<ReperibilityPeriod>.");
      throw e;
    }
  }

}
