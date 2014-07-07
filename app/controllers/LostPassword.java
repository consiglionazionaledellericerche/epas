package controllers;

import java.math.BigInteger;
import java.security.SecureRandom;

import models.Person;
import models.User;

import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.libs.Codec;
import play.libs.Mail;
import play.mvc.Controller;

public class LostPassword extends Controller{
	
	private static final String GET_RECOVERY_PREFIX = Play.configuration.getProperty("application.baseUrl") + "lostpassword/lostpasswordrecovery?token=";
	
	private static final String USERNAME = "username";
	
	public static void sendTokenRecoveryPassword(String email) throws Throwable
	{
		if(email==null || email.equals("") || !email.contains("@"))
		{
			flash.error("Fornire un indirizzo email valido, operazione annullata.");
			LostPassword.lostPassword();
		}

		//Person person = Person.find("byEmail", email).first();
		Person person = Person.find("bySurname", "Lucchesi").first();
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
		simpleEmail.setFrom("epas@iit.cnr.it");
		//simpleEmail.addTo(email);
		simpleEmail.addTo("alessandro.martelli@iit.cnr.it");
		simpleEmail.setSubject("ePas Recupero Password");
		String message = "Utente: " + person.user.username + "\r\n" + "Per ottenere una nuova password apri il seguente collegamento: " + GET_RECOVERY_PREFIX + token;
		
		Logger.info("Messaggio recovery password spedito è: %s", message);
		
		simpleEmail.setMsg(message);
		Mail.send(simpleEmail); 
		
		flash.success("E' stata inviata una mail all'indirizzo %s. Completare la procedura di recovery password entro la data di oggi.",person.email);
		Secure.login();
	}
	
	public static void lostPasswordRecovery(String token) throws Throwable
	{
		if(token==null || token.equals(""))
		{
			flash.error("Accesso non autorizzato. Operazione annullata.");
			Secure.login();
		}
		
		User user = User.find("byRecoveryToken", token).first();
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
