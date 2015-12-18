package controllers;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;

import dao.PersonDao;
import dao.UserDao;

import helpers.deserializers.ImageToByteArrayDeserializer;

import manager.ReportCentreManager;

import models.Person;
import models.User;
import models.exports.ReportData;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.libs.Mail;
import play.mvc.Controller;

import java.io.InputStreamReader;
import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.validation.Valid;


public class ReportCentre extends Controller {

  @Inject
  static UserDao userDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static ReportCentreManager reportCentreManager;

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
  }

  


  
}
