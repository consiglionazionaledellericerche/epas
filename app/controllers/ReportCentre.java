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

package controllers;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;
import controllers.Resecure.NoCheck;
import dao.GeneralSettingDao;
import dao.UserDao;
import helpers.OilConfig;
import helpers.Web;
import helpers.deserializers.ImageToByteArrayDeserializer;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.services.helpdesk.HelpdeskServiceManager;
import models.User;
import models.exports.ReportData;
import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Util;

/**
 * Classi di supporto per l'invio delle segnalazioni utente.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class ReportCentre extends Controller {

  @Inject
  static UserDao userDao;

  @Inject
  static GeneralSettingDao generalSettingDao;
  
  @Inject
  static HelpdeskServiceManager helpdeskServiceManager;

  /**
   * Renderizza il javascript del feedback.js.
   */
  @NoCheck
  public static void javascript() {
    response.contentType = MediaType.JAVASCRIPT_UTF_8.toString();
    response.setHeader("Cache-Control", "max-age=" + 31536000);
    response.setHeader("Expires", LocalDateTime.now().plusYears(1).toString());

    Map<String, String> categoryMap = OilConfig.categoryMap();
    //Se sono presenti le categorie è obbligatorio per l'utente selezionarle
    String selectedCategory = OilConfig.selectedCategory();

    render("/feedback.js", categoryMap, selectedCategory);
  }

  /**
   * Invia un report via email leggendo la segnalazione via post json.
   */
  public static void sendReport() {
    final ReportData data = new GsonBuilder()
        .registerTypeHierarchyAdapter(byte[].class,
            new ImageToByteArrayDeserializer()).create()
        .fromJson(new InputStreamReader(request.body), ReportData.class);

    val generalSettings = generalSettingDao.generalSetting();
    //Questo è il caso di invio delle segnalazioni tramite il servizio esterno.
    if (generalSettings.isEpasHelpdeskServiceEnabled() && generalSettings.getEpasHelpdeskServiceUrl() != null) {
      helpdeskServiceManager.sendReport(data);
      return;
    }

    final Optional<User> currentUser = Security.getUser();

    if ("true".equals(Play.configuration.getProperty("oil.enabled")) && currentUser.isPresent()) {
      if (userDao.hasAdminRoles(currentUser.get())) {
        OilMailer.sendFeedbackToOil(data, session, currentUser.get());
        log.info("Inviata segnalazione ad OIL. Utente {}. Categoria: '{}'. Url: {}. Note: {}", 
            currentUser.get().getUsername(), OilConfig.categoryMap().get(data.getCategory()), 
            data.getUrl(), data.getNote());
      } else {
        ReportMailer.feedback(data, session, currentUser);
      }
    } else {
      ReportMailer.feedback(data, session, currentUser);
    }
  }

  /**
   * Form per l'invio di ulteriori informazioni relativemante ad un ticket OIL.
   */
  public static void oilUserReply(@Required String id, @Required String azione) {
    if (Validation.hasErrors()) {
      flash.error(Web.MSG_HAS_ERRORS);
      Application.index();
    }
    render(id, azione);
  }

  /**
   * Invio ad OIL delle informazioni aggiuntive relative ad un ticket.
   */
  public static void sendOilUserReplay(@Required String id, @Required String azione,
      @Required String description) {
    if (Validation.hasErrors()) {
      flash.error(Web.MSG_HAS_ERRORS);
      oilUserReply(id, azione);
    }
    OilMailer.sendUserReply(Security.getUser(), id, azione, description);
    flash.success("Risposta al feedback inviata correttamente");
    oilUserReplySent(id);
  }

  public static void oilUserReplySent(String id) {
    render(id);
  }

}
