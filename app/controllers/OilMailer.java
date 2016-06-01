package controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.mail.EmailAttachment;

import com.google.common.base.Optional;
import com.google.common.base.Verify;

import helpers.OilConfig;
import helpers.deserializers.InlineStreamHandler;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.User;
import models.exports.ReportData;
import play.mvc.Mailer;
import play.mvc.Scope;

/**
 * Invio delle segnalazioni per email al sistema CNR OIL.
 * Nella configurazione ci possono essere:
 * <dl>
 *  <dt>oil.app.name</dt><dd>Nome dell'instanza OIL, utilizzato nel subject del messaggio</dd>
 *  <dt>oil.email.to</dt><dd>Indirizzo email di OIL a cui inviare le segnalazioni</dd>
 *  <dt>oil.email.subject</dt><dd>Oggetto delle segnalazioni</dd>
 * </dl>
 * Comunque ci sono dei default.
 *
 * @author cristian
 *
 */
@Slf4j
public class OilMailer extends Mailer {
  
  //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
  private static final String OIL_EMAIL_SUBJECT_PATTERN = 
      "[%s]~~%s~~%s~~%s~~%s~~%s~~%s";
  
  //[adp]~~azione~~Id
  private static final String OIL_EMAIL_REPLAY_SUBJECT_PATTER = 
      "[%s]~~%s~~%s";
  
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
    
    addRecipient(OilConfig.emailTo());
    setFrom(user.person.email);
    
    //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
    String subject = 
        String.format(OIL_EMAIL_SUBJECT_PATTERN,
            OilConfig.appName(),
            data.getCategory(),
            OilConfig.categoryMap().get(data.getCategory()),
            OilConfig.emailSubject(),
            user.person.name, user.person.surname, user.person.email);
    setSubject(subject);

    try {
      //HTML della pagina da cui è stata fatta la segnalazione in Attachment
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

      //Informazioni di debug in Attachment
      ByteArrayOutputStream debugGz = new ByteArrayOutputStream();
      GZIPOutputStream debugStream = new GZIPOutputStream(debugGz);
      debugStream.write(String.format("User Agent: %s\n\n", data.getBrowser().getUserAgent()).getBytes());

      for (val s : session.all().entrySet()) {
        debugStream.write(String.format("Session.%s: %s\n", s.getKey(), s.getValue()).getBytes());
      }
      
      debugStream.close();

      URL debugUrl = new URL(null, "inline:///text",
          new InlineStreamHandler(debugGz.toByteArray(), "text/plain"));
      EmailAttachment debugAttachment = new EmailAttachment();
      debugAttachment.setDescription("Debug Info");
      debugAttachment.setName("debug.txt.gz");
      debugAttachment.setURL(debugUrl);
      debugAttachment.setDisposition(EmailAttachment.ATTACHMENT);
      addAttachment(debugAttachment);
      
      //Screenshot in Attachment
      URL imgUrl =
          new URL(null, "inline://image", new InlineStreamHandler(data.getImg(), "image/png"));
      EmailAttachment img = new EmailAttachment();
      img.setDescription("Report image");
      img.setName("image.png");
      img.setURL(imgUrl);
      img.setDisposition(EmailAttachment.ATTACHMENT);
      addAttachment(img);
      log.info("Invio segnalazione ad OIL, suject: {}", subject);
      send(user, data, session);
    } catch (MalformedURLException e) {
      log.error("malformed url", e);
    } catch (IOException e) {
      log.error("io error", e);
    }
  }
  
  /**
   * Formato mail di risposta alle richieste dell’esperto (ticket già aperto)
   * L'oggetto è tokenizzato, dove il separatore è la doppia tilde, ed ha il seguente formato:
   * [adp]~~azione~~Id
   * dove i valori di azione ed Id vengono trasmessi al form tramite query-string, e così come sono vanno riportati nella mail.
   * Un esempio di query-string generata potrebbe essere:
   * http://indirizzoformdirisposta.it?id=9999&azione=c1
   * Il destinatario è sempre oil.cert@cnr.it
   * La descrizione contiene la risposta dell’utente.
   *
   * @param user
   * @param oilId
   * @param action
   * @param description
   */
  public static void sendUserReply(Optional<User> user, String oilId, String action, String description) {
    Verify.verifyNotNull(user);
    Verify.verifyNotNull(oilId);
    Verify.verifyNotNull(action);
    Verify.verifyNotNull(description);
    String email;
    if (user.isPresent()) {
      Verify.verifyNotNull(user.get().person);
      Verify.verifyNotNull(user.get().person.email);
      email = user.get().person.email;
    } else {
      email =  OilConfig.defaultEmailFromForUserReply();
    }
    addRecipient(OilConfig.emailTo());
    setFrom(email);
    
  //[adp]~~azione~~Id
    String subject = 
        String.format(OIL_EMAIL_REPLAY_SUBJECT_PATTER,
            OilConfig.appName(), action, oilId);
    setSubject(subject);
    log.info("Invio risposta utente ad OIL, subject = {}", subject);
    send(description);
  }
}
