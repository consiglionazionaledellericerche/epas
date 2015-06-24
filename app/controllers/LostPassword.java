package controllers;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.inject.Inject;

import models.Person;
import models.User;

import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.libs.Mail;
import play.mvc.Controller;
import dao.PersonDao;
import dao.UserDao;

public class LostPassword extends Controller{
	
	@Inject
	private static PersonDao personDao;
	@Inject
	private static UserDao userDao;
	
	
	private static final String RECOVERY_PATH = "lostpassword/lostpasswordrecovery?token=";
	private static final String USERNAME = "username";

	
	private static String getRecoveryBaseUrl() {
		String baseUrl = Play.configuration.getProperty("application.baseUrl");
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		return baseUrl + RECOVERY_PATH;
	}
	
	public static void sendTokenRecoveryPassword(String email) throws Throwable
	{
		if(email==null || email.equals("") || !email.contains("@"))
		{
			flash.error("Fornire un indirizzo email valido, operazione annullata.");
			LostPassword.lostPassword();
		}

		Person person = personDao.byEmail(email).orNull();
		if(person==null)
		{
			flash.error("L'indirizzo email fornito è sconosciuto. Operazione annullata.");
			LostPassword.lostPassword();
		}
		
		//generate random token
		SecureRandom random = new SecureRandom();
		String token = new BigInteger(130, random).toString(32);
		
		//Person person = contactData.person;
		person.user.recoveryToken = token;
		person.user.expireRecoveryToken = new LocalDate();
		person.user.save();
		
		SimpleEmail simpleEmail = new SimpleEmail();
		simpleEmail.setFrom(Play.configuration.getProperty("application.mail.address"));
		simpleEmail.addTo(email);
		if(Play.configuration.getProperty("mail.smtp.user")!=null && 
				!Play.configuration.getProperty("mail.smtp.user").equals("") && 
				Play.configuration.getProperty("mail.smtp.port") != null && 
				!Play.configuration.getProperty("mail.smtp.port").equals("")){
			simpleEmail.setAuthentication(Play.configuration.getProperty("mail.smtp.user"), 
					Play.configuration.getProperty("mail.smtp.password"));
			simpleEmail.setSmtpPort(new Integer(Play.configuration.getProperty("mail.smtp.port")).intValue());
		}
		
		simpleEmail.setSubject("ePas Recupero Password");

//		String message = "Utente: " + person.user.username + "\r\n" + 
//		"Per ottenere una nuova password apri il seguente collegamento: " + GET_RECOVERY_PREFIX + token;

		String message = "Utente: " + person.user.username + "\r\n" + "Per ottenere una nuova password apri il seguente collegamento: " + getRecoveryBaseUrl() + token;

		
		Logger.info("Messaggio recovery password spedito è: %s", message);
		
		simpleEmail.setMsg(message);
		Mail.send(simpleEmail); 
		
		flash.success("E' stata inviata una mail all'indirizzo %s. "
				+ "Completare la procedura di recovery password entro la data di oggi."
				,person.email);
		Secure.login();
	}
	
	public static void lostPasswordRecovery(String token) throws Throwable
	{
		if(token==null || token.equals(""))
		{
			flash.error("Accesso non autorizzato. Operazione annullata.");
			Secure.login();
		}
		
		User user = userDao.getUserByRecoveryToken(token);
		if(user==null)
		{
			flash.error("Accesso non autorizzato. Operazione annullata.");
			Secure.login();
		}
		if(!user.expireRecoveryToken.equals(LocalDate.now()))
		{
			flash.error("La procedura di recovery password è scaduta. Operazione annullata.");
			Secure.login();
		}
		
		session.put(USERNAME, user.username);
		
		render();
	}
	
	
	public static void lostPassword()
	{
		render();
	}

}
