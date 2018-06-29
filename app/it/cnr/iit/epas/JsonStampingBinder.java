package it.cnr.iit.epas;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import injection.StaticInject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import models.enumerate.StampTypes;
import models.exports.StampingFromClient;
import org.joda.time.LocalDateTime;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Classe per il binding delle timbrature ricevute via Json.
 * 
 * @author cristian.
 */
@Slf4j
@Global
@StaticInject
public class JsonStampingBinder implements TypeBinder<StampingFromClient> {

  /**
   * Binding dal Json in un DTO con le info della timbratura.
   * 
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      @SuppressWarnings("rawtypes") Class actualClass, Type genericType) throws Exception {

    try {
      final JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

      log.debug("jsonObject = {}", jsonObject);

      StampingFromClient stamping = new StampingFromClient();
      
      final Integer inOut = jsonObject.get("operazione").getAsInt();
      if (inOut != null) {
        stamping.inOut = inOut;
      }

      if (jsonObject.has("causale") && !jsonObject.get("causale").isJsonNull()) {
        final String causale = jsonObject.get("causale").getAsString();
        if (!Strings.isNullOrEmpty(causale)) {
          if (StampTypes.isActive(causale)) {
            stamping.stampType = StampTypes.byCode(causale);
          } else {
            log.warn("Causale con codice {} sconosciuta.", causale);
          }
        }
      }

      if (jsonObject.has("admin") && !jsonObject.get("admin").isJsonNull()) {
        String admin = jsonObject.get("admin").getAsString();
        if ("true".equals(admin)) {
          stamping.markedByAdmin = true;
        }
      }
      
      if (jsonObject.get("terminale") != null) {
        stamping.zona = jsonObject.get("terminale").getAsString();
      } else {
        stamping.zona = null;
      }
      
      Integer anno = jsonObject.get("anno").getAsInt();
      Integer mese = jsonObject.get("mese").getAsInt();
      Integer giorno = jsonObject.get("giorno").getAsInt();
      Integer ora = jsonObject.get("ora").getAsInt();
      Integer minuti = jsonObject.get("minuti").getAsInt();

      if (anno != null && mese != null && giorno != null && ora != null && minuti != null) {
        stamping.dateTime = new LocalDateTime(anno, mese, giorno, ora, minuti, 0);
      } else {
        log.warn("Uno dei parametri relativi alla data è risultato nullo. "
                + "Impossibile crearla. StampingFromClient: {}, {}, {}, {}, {}",
            name, annotations, value, actualClass, genericType);
        return null;
      }
      if (jsonObject.get("note") != null) {
        stamping.note =  jsonObject.get("note").getAsString();  
      }
      
      if (jsonObject.has("matricolaFirma") && !jsonObject.get("matricolaFirma").isJsonNull()) {
        stamping.numeroBadge = jsonObject.get("matricolaFirma").getAsString();        
      } else {
        log.warn("Matricola non trovata per la timbratura {}", jsonObject);
      }

      log.debug("Effettuato il binding, stampingFromClient = {}", stamping);

      return stamping;
    } catch (Exception ex) {
      log.error("Problem during binding StampingFromClient: {}, {}, {}, {}, {}",
          name, annotations, value, actualClass, genericType);
      return null;
    }
  }
}
