package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import controllers.Security;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.SecureManager;

import models.Person;
import models.User;
import models.enumerate.StampTypes;
import models.exports.StampingFromClient;

import org.joda.time.LocalDateTime;

import injection.StaticInject;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;

/**
 * @author cristian.
 */
@Slf4j
@Global
@StaticInject
public class JsonStampingBinder implements TypeBinder<StampingFromClient> {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static SecureManager secureManager;

  /**
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      @SuppressWarnings("rawtypes") Class actualClass, Type genericType) throws Exception {

    try {
      Optional<User> user = Security.getUser();
      if (!user.isPresent()) {
        log.info("StampingFromClient: {}, {}, {}, {}, {}", name, annotations, value, actualClass, genericType);
        log.info("StampingFromClient: l'user non presente");
        return null;
      }
      if (user.get().badgeReader == null) {
        log.error("L'utente {} utilizzato per l'invio della timbratura" +
            " non ha una istanza badgeReader valida associata.");
        return null;
      }

      final JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

      log.debug("jsonObject = {}", jsonObject);

      StampingFromClient stamping = new StampingFromClient();

      stamping.badgeReader = user.get().badgeReader;

      String matricolaFirma = jsonObject.get("matricolaFirma").getAsString();

      final Person person = personDao.getPersonByBadgeNumber(matricolaFirma, stamping.badgeReader);

      if (person == null) {
        log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
            + " matricolaFirma={}. Controllare il database.", matricolaFirma);
        return null;
      }

      stamping.personId = person.id;

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
        if (admin.equals("true")) {
          stamping.markedByAdmin = true;
        }
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

      log.debug("Effettuato il binding, stampingFromClient = {}", stamping.toString());

      return stamping;
    } catch (Exception e) {
      log.error("Problem during binding StampingFromClient: {}, {}, {}, {}, {}",
          name, annotations, value, actualClass, genericType);
      return null;
    }
  }
}
