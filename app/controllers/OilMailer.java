package controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.mail.EmailAttachment;

import com.google.common.base.Verify;

import helpers.deserializers.InlineStreamHandler;
import lombok.extern.slf4j.Slf4j;
import models.User;
import models.exports.ReportData;
import play.Play;
import play.mvc.Mailer;
import play.mvc.Scope;

/**
 * Invio delle segnalazioni per email al sistema CNR OIL.
 * Nella configurazione ci possono essere:
 * <dl>
 *  <dt>oil.email.to</dt><dd>Indirizzo email di OIL a cui inviare le segnalazioni</dd>
 *  <dt>oil.email.subject</dt><dd>Oggetto delle segnalazioni</dd>
 *  <dt>oil.category.id</dt><dd>id della categoria ePAS inviata a OIL</dd>
 *  <dt>oil.category.name</dt><dd>nome della categoria inviata a OIL</dd>
 * </dl>
 * Comunque ci sono dei default.
 *
 * @author cristian
 *
 */
@Slf4j
public class OilMailer extends Mailer {

  /**
   * È possibile configurare l'integrazione con OIL inserendo questi parametri nella
   * configurazione del play.
   */
  private static final String OIL_EMAIL_TO = "oil.email.to";
  private static final String OIL_EMAIL_SUBJECT = "oil.email.subject";
  private static final String OIL_CATEGORY_ID = "oil.category.id";
  private static final String OIL_CATEGORY_NAME = "oil.category.name";
  
  // default per parametri integrazione OIL
  
  private static final String OIL_DEFAULT_EMAIL_TO = "oil.cert@cnr.it";
  private static final String OIL_DEFAULT_EMAIL_SUBJECT = "Segnalazione tecnica ePAS";
  private static final String OIL_DEFAULT_CATEGORY_ID = "4";
  private static final String OIL_DEFAULT_CATEGORY_NAME = "ePAS";
  
  //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
  private static final String OIL_EMAIL_SUBJECT_PATTERN = 
      "[adp]~~%s~~%s~~%s~~%s~~%s~~%s";
  
  /**
   * Costruisce e invia il report agli utenti indicati nella configurazione.
   *
   * @param data i dati del feedback da inviare
   * @param session la sessione http corrente
   * @param user l'eventuale utente loggato
   */
  public static void sendFeedbackToOil(ReportData data, Scope.Session session,
      User user) {

    Verify.verifyNotNull(user);
    Verify.verifyNotNull(user.person);
    Verify.verifyNotNull(user.person.email);
    
    addRecipient(Play.configuration.getProperty(OIL_EMAIL_TO, OIL_DEFAULT_EMAIL_TO));
    setFrom(user.person.email);
    
    //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
    String subject = 
        String.format(OIL_EMAIL_SUBJECT_PATTERN,
            Play.configuration.getProperty(OIL_CATEGORY_ID, OIL_DEFAULT_CATEGORY_ID),
            Play.configuration.getProperty(OIL_CATEGORY_NAME, OIL_DEFAULT_CATEGORY_NAME),
            Play.configuration.getProperty(OIL_EMAIL_SUBJECT, OIL_DEFAULT_EMAIL_SUBJECT),
            user.person.name, user.person.surname, user.person.email);
    setSubject(subject);

    try {
      ByteArrayOutputStream htmlGz = new ByteArrayOutputStream();
      GZIPOutputStream gz = new GZIPOutputStream(htmlGz);
      gz.write(data.getHtml().getBytes());
      gz.close();

      URL htmlUrl = new URL(null, "inline:///html",
          new InlineStreamHandler(htmlGz.toByteArray(), "application/gzip"));
      EmailAttachment html = new EmailAttachment();
      html.setDescription("Original HTML");
      html.setName("page.html.gz");
      html.setURL(htmlUrl);
      html.setDisposition(EmailAttachment.ATTACHMENT);
      addAttachment(html);

      URL imgUrl =
          new URL(null, "inline://image", new InlineStreamHandler(data.getImg(), "image/png"));
      EmailAttachment img = new EmailAttachment();
      img.setDescription("Report image");
      img.setName("image.png");
      img.setURL(imgUrl);
      img.setDisposition(EmailAttachment.ATTACHMENT);
      addAttachment(img);
      send(user, data, session);
    } catch (MalformedURLException e) {
      log.error("malformed url", e);
    } catch (IOException e) {
      log.error("io error", e);
    }
  }
}
