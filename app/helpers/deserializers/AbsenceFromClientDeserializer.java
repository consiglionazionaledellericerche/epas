package helpers.deserializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import dao.PersonDao;

import injection.StaticInject;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.exports.AbsenceFromClient;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;

import javax.inject.Inject;

@Slf4j
@StaticInject
public class AbsenceFromClientDeserializer implements JsonDeserializer<AbsenceFromClient> {

  @Inject
  private static PersonDao personDao;

  /**
   * Deserializza il JSON proveniente dal SolariAbsence Client per importare le assenze romane.
   */
  @Override
  public AbsenceFromClient deserialize(JsonElement json, Type arg1,
                                       JsonDeserializationContext arg2) throws JsonParseException {

    JsonObject jsonAbsence = json.getAsJsonObject();

    AbsenceFromClient afc = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .create().fromJson(json, AbsenceFromClient.class);

    /**
     * Cercare la persona in funzione del tipo di matricolaFirma.
     * Nel campo matricolaFirma decido di riportare il valore dell'id
     * con cui viene salvata la persona sul db invece che la matricola
     */
    JsonArray tipoMatricola = jsonAbsence.getAsJsonArray("tipoMatricolaFirma");
    String matricolaFirma = jsonAbsence.get("matricolaFirma").getAsString();

    Person person = null;

    for (JsonElement je : tipoMatricola) {
      String tipo = je.getAsString();

      if (tipo.equals("matricolaCNR")) {
        Integer matricola = Integer.parseInt(matricolaFirma);
        person = personDao.getPersonByNumber(matricola);
      }

      if (person != null) {
        break;
      }
    }

    if (person == null) {
      log.warn("Impossibile trovare la persona dal Json ricevuto: matricolaFirma {}", 
          matricolaFirma);
      return null;
    }
    afc.person = person;

    return afc;
  }

}
