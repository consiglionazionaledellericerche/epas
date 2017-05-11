package it.cnr.iit.epas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.CompetenceCodeDao;
import dao.PersonDao;

import injection.StaticInject;

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
 * @author arianna
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
        log.debug("Find persons {} with email {}", person.name, personEmail);

        CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode("S1");
        Competence competence = new Competence(person, competenceCode, 0, 0);
        competence.setValueApproved(
            jsonObject.get("ore").getAsInt(), jsonObject.get("motivazione").getAsString());

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
