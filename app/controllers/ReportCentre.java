package controllers;

import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;

import helpers.deserializers.ImageToByteArrayDeserializer;

import models.exports.ReportData;

import play.mvc.Controller;

import java.io.InputStreamReader;
import java.time.LocalDateTime;


public class ReportCentre extends Controller {

  /**
   * Renderiza il javascript del feedback.js.
   */
  public static void javascript() {
    response.contentType = MediaType.JAVASCRIPT_UTF_8.toString();
    response.setHeader("Cache-Control", "max-age=" + 31536000);
    response.setHeader("Expires", LocalDateTime.now().plusYears(1).toString());
    render("/feedback.js");
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
    if (Security.getUser().isPresent()) {
      OilMailer.sendFeedbackToOil(data, session, Security.getUser().get());
    }
  }





}
