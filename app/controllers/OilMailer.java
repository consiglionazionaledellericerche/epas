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
import com.google.common.base.Verify;
import helpers.OilConfig;
import helpers.deserializers.InlineStreamHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.User;
import models.exports.ReportData;
import org.apache.commons.mail.EmailAttachment;
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
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class OilMailer extends Mailer {
  
  //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
  private static final String OIL_EMAIL_SUBJECT_PATTERN = 
      "[%s]~~%s~~%s~~%s~~%s~~%s~~%s";
  
  //[adp]~~azione~~Id
  private static final String OIL_EMAIL_REPLAY_SUBJECT_PATTERN = 
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
    Verify.verifyNotNull(user.getPerson());
    Verify.verifyNotNull(user.getPerson().getEmail());
    
    addRecipient(OilConfig.emailTo());
    setFrom(user.getPerson().getEmail());
    
    //[adp]~~X~~YYYYYYYYYYYYYYY~~oggetto~~nome~~cognome~~email
    String subject = 
        String.format(OIL_EMAIL_SUBJECT_PATTERN,
            OilConfig.appName(),
            data.getCategory(),
            OilConfig.categoryMap().get(data.getCategory()),
            OilConfig.emailSubject(),
            user.getPerson().getName(), user.getPerson().getSurname(), user.getPerson().getEmail());
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
      debugStream.write(
          String.format("User Agent: %s\n\n", data.getBrowser().getUserAgent()).getBytes());

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
    } catch (MalformedURLException mue) {
      log.error("malformed url", mue);
    } catch (IOException ioe) {
      log.error("io error", ioe);
    }
  }
  
  /**
   * Formato mail di risposta alle richieste dell’esperto (ticket già aperto)
   * L'oggetto è tokenizzato, dove il separatore è la doppia tilde, ed ha il seguente formato:
   * [adp]~~azione~~Id
   * dove i valori di azione ed Id vengono trasmessi al form tramite query-string, e così come 
   * sono vanno riportati nella mail.
   * Un esempio di query-string generata potrebbe essere:
   * http://indirizzoformdirisposta.it?id=9999&azione=c1
   * Il destinatario è sempre oil.cert@cnr.it
   * La descrizione contiene la risposta dell’utente.
   *
   * @param user utente a cui inviare la risposta.
   * @param oilId id del sistema OIL.
   * @param action parametro necessario ad OIL.
   * @param description contiene la risposta dell'utente.
   */
  public static void sendUserReply(Optional<User> user, String oilId, 
      String action, String description) {
    Verify.verifyNotNull(user);
    Verify.verifyNotNull(oilId);
    Verify.verifyNotNull(action);
    Verify.verifyNotNull(description);
    String email;
    if (user.isPresent()) {
      Verify.verifyNotNull(user.get().getPerson());
      Verify.verifyNotNull(user.get().getPerson().getEmail());
      email = user.get().getPerson().getEmail();
    } else {
      email =  OilConfig.defaultEmailFromForUserReply();
    }
    addRecipient(OilConfig.emailTo());
    setFrom(email);
    
    //[adp]~~azione~~Id
    String subject = 
        String.format(OIL_EMAIL_REPLAY_SUBJECT_PATTERN,
            OilConfig.appName(), action, oilId);
    setSubject(subject);
    log.info("Invio risposta utente ad OIL, subject = {}", subject);
    send(description);
  }
}
