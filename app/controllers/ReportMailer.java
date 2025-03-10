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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import dao.UserDao;
import helpers.deserializers.InlineStreamHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Role;
import models.User;
import models.exports.ReportData;
import org.apache.commons.mail.EmailAttachment;
import play.Play;
import play.mvc.Mailer;
import play.mvc.Scope;

/**
 * Invio delle segnalazioni per email. Nella configurazione ci possono essere: <dl>
 * <dt>report.to</dt><dd>Destinatari separati da virgole</dd> <dt>report.from</dt><dd>Email
 * mittente</dd> <dt>report.subject</dt><dd>Oggetto della email</dd> </dl> Comunque ci sono dei
 * default.
 *
 * @author Marco Andreini
 */
@Slf4j
public class ReportMailer extends Mailer {

  /**
   * È possibile configurare l'email inserendo questi parametri nella configurazione del play.
   */
  private static final String EMAIL_TO = "report.to";
  private static final String EMAIL_FROM = "report.from";
  private static final String EMAIL_SUBJECT = "report.subject";

  private static final String EMAIL_ALWAYS_TO_PERSONNEL_ADMINS = "report.always_to_personnel_admins";
  // default decenti

  private static final String DEFAULT_EMAIL_FROM = "segnalazioni@epas.tools.iit.cnr.it";
  private static final String DEFAULT_EMAIL_TO = "epas@iit.cnr.it";
  private static final String DEFAULT_SUBJECT = "Segnalazione ePAS";

  private static final Splitter COMMAS = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  @Inject
  static UserDao userDao;

  /**
   * Costruisce e invia il report agli utenti indicati nella configurazione.
   *
   * @param data    i dati del feedback da inviare
   * @param session la sessione http corrente
   * @param user    l'eventuale utente loggato
   */
  public static void feedback(ReportData data, Scope.Session session, Optional<User> user) {

    List<String> dests = Lists.newArrayList();

    boolean toPersonnelAdmin = false;

    boolean alwaysToPersonnelAdmins = 
        "true".equalsIgnoreCase(Play.configuration.getProperty(EMAIL_ALWAYS_TO_PERSONNEL_ADMINS, "false"));

    if (user.isPresent() && (!userDao.hasAdminRoles(user.get()) || alwaysToPersonnelAdmins)) {
      if (user.get().getPerson() != null) {
        dests = userDao.getUsersWithRoles(user.get().getPerson().getOffice(), 
            Role.PERSONNEL_ADMIN).stream()
            .filter(u -> u.getPerson() != null).map(u -> u.getPerson().getEmail())
            .collect(Collectors.toList());
        toPersonnelAdmin = true;
      }
    } else {
      dests = COMMAS.splitToList(Play.configuration
          .getProperty(EMAIL_TO, DEFAULT_EMAIL_TO));
    }

    if (dests.isEmpty()) {
      log.error("please correct {} in application.conf", EMAIL_TO);
      return;
    }
    for (String to : dests) {
      addRecipient(to);
    }
    if (user.isPresent() && user.get().getPerson() != null
        && !Strings.isNullOrEmpty(user.get().getPerson().getEmail())) {
      setReplyTo(user.get().getPerson().getEmail());
    }
    setFrom(Play.configuration.getProperty(EMAIL_FROM, DEFAULT_EMAIL_FROM));
    val username = user.isPresent() 
        ? user.get().getPerson() != null 
          ? user.get().getPerson().getFullname() : user.get().getUsername() : "utente anonimo"; 
    setSubject(
        String.format("%s: %s", 
            Play.configuration.getProperty(EMAIL_SUBJECT, DEFAULT_SUBJECT),
            username));
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
      send(user, data, session, toPersonnelAdmin);
    } catch (MalformedURLException ex) {
      log.error("malformed url", ex);
    } catch (IOException ex) {
      log.error("io error", ex);
    }
  }
}
