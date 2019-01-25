package it.cnr.iit.epas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import injection.StaticInject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import models.exports.MissionFromClient;
import org.joda.time.LocalDateTime;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

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
      mission.tipoMissione = jsonObject.get("tipo_missione").getAsString();
      mission.codiceSede = jsonObject.get("codice_sede").getAsInt();
      mission.id = jsonObject.get("id").getAsLong();
      mission.matricola = jsonObject.get("matricola").getAsString();
      mission.dataInizio =
          LocalDateTime.parse(getDateFromJson(jsonObject.get("data_inizio").getAsString()));
      mission.dataFine =
          LocalDateTime.parse(getDateFromJson(jsonObject.get("data_fine").getAsString()));
      if (jsonObject.get("id_ordine").isJsonNull()) {
        mission.idOrdine = null;
      } else {
        mission.idOrdine = jsonObject.get("id_ordine").getAsLong();
      }

      log.info("Effettuato il binding, MissionFromClient = {}", mission);
      return mission;
    } catch (Exception ex) {
      log.error("Problem during binding MissionFromClient: {}, {}, {}, {}, {}",
          name, annotations, value, actualClass, genericType);
      return null;
    }

  }

  /**
   * @param string la stringa contenente la data passata nel json
   * @return la sottostringa contenente la data nel formato yyyy-mm-dd.
   */
  private String getDateFromJson(String string) {
    String date = string.substring(0, 19);
    return date;
  }
}
