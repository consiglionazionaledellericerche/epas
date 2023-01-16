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
package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.CheckGreenPass;
import models.Person;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.Play;
import play.libs.Mail;

/**
 * Manager per la gestione delle mail per recuperare la password.
 *
 * @author Daniele Murgia
 * @since 13/10/15
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

  /**
   * Invia la mail contenente il message al destinatario to con campo from e object compilati.
   *
   * @param from il mittente della mail
   * @param to il destinatario della mail
   * @param subject l'oggetto della mail
   * @param message il messaggio
   */
  public void sendMail(Optional<String> from, String to, Optional<String> cc, 
      String subject, String message) {

    SimpleEmail simpleEmail = new SimpleEmail();

    try {
      if (from.isPresent()) {
        simpleEmail.setFrom(from.get());
      }
      simpleEmail.addTo(to);
      if (cc.isPresent()) {
        simpleEmail.addCc(cc.get());
      }      
      simpleEmail.setSubject(subject);
      simpleEmail.setMsg(message);
    } catch (EmailException ex) {
      log.error("Errore nella generazione dell'emai con oggetto {} da inviare a {}", subject, to);
    }

    Mail.send(simpleEmail);

    log.info("Inviata mail con oggetto '{}' a {}", subject, to);
  }

  /**
   * Manda la mail per recuperare la password alla persona passata.
   *
   * @param person la persona cui occorre recuperare la password
   */
  public void recoveryPasswordMail(Person person) {
    Preconditions.checkState(person != null && person.isPersistent());

    final String message = "Utente: " + person.getUser().getUsername()
            + "\r\n" + "Per ottenere una nuova password apri il seguente collegamento: "
            + getRecoveryBaseUrl(person.getUser().getRecoveryToken());

    final String subject = "ePas Recupero Password";

    sendMail(Optional.<String>absent(), person.getEmail(), Optional.absent(), subject, message);
  }

  /**
   * Invia la email per il recovery password successiva a creazione persona. (Solo se
   * il parametro send email della sua sede è attivo).  
   */
  public void newUserMail(Person person) {
    Preconditions.checkState(person != null && person.isPersistent());

    if (!(Boolean) configurationManager.configValue(person.getOffice(), EpasParam.SEND_EMAIL)) {
      log.info("Non verrà inviata la mail a {} in quanto "
          + "la sua sede {} ha invio mail disabilitato",
          person.getFullname(), person.getOffice().getName());
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
            person.fullName(), BASE_URL, person.getUser().getUsername(),
            getRecoveryBaseUrl(person.getUser().getRecoveryToken()));

    final String subject = "Nuovo inserimento Utente in ePas";

    sendMail(Optional.<String>absent(), person.getEmail(), Optional.absent(), subject, message);
  }
  
  /**
   * Metodo che informa via mail la persona del controllo del green pass.
   *
   * @param person la persona a cui inviare la mail informativa
   */
  public void infoDrawnPersonForCheckingGreenPass(Person person) {
    Preconditions.checkState(person != null && person.isPersistent());
    
    final String subject = String.format("Invio mail per controllo green pass a %s", 
        person.fullName());
    final String message = "Cara/o,\n" 
        + "\n" 
        + "Per adempiere agli obblighi derivanti dalla nota del Direttore Generale 0067412 del "
        + "13 ottobre 2021 sei pregata/o di recarti immediatamente dal personale preposto ai "
        + "controlli (Antonella Mamone, o in caso di sua assenza Carlo Carbone) munito di "
        + "green pass o altri documenti equipollenti. Nel caso non fosse possibile procedere "
        + "immediatamente al controllo, a causa di un'attività lavorativa in corso che non possa "
        + "essere interrotta, invia una e-mail al Direttore e in cc ai due preposti con "
        + "l’indicazione della motivazione per il ritardo nella presentazione del Green Pass e "
        + "di quando terminerà l’attività in corso. "
        + "Appena completata l’attività in corso si proceda immediatamente alla "
        + "presentazione del green pass."
        + "\n" 
        + "\n" 
        + "Si ricorda che in caso di mancata presentazione del green pass, o altri documenti "
        + "equipollenti, sarai considerato/a “assente ingiustificato” e il direttore dovrà "
        + "comunicarlo all’ufficio gestione delle risorse umane per le opportune azioni del caso. "
        + "\n" 
        + "\n" 
        + "Si ringrazia per la collaborazione\n" 
        + "\n" 
        + "Il Direttore IIT";
    
    sendMail(Optional.of("direttore@iit.cnr.it"), person.getEmail(), Optional.absent(), 
        subject, message);
  }
  
  /**
   * Informa via mail l'amministrazione e l'ufficio tecnico di chi devono contattare
   * per il check del green pass.
   *
   * @param peopleSelected la lista di persone selezionate
   * @param date la data in cui sono selezionate
   */
  public void infoPeopleSelected(List<CheckGreenPass> peopleSelected, LocalDate date, 
      Integer numberOfActivePeople) {
    Preconditions.checkState(peopleSelected != null);
    DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yyyy");
    final String subject = String.format("Lista selezionati per controllo del %s",
        date.toString(df));
    StringBuilder sb = new StringBuilder();
    
    if (peopleSelected.isEmpty()) {
      sb.append("Nessuna persona sorteggiata per oggi, "
          + "ignorare questa email se si tratta di un giorno festivo.\r\n");
    } else {
      sb.append(String.format(
          "\r\nSono state sorteggiate %d persone su un totale di %s persone che hanno "
          + "timbrature nel giorno %s.\r\n", 
          peopleSelected.size(), numberOfActivePeople, date.toString(df)));
      sb.append(String.format("Lista dei dipendenti da controllare per il "
          + "%s:\r\n\r\n", date.toString(df)));

      peopleSelected.sort(
          new Comparator<CheckGreenPass>() {
            @Override
            public int compare(CheckGreenPass cgp1, CheckGreenPass cgp2) {
              return cgp1.getPerson().getSurname().compareTo(cgp2.getPerson().getSurname());
            }
          });

      for (CheckGreenPass gp : peopleSelected) {
        sb.append(gp.getPerson().getFullname() + "\r\n");
      }      
    }

    sendMail(Optional.of("epas@iit.cnr.it"), "antonella.mamone@iit.cnr.it", 
        Optional.of("marco.conti@iit.cnr.it"), subject, sb.toString());
    sendMail(Optional.of("epas@iit.cnr.it"), "carlo.carbone@iit.cnr.it", 
        Optional.absent(), subject, sb.toString());
  }

}