package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.enumerate.EpasParam;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import play.Play;
import play.libs.Mail;

/**
 * Created by daniele on 13/10/15.
 */
@Slf4j
public class EmailManager {

  private final ConfigurationManager configurationManager;

  @Inject
  public EmailManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
    
  }
  
  
  private static final String BASE_URL = Play.configuration.getProperty("application.baseUrl");
  private static final String RECOVERY_PATH = "lostpassword/lostpasswordrecovery?token=";

  private static String getRecoveryBaseUrl(String token) {
    Preconditions.checkState(!Strings.isNullOrEmpty(token));
    String baseUrl = BASE_URL;
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    return baseUrl + RECOVERY_PATH + token;
  }

  public void sendMail(Optional<String> from, String to, String subject, String message) {

    SimpleEmail simpleEmail = new SimpleEmail();

    try {
      if (from.isPresent()) {
        simpleEmail.setFrom(from.get());
      }
      simpleEmail.addTo(to);
      simpleEmail.setSubject(subject);
      simpleEmail.setMsg(message);
    } catch (EmailException e) {
      log.error("Errore nella generazione dell'emai con oggetto {} da inviare a {}", subject, to);
    }

    Mail.send(simpleEmail);

    log.info("Inviata mail con oggetto '{}' a {}", subject, to);
  }

  public void recoveryPasswordMail(Person person) {
    Preconditions.checkState(person != null && person.isPersistent());

    final String message = "Utente: " + person.user.username
            + "\r\n" + "Per ottenere una nuova password apri il seguente collegamento: "
            + getRecoveryBaseUrl(person.user.recoveryToken);

    final String subject = "ePas Recupero Password";

    sendMail(Optional.<String>absent(), person.email, subject, message);
  }

  /**
   * Invia la email per il recovery password successiva a creazione persona. (Solo se
   * il parametro send email della sua sede è attivo).  
   * @param person
   */
  public void newUserMail(Person person) {
    Preconditions.checkState(person != null && person.isPersistent());

    if (!(Boolean)configurationManager.configValue(person.office, EpasParam.SEND_EMAIL)) {
      log.info("Non verrà inviata la mail a {} in quanto "
          + "la sua sede {} ha invio mail disabilitato",
          person.getFullname(), person.office.name);
      return;
    }
    
    final String message = String.format("Gentile %s,\r\n"
                    + "Ti informiamo che e' stato inserito il tuo nominativo nel sistema ePas "
                    + "raggiungibile all'indirizzo %s\r\n"
                    + "\r\nIl tuo username per l'accesso all'applicazione e':\r\n"
                    + "\r\n"
                    + "User: %s\r\n"
                    + "\r\n"
                    + "Per poter eseguire l'accesso e' necessario generare una nuova password "
                    + "da questo indirizzo %s\r\n"
                    + "\r\n"
                    + "Il Team di ePas.",
            person.fullName(), BASE_URL, person.user.username,
            getRecoveryBaseUrl(person.user.recoveryToken));

    final String subject = "Nuovo inserimento Utente in ePas";

    sendMail(Optional.<String>absent(), person.email, subject, message);
  }
}
