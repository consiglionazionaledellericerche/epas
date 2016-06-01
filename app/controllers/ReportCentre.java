package controllers;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;

import controllers.Resecure.NoCheck;
import helpers.OilConfig;
import helpers.Web;
import helpers.deserializers.ImageToByteArrayDeserializer;
import models.exports.ReportData;
import play.Play;
import play.data.validation.Required;
import play.mvc.Controller;

public class ReportCentre extends Controller {

  /**
   * Renderiza il javascript del feedback.js.
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

    ReportMailer.feedback(data, session, Security.getUser());
    if ("true".equals(Play.configuration.getProperty("oil.enabled")) && Security.getUser().isPresent()) {
      OilMailer.sendFeedbackToOil(data, session, Security.getUser().get());
    }
  }
  
  public static void oilUserReply(@Required String id, @Required String azione) {
    if (validation.hasErrors()) {
      flash.error(Web.MSG_HAS_ERRORS);
      Application.index();
    }
    render(id, azione);
  }
  
  public static void sendOilUserReplay(@Required String id, @Required String azione, 
      @Required String description) {
    if (validation.hasErrors()) {
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
