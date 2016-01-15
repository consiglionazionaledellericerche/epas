package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.SecureManager;

import models.Office;
import models.Person;
import models.User;
import models.enumerate.StampTypes;
import models.exports.StampingFromClient;

import org.joda.time.LocalDateTime;

import controllers.Security;
import injection.StaticInject;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

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
                     @SuppressWarnings("rawtypes") Class actualClass,
                     Type genericType) throws Exception {

    try {

      Optional<User> user = Security.getUser();
      if (!user.isPresent()) {
        log.info("StampingFromClient: {}, {}, {}, {}, {}", name,
            annotations, value, actualClass, genericType);

        log.info("StampingFromClient: l'user non presente");
        return null;
      }
      Set<Office> offices = secureManager
          .officesBadgeReaderAllowed(Security.getUser().get());

      Person person = null;

      final JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

      Logger.debug("jsonObject = %s", jsonObject);

      StampingFromClient stamping = new StampingFromClient();

      stamping.badgeReader = Security.getUser().get().badgeReader;

      final Integer inOut = jsonObject.get("operazione").getAsInt();
      if (inOut != null) {
        stamping.inOut = inOut;
      }

      if (jsonObject.has("causale") && !jsonObject.get("causale").isJsonNull()) {
        final String causale = jsonObject.get("causale").getAsString();
        if (!Strings.isNullOrEmpty(causale)) {
          stamping.stampType = StampTypes.byCode(causale);

          if (stamping.stampType == null) {
            throw new IllegalArgumentException(String
                .format("Causale con codice %s sconosciuta.", causale));
          }

        }
      }

      if (jsonObject.has("admin") && !jsonObject.get("admin").isJsonNull()) {
        String admin = jsonObject.get("admin").getAsString();
        if (admin.equals("true")) {
          stamping.markedByAdmin = true;
        }
      }

      String matricolaFirma = jsonObject.get("matricolaFirma").getAsString();

      if (Security.getUser().get().badgeReader == null) {
        log.warn("L'user autenticato come badgeReader "
            + "non ha una istanza badgeReader valida associata.");

        return null;
      }
      person = personDao.getPersonByBadgeNumber(matricolaFirma,
          Security.getUser().get().badgeReader);

      if (person != null) {
        stamping.personId = person.id;
      }

      if (stamping.personId == null) {
        log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
            + " matricolaFirma={}. Controllare il database.", matricolaFirma);
        return null;
      }

      Integer anno = jsonObject.get("anno").getAsInt();
      Integer mese = jsonObject.get("mese").getAsInt();
      Integer giorno = jsonObject.get("giorno").getAsInt();
      Integer ora = jsonObject.get("ora").getAsInt();
      Integer minuti = jsonObject.get("minuti").getAsInt();
      if (anno != null && mese != null && giorno != null && ora != null && minuti != null) {
        LocalDateTime date = new LocalDateTime(anno, mese, giorno, ora, minuti, 0);
        stamping.dateTime = date;
      } else {
        Logger.warn("Uno dei parametri relativi alla data è risultato nullo. "
                + "Impossibile crearla. StampingFromClient: %s, %s, %s, %s, %s",
            name, annotations, value, actualClass, genericType);
        return null;
      }

      Logger.debug("Effettuato il binding, stampingFromClient = %s", stamping.toString());

      return stamping;


    } catch (Exception e) {
      Logger.error(e, "Problem during binding StampingFromClient: %s, %s, %s, %s, %s",
          name, annotations, value, actualClass, genericType);
      return null;
    }
  }
}
