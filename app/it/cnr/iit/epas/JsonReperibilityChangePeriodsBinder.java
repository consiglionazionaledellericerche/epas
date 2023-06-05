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

package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.injection.StaticInject;
import dao.PersonDao;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import org.joda.time.LocalDate;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Binder per il json relativo alle richieste di cambiamento di giorni di reperibilità.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@Global
@StaticInject
public class JsonReperibilityChangePeriodsBinder implements TypeBinder<ReperibilityPeriods> {

  @Inject
  private static PersonDao personDao;

  /**
   * Binder per il json relativo alle richieste di cambiamento di giorni di reperibilità.
   *
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   *     java.lang.String, java.lang.Class, java.lang.reflect.Type)
   *     "mail_req" : "ruberti@iit.cnr.it",  "mail_sub" : "lorenzo.rossi@iit.cnr.it",
   *     "req_start_date" : "2012-12-10",
   *     "req_end_date" :"2012-12-10", "sub_start_date" : "2012-12-10",
   *     "sub_end_date" : "2012-12-10"
   *     <p>
   *       - mail_req: mail richiedente - mail_sub: mail sostituto - req_start_date: data inizio del
   *       periodo del richiedente - req_end_date: data fine periodo richiedente - sub_start_date:
   *       data inizio del periodo del sostituto - sub_end_date: data fine del periodo del sostituto
   *     </p>
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value, Class actualClass, Type genericType)
          throws Exception {

    log.debug("binding ReperibilityChangePeriods: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    try {

      List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();

      JsonObject jsonObject = null;
      Person reqPerson = null;
      Person subPerson = null;

      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        Logger.debug("jsonObject = %s", jsonObject);

        String reqPersonEmail = jsonObject.get("mail_req").getAsString();

        // get the reperibility period for the requester
        reqPerson = personDao.byEmail(reqPersonEmail).orNull();
        log.debug("reqPerson = {}", reqPerson);
        if (reqPerson == null) {
          throw new IllegalArgumentException(
              String.format(
                  "Person with email %s not exist in the ePAS database",
                  reqPersonEmail));
        }

        LocalDate reqStartDate = new LocalDate(jsonObject.get("req_start_date").getAsString());
        LocalDate reqEndDate = new LocalDate(jsonObject.get("req_end_date").getAsString());

        log.debug("reqPersonEmail = {}, reqStartDate = {}, reqEndDate={}",
            reqPersonEmail, reqStartDate, reqEndDate);

        ReperibilityPeriod reperibilityPeriod =
            new ReperibilityPeriod(reqPerson, reqStartDate, reqEndDate);
        reperibilityPeriods.add(reperibilityPeriod);

        String subPersonEmail = jsonObject.get("mail_sub").getAsString();

        // get the reperibility period for the subtitute
        subPerson = personDao.byEmail(subPersonEmail).orNull();
        if (subPerson == null) {
          throw new IllegalArgumentException(
              String.format("Person with email %s not exist in the ePAS database",
                  subPersonEmail));
        }
        LocalDate subStartDate = new LocalDate(jsonObject.get("sub_start_date").getAsString());
        LocalDate subEndDate = new LocalDate(jsonObject.get("sub_end_date").getAsString());
        log.debug("subPersonEmail = {}, subStartDate = {}, subEndDate={}",
            subPersonEmail, subStartDate, subEndDate);

        ReperibilityPeriod subReperibilityPeriod =
            new ReperibilityPeriod(subPerson, subStartDate, subEndDate);

        reperibilityPeriods.add(subReperibilityPeriod);
      }

      log.debug("reperibilityPeriods = {}", reperibilityPeriods);

      return new ReperibilityPeriods(reperibilityPeriods);

    } catch (Exception ex) {
      log.error("Problem during binding List<ReperibilityPeriod>.", ex);
      throw ex;
    }
  }

}
