package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import helpers.deserializers.InlineStreamHandler;

import lombok.extern.slf4j.Slf4j;

import models.User;
import models.exports.ReportData;

import org.apache.commons.mail.EmailAttachment;

import play.Play;
import play.mvc.Mailer;
import play.mvc.Scope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Invio delle segnalazioni per email.
 * Nella configurazione ci possono essere:
 * <dl>
 *  <dt>report.to</dt><dd>Destinatari separati da virgole</dd>
 *  <dt>report.from</dt><dd>Email mittente</dd>
 *  <dt>report.subject</dt><dd>Oggetto della email</dd>
 * </dl>
 * Comunque ci sono dei default.
 *
 * @author marco
 *
 */
@Slf4j
public class ReportMailer extends Mailer {

  /**
   * È possibile configurare l'email inserendo questi parametri nella
   * configurazione del play.
   */
  private static final String EMAIL_TO = "report.to";
  private static final String EMAIL_FROM = "report.from";
  private static final String EMAIL_SUBJECT = "report.subject";

  // default decenti
  private static final String DEFAULT_EMAIL = "segnalazioni@iit.cnr.it";
  private static final String DEFAULT_SUBJECT = "Segnalazione ePAS";

  private static final Splitter COMMAS = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  /**
   * Costruisce e invia il report agli utenti indicati nella configurazione.
   *
   * @param data i dati del feedback da inviare
   * @param session la sessione http corrente
   * @param user l'eventuale utente loggato
   */
  public static void feedback(ReportData data, Scope.Session session,
      Optional<User> user) {

    final List<String> dests = COMMAS.splitToList(Play.configuration
        .getProperty(EMAIL_TO, DEFAULT_EMAIL));
    if (dests.isEmpty()) {
      log.error("please correct {} in application.conf", EMAIL_TO);
      return;
    }
    for (String to : dests) {
      addRecipient(to);
    }
    if (user.isPresent() && user.get().person != null
        && !Strings.isNullOrEmpty(user.get().person.email)) {
      setReplyTo(user.get().person.email);
    }
    setFrom(Play.configuration.getProperty(EMAIL_FROM, DEFAULT_EMAIL));
    setSubject(Play.configuration.getProperty(EMAIL_SUBJECT, DEFAULT_SUBJECT));

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
