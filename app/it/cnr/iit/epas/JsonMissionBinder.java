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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.injection.StaticInject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import models.exports.MissionFromClient;
import org.joda.time.LocalDateTime;
import org.threeten.bp.LocalDate;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Binder per l'oggetto json proveniente da Missioni.
 *
 * @author dario
 *
 */
@Slf4j
@Global
@StaticInject
public class JsonMissionBinder implements TypeBinder<MissionFromClient> {

  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      @SuppressWarnings("rawtypes") Class actualClass, Type genericType) throws Exception {
    
    try {
      final JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
      log.debug("jsonObject = {}", jsonObject);
      MissionFromClient mission = new MissionFromClient();
      mission.destinazioneMissione = jsonObject.get("dest_missione").getAsString();
      mission.tipoMissione = jsonObject.get("tipo_missione").getAsString();
      if (jsonObject.get("codice_sede") != null) {
        mission.codiceSede = jsonObject.get("codice_sede").getAsString();
      } else {
        mission.codiceSede = "";
      }
      if (jsonObject.get("id") != null) {
        mission.id = jsonObject.get("id").getAsLong();
      }
      mission.matricola = jsonObject.get("matricola").getAsString();
      mission.dataInizio = 
          LocalDateTime.parse(getDateFromJson(jsonObject.get("data_inizio").getAsString()));
      mission.dataFine = 
          LocalDateTime.parse(getDateFromJson(jsonObject.get("data_fine").getAsString()));
      if (jsonObject.has("nel_comune_di_residenza")) {
        mission.destinazioneNelComuneDiResidenza = jsonObject.get("nel_comune_di_residenza").getAsBoolean();
      } else if (jsonObject.has("destinazione_nel_comunediresidenza")) {
        mission.destinazioneNelComuneDiResidenza = jsonObject.get("destinazione_nel_comunediresidenza").getAsBoolean();
      } else {
        mission.destinazioneNelComuneDiResidenza = null;
      }
          
      if (jsonObject.get("id_ordine") == null 
          || jsonObject.get("id_ordine").isJsonNull()) {
        mission.idOrdine = null;
      } else {
        mission.idOrdine = jsonObject.get("id_ordine").getAsLong();
      }
      if (jsonObject.get("anno") != null) {
        mission.anno = jsonObject.get("anno").getAsInt();
      } else {
        mission.anno = 
            mission.dataInizio != null ? mission.dataInizio.getYear() : LocalDate.now().getYear();
      }
      if (jsonObject.get("numero") != null) {
        mission.numero = jsonObject.get("numero").getAsLong();
      }

      log.debug("Effettuato il binding, MissionFromClient = {}", mission);
      return mission;
    } catch (Exception ex) {
      log.error("Problem during binding MissionFromClient: {}, {}, {}, {}, {}",
          name, annotations, value, actualClass, genericType, ex);
      return null;
    }
    
  }
  
  /**
   * Estrazione della data dalla stringa passata.
   *
   * @param string la stringa contenente la data passata nel json
   * @return la sottostringa contenente la data nel formato yyyy-mm-dd.
   */
  private String getDateFromJson(String string) {
    String date = string.substring(0, 19);
    return date;
  }
}
