package controllers;

import dao.PersonDao;
import dao.UserDao;

import manager.EmailManager;
import manager.UserManager;

import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;

import javax.inject.Inject;

public class LostPassword extends Controller {

  private static final String USERNAME = "username";
  @Inject
  private static PersonDao personDao;
  @Inject
  private static UserDao userDao;
  @Inject
  private static UserManager userManager;
  @Inject
  private static EmailManager emailManager;

  /**
   * metodo che invia una mail al dipendente che richiede il recupero password.
   * @param email la mail della persona
   * @throws Throwable eccezione
   */
  public static void sendTokenRecoveryPassword(@Required @Email String email) throws Throwable {
    if (Validation.hasErrors()) {
      flash.error("Fornire un indirizzo email valido, operazione annullata.");
      LostPassword.lostPassword();
    }

    Person person = personDao.byEmail(email).orNull();
    if (person == null) {

      flash.error("L'indirizzo email fornito è sconosciuto. Operazione annullata.");
      LostPassword.lostPassword();
    }

    userManager.generateRecoveryToken(person);
    emailManager.recoveryPasswordMail(person);

    flash.success("E' stata inviata una mail all'indirizzo %s. "
                    + "Completare la procedura di recovery password entro la data di oggi.", 
                    person.email);
    Secure.login();
  }

  /**
   * metodo che renderizza la pagina di recupero password.
   * @param token il token inviato al dipendente
   * @throws Throwable eccezione
   */
  public static void lostPasswordRecovery(String token) throws Throwable {
    if (token == null || token.equals("")) {
      flash.error("Accesso non autorizzato. Operazione annullata.");
      Secure.login();
    }

    User user = userDao.getUserByRecoveryToken(token);
    if (user == null) {
      flash.error("Accesso non autorizzato. Operazione annullata.");
      Secure.login();
    }
    if (!user.expireRecoveryToken.equals(LocalDate.now())) {
      flash.error("La procedura di recovery password è scaduta. Operazione annullata.");
      Secure.login();
    }

    session.put(USERNAME, user.username);

    render();
  }

  public static void lostPassword() {
    render();
  }

}
