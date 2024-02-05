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

package helpers;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.testng.collections.Maps;
import play.Play;

/**
 * Nella configurazione ci possono essere:
 * <dl>
 *  <dt>oil.app.name</dt><dd>Nome dell'instanza OIL, utilizzato nel subject del messaggio</dd>
 *  <dt>oil.email.to</dt><dd>Indirizzo email di OIL a cui inviare le segnalazioni</dd>
 *  <dt>oil.email.subject</dt><dd>Oggetto delle segnalazioni</dd>
 *  <dt>oil.categories.selected</dt><dd>Id della categoria da selezionare come 
 *      predefinita (facoltativo)</dd>
 * </dl>
 * Comunque ci sono dei default.
*/
@Slf4j
public class OilConfig {

  /**
   * È possibile configurare l'integrazione con OIL inserendo questi parametri nella
   * configurazione del play.
   */
  private static final String OIL_APP_NAME = "oil.app.name";
  private static final String OIL_EMAIL_TO = "oil.email.to";
  private static final String OIL_EMAIL_SUBJECT = "oil.email.subject";
  private static final String OIL_CATEGORIES = "oil.categories";
  
  private static final String OIL_SELECTED_CATEGORY = "oil.categories.selected";
  private static final String OIL_DEFAULT_EMAIL_FROM_FOR_USER_REPLY = "mail.smtp.from";
  
  // default per parametri integrazione OIL
  private static final String OIL_DEFAULT_APP_NAME = "siper";
  private static final String OIL_DEFAULT_EMAIL_TO = "helpdesk.mailbox@cnr.it";
  private static final String OIL_DEFAULT_EMAIL_SUBJECT = "Segnalazione ePAS";
  private static final String OIL_DEFAULT_CATEGORIES = 
      "50:Problemi Tecnici - ePAS,51:Problemi Amministrativi - ePAS";
  private static final String OIL_DEFAULT_SELECTED_CATEGORY = "0";
  
  /**
   * Le categorie vengono lette dalla configurazione dal parametro
   * oil.categories. Il parametro può contenere la lista delle categorie suddivise
   * dal carattere ",". 
   * Ogni categoria è sua volta una stringa suddivisa dal carattere ":",
   * la prima parte corrisponde al codice della categoria, la seconda al testo visualizzato
   * all'utente per la sua selezione.
   *
   * @return una mappa contenente come chiave il codice della categoria, come valore il testo
   *        da visualizzare all'utente.
   */
  public static Map<String, String> categoryMap() {
    String oilCategories = categories();
    Map<String, String> categoryMap = Maps.newLinkedHashMap();
    if (oilCategories != null && !oilCategories.isEmpty()) {
      for (String category : oilCategories.split(",")) {
        String[] categoryFields = category.split(":");
        if (categoryFields.length == 2) {
          categoryMap.put(categoryFields[0], categoryFields[1]);
        } else {
          log.warn("Categoria di segnalazione oil %s non specificata correttamente.", category);
        }
      }
    }
    return categoryMap;
  }
  
  public static String appName() {
    return Play.configuration.getProperty(OIL_APP_NAME, OIL_DEFAULT_APP_NAME);
  }
  
  public static String categories() {
    return Play.configuration.getProperty(OIL_CATEGORIES, OIL_DEFAULT_CATEGORIES);
  }
  
  public static String selectedCategories() {
    return Play.configuration.getProperty(OIL_SELECTED_CATEGORY, OIL_DEFAULT_SELECTED_CATEGORY);
  }
  
  public static String emailSubject() {
    return Play.configuration.getProperty(OIL_EMAIL_SUBJECT, OIL_DEFAULT_EMAIL_SUBJECT);
  }
  
  public static String emailTo() {
    return Play.configuration.getProperty(OIL_EMAIL_TO, OIL_DEFAULT_EMAIL_TO);
  }
  
  public static String selectedCategory() {
    return Play.configuration.getProperty(OIL_SELECTED_CATEGORY, OIL_DEFAULT_SELECTED_CATEGORY);
  }
  
  public static String defaultEmailFromForUserReply() {
    return Play.configuration.getProperty(OIL_DEFAULT_EMAIL_FROM_FOR_USER_REPLY, "epas@iit.cnr.it");
  }
}
