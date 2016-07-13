package controllers;

import com.google.common.base.Strings;

import dao.PersonDao;
import dao.UserDao;

import manager.EmailManager;
import manager.UserManager;

import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.data.validation.Email;
import play.data.validation.Equals;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.libs.Codec;
import play.mvc.Controller;

import javax.inject.Inject;

public class LostPassword extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static UserDao userDao;
  @Inject
  static UserManager userManager;
  @Inject
  static EmailManager emailManager;

  /**
   * metodo che invia una mail al dipendente che richiede il recupero password.
   *
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
   *
   * @param token il token inviato al dipendente
   * @throws Throwable eccezione
   */
  public static void lostPasswordRecovery(String token) throws Throwable {
    if (Strings.isNullOrEmpty(token)) {
      flash.error("Token non valido.");
      Secure.login();
    }

    User user = userDao.getUserByRecoveryToken(token);
    if (user == null) {
      flash.error("Token non valido.");
      Secure.login();
    }
    if (!user.expireRecoveryToken.equals(LocalDate.now())) {
      flash.error("Il token per effettuare il recupero password è scaduto. " +
          "Effettuare una nuova richiesta.");
      Secure.login();
    }

    render(token);
  }

  public static void lostPassword() {
    render();
  }

  /**
   * Salva la nuova password.
   *
   * @param nuovaPassword    nuovaPassword
   * @param confermaPassword confermaPassword
   * @throws Throwable boh.
   */
  public static void resetUserPassword(String token, @MinSize(5) @Required String nuovaPassword,
      @Required @Equals(value = "nuovaPassword", message = "Le password non corrispondono")
          String confermaPassword) throws Throwable {

    if (Strings.isNullOrEmpty(token)) {
      flash.error("Token non valido.");
      Secure.login();
    }

    User user = userDao.getUserByRecoveryToken(token);
    if (user == null) {
      flash.error("Token non valido.");
      Secure.login();
    }

    if (user.expireRecoveryToken == null || !user.expireRecoveryToken.equals(LocalDate.now())) {
      flash.error("Il token per effettuare il recupero password è scaduto. " +
          "Effettuare una nuova richiesta.");
      Secure.login();
    }

    if (validation.hasErrors()) {
      flash.error("Correggere gli errori riportati");
      render("@lostPasswordRecovery", token, nuovaPassword, confermaPassword);
    }

    user.password = Codec.hexMD5(nuovaPassword);
    user.recoveryToken = null;
    user.expireRecoveryToken = null;
    user.save();

    flash.success("La password è stata re-impostata con successo.");
    Secure.login();
  }
}
