/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;
import common.security.ClientAddress;
import controllers.Resecure.NoCheck;
import dao.GeneralSettingDao;
import dao.UserDao;
import helpers.OilConfig;
import helpers.Web;
import helpers.deserializers.ImageToByteArrayDeserializer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.services.helpdesk.HelpdeskServiceManager;
import models.User;
import models.exports.ReportData;
import play.Play;
import play.cache.Cache;
import play.data.validation.Match;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * Classi di supporto per l'invio delle segnalazioni utente.
 *
 * @author Cristian Lucchesi
 *
 */
@Slf4j
@With(Resecure.class)
public class ReportCentre extends Controller {

  /**
   * Dimensione massima di una segnalazione (json con HTML e immagine della pagina).
   */
  static final int MAX_REPORT_CHARS = 10 * 1024 * 1024;
  /**
   * Numero massimo di segnalazioni anonime per indirizzo in un'ora.
   */
  static final int MAX_ANONYMOUS_REPORTS = 5;
  static final String ANONYMOUS_REPORTS_KEY = "anonymousReports.";
  private static final int TOO_MANY_REQUESTS = 429;
  private static final Object ANONYMOUS_REPORTS_LOCK = new Object();
  private static final String OIL_PARAM_PATTERN = "[A-Za-z0-9_-]{1,64}";

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
    response.setHeader("Cache-Control", "max-age=" + 86400);
    response.setHeader("Expires", LocalDateTime.now().plusDays(1).toString());

    Map<String, String> categoryMap = OilConfig.categoryMap();
    //Se sono presenti le categorie è obbligatorio per l'utente selezionarle
    String selectedCategory = OilConfig.selectedCategory();

    render("/feedback.js", categoryMap, selectedCategory);
  }

  /**
   * Invia un report via email leggendo la segnalazione via post json.
   * Disponibile anche senza autenticazione, per le segnalazioni dalla pagina di login.
   */
  @NoCheck
  public static void sendReport() {
    final Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() && anonymousReportsLimitReached()) {
      log.warn("Troppe segnalazioni anonime dall'indirizzo {}", ClientAddress.current().orNull());
      response.status = TOO_MANY_REQUESTS;
      renderText("Troppe segnalazioni, riprovare più tardi.");
    }

    // Il json viene letto dal parametro "body" di play e non da request.body: gli interceptor
    // (es. RequestInit per gli utenti autenticati) leggono i parametri e consumano lo stream.
    final String body = Strings.nullToEmpty(params.get("body"));
    if (body.length() > MAX_REPORT_CHARS) {
      log.warn("Segnalazione scartata: dimensione oltre {} caratteri", MAX_REPORT_CHARS);
      badRequest("Segnalazione troppo grande");
    }
    final ReportData data = new GsonBuilder()
        .registerTypeHierarchyAdapter(byte[].class,
            new ImageToByteArrayDeserializer()).create()
        .fromJson(body, ReportData.class);
    if (data == null) {
      badRequest("Segnalazione non valida");
    }
    if (!currentUser.isPresent()) {
      // dagli utenti non autenticati non si inoltra l'HTML della pagina, scelto da chi invia
      data.setHtml(null);
    }
    // la sessione play corrente, senza token e segreti (sostituisce quella eventualmente
    // presente nel json ricevuto)
    data.setSession(ReportData.sessionForReport(session.all()));

    val generalSettings = generalSettingDao.generalSetting();
    //Questo è il caso di invio delle segnalazioni tramite il servizio esterno.
    if (generalSettings.isEpasHelpdeskServiceEnabled() 
        && generalSettings.getEpasHelpdeskServiceUrl() != null) {
      if (!helpdeskServiceManager.sendReport(data)) {
        log.warn("Errore nell'invio della segnalazione {}", data);
        error(String.format("Errore nell'invio della segnalazione %s", data));
      }
      return;
    }

    if ("true".equals(Play.configuration.getProperty("oil.enabled")) && currentUser.isPresent()) {
      if (userDao.hasAdminRoles(currentUser.get())) {
        OilMailer.sendFeedbackToOil(data, data.getSession(), currentUser.get());
        log.info("Inviata segnalazione ad OIL. Utente {}. Categoria: '{}'. Url: {}. Note: {}", 
            currentUser.get().getUsername(), OilConfig.categoryMap().get(data.getCategory()), 
            data.getUrl(), data.getNote());
      } else {
        ReportMailer.feedback(data, data.getSession(), currentUser);
      }
    } else {
      ReportMailer.feedback(data, data.getSession(), currentUser);
    }
  }

  /**
   * Conta una segnalazione anonima dall'indirizzo del client corrente e indica se è stato
   * superato il limite di {@link #MAX_ANONYMOUS_REPORTS} segnalazioni in un'ora.
   */
  @Util
  static boolean anonymousReportsLimitReached() {
    final String key = ANONYMOUS_REPORTS_KEY + Hashing.sha256().hashString(
        ClientAddress.current().or("-"), StandardCharsets.UTF_8);
    synchronized (ANONYMOUS_REPORTS_LOCK) {
      final Integer reports = Cache.get(key, Integer.class);
      final int count = reports == null ? 1 : reports + 1;
      if (reports == null) {
        Cache.safeSet(key, count, "1h");
      } else {
        Cache.safeReplace(key, count, "1h");
      }
      return count > MAX_ANONYMOUS_REPORTS;
    }
  }

  /**
   * Form per l'invio di ulteriori informazioni relativemante ad un ticket OIL.
   */
  public static void oilUserReply(@Required @Match(OIL_PARAM_PATTERN) String id,
      @Required @Match(OIL_PARAM_PATTERN) String azione) {
    if (Validation.hasErrors()) {
      flash.error(Web.MSG_HAS_ERRORS);
      Application.index();
    }
    render(id, azione);
  }

  /**
   * Invio ad OIL delle informazioni aggiuntive relative ad un ticket.
   */
  public static void sendOilUserReplay(@Required @Match(OIL_PARAM_PATTERN) String id,
      @Required @Match(OIL_PARAM_PATTERN) String azione,
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
