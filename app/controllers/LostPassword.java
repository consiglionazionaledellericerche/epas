package controllers;

import java.math.BigInteger;
import java.security.SecureRandom;

import models.ContactData;
import models.Person;
import models.User;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;

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

		ContactData contactData = ContactData.find("byEmail", email).first();
		if(contactData==null)
		{
			flash.error("L'indirizzo email fornito è sconosciuto. Operazione annullata.");
			LostPassword.lostPassword();
		}
		
		//generate random token
		SecureRandom random = new SecureRandom();
		String token = new BigInteger(130, random).toString(32);
		
		Person person = contactData.person;
		person.user.recoveryToken = token;
		person.user.expireRecoveryToken = new LocalDate();
		person.user.save();
		
		SimpleEmail simpleEmail = new SimpleEmail();
		simpleEmail.setFrom("epas@iit.cnr.it");
		simpleEmail.addTo(email);
		simpleEmail.setSubject("ePas Recupero Password");
		String message = "Utente: " + person.user.username + "\r\n" + "Per ottenere una nuova password apri il seguente collegamento: " + GET_RECOVERY_PREFIX + token;
		
		simpleEmail.setMsg(message);
		Mail.send(simpleEmail); 
		
		flash.success("E' stata inviata una mail all'indirizzo %s. Completare la procedura di recovery password entro la data di oggi.",contactData.email);
		LostPassword.lostPassword();
	}
	
	public static void lostPasswordRecovery(String token)
	{
		if(token==null || token.equals(""))
		{
			flash.error("Accesso non autorizzato. Operazione annullata.");
			render();
		}
		
		User user = User.find("byRecoveryToken", token).first();
		if(user==null)
		{
			flash.error("Accesso non autorizzato. Operazione annullata.");
			render();
		}
		if(!user.expireRecoveryToken.equals(LocalDate.now()))
		{
			flash.error("La procedura di recovery password è scaduta. Operazione annullata.");
			render();
		}
		String newPassword = "ePas"+LocalDate.now().getYear();
		Codec codec = new Codec();
		user.password = codec.hexMD5(newPassword);
		user.recoveryToken = null;
		user.save();
		
		session.put(USERNAME, user.username);
		flash.success("Il sistema ha assegnato al tuo account la password default %s. Si suggerisce di modificarla per motivi di sicurezza.", newPassword);
		Persons.changePassword(user.person.id);
	}
	
	
	public static void lostPassword()
	{
		render();
	}

}
