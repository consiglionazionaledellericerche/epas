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
import dao.CompetenceCodeDao;
import dao.PersonDao;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.exports.PersonsCompetences;
import play.data.binding.Global;
import play.data.binding.TypeBinder;


/**
 * Binder per il json con le richieste di straordinario.
 *
 * @author Arianna Del Soldato
 */
@Slf4j
@Global
@StaticInject
public class JsonRequestedOvertimeBinder implements TypeBinder<PersonsCompetences> {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static CompetenceCodeDao competenceCodeDao;

  /**
   * Binder per il json con le richieste di straordinario.
   *
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value, Class actualClass, Type genericType)
      throws Exception {

    log.debug("binding ReperibilityCompetence: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    try {
      List<Competence> personsCompetences = new ArrayList<Competence>();

      JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
      log.debug("jsonArray = {}", jsonArray);

      JsonObject jsonObject = null;
      Person person = null;
      String personEmail = "";

      for (JsonElement jsonElement : jsonArray) {

        jsonObject = jsonElement.getAsJsonObject();
        log.trace("jsonObject = {}", jsonObject);

        personEmail = jsonObject.get("email").getAsString();

        person = personDao.byEmail(personEmail).orNull();
        if (person == null) {
          throw new IllegalArgumentException(
              String.format("Person with email = %s doesn't exist", personEmail));
        }
        log.debug("Find persons {} with email {}", person.getName(), personEmail);

        CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode("S1");
        Competence competence = new Competence(person, competenceCode, 0, 0);
        competence.setValueApproved(jsonObject.get("ore").getAsInt());
        competence.setReason(jsonObject.get("motivazione").getAsString());

        log.debug("Letto ore = {} e motivazione = {}",
            jsonObject.get("ore").getAsInt(), jsonObject.get("motivazione").getAsString());

        personsCompetences.add(competence);
      }

      log.debug("personsCompetence = {}", personsCompetences);

      return new PersonsCompetences(personsCompetences);

    } catch (Exception ex) {
      log.error("Problem during binding List<Competence>.", ex);
      throw ex;
    }
  }

}
