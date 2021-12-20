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

package helpers.deserializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import common.injection.StaticInject;
import dao.PersonDao;
import java.lang.reflect.Type;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.exports.AbsenceFromClient;
import org.joda.time.LocalDate;

/**
 * Deserializza le assenze provenienti da client.
 *
 */
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

    /*
     * Cercare la persona in funzione del tipo di matricolaFirma.
     * Nel campo matricolaFirma decido di riportare il valore dell'id
     * con cui viene salvata la persona sul db invece che la matricola
     */
    JsonArray tipoMatricola = jsonAbsence.getAsJsonArray("tipoMatricolaFirma");
    String matricola = jsonAbsence.get("matricolaFirma").getAsString();

    Person person = null;

    for (JsonElement je : tipoMatricola) {
      String tipo = je.getAsString();

      if ("matricolaCNR".equals(tipo)) {
        person = personDao.getPersonByNumber(matricola);
      }

      if (person != null) {
        break;
      }
    }

    if (person == null) {
      log.warn("Impossibile trovare la persona dal Json ricevuto: matricolaFirma {}",
          matricola);
      return null;
    }
    afc.person = person;

    return afc;
  }

}
