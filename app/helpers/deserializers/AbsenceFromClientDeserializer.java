package helpers.deserializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import dao.PersonDao;

import models.Person;
import models.exports.AbsenceFromClient;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import injection.StaticInject;

import java.lang.reflect.Type;

import javax.inject.Inject;

@StaticInject
public class AbsenceFromClientDeserializer implements JsonDeserializer<AbsenceFromClient> {

  private final static Logger log = LoggerFactory.getLogger(AbsenceFromClientDeserializer.class);
  @Inject
  private static PersonDao personDao;

  @Override
  public AbsenceFromClient deserialize(JsonElement json, Type arg1,
                                       JsonDeserializationContext arg2) throws JsonParseException {

    JsonObject jAbsence = json.getAsJsonObject();

    AbsenceFromClient afc = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .create().fromJson(json, AbsenceFromClient.class);

    /**
     * Cercare la persona in funzione del tipo di matricolaFirma.
     * Nel campo matricolaFirma decido di riportare il valore dell'id
     * con cui viene salvata la persona sul db invece che la matricola
     */
    JsonArray tipoMatricola = jAbsence.getAsJsonArray("tipoMatricolaFirma");
    String matricolaFirma = jAbsence.get("matricolaFirma").getAsString();

    Person person = null;

    for (JsonElement je : tipoMatricola) {
      String tipo = je.getAsString();

      if (tipo.equals("matricolaCNR")) {
        Integer matricola = Integer.parseInt(matricolaFirma);
        person = personDao.getPersonByNumber(matricola);
      } else if (tipo.equals("matricolaBadge")) {
        String badgeNumber = matricolaFirma.replaceFirst("^0+(?!$)", "");
        person = personDao.getPersonByBadgeNumber(badgeNumber);
      } else if (tipo.equals("idTabellaINT")) {
        Long oldId = Long.parseLong(
                matricolaFirma.substring(matricolaFirma.indexOf("INT") + 3).trim());
        person = personDao.getPersonByOldID(oldId);
      }

      if (person != null) {
        break;
      }
    }

    if (person == null) {
      log.error("Impossibile trovare la persona dal Json "
              + "ricevuto: matricolaFirma {}", matricolaFirma);
      throw new JsonParseException("Persona non trovata in anagrafica");
    }
    afc.person = person;

    return afc;
  }

}
