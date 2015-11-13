package controllers;

import com.google.common.base.Optional;
import dao.UserDao;
import it.cnr.iit.epas.JsonReportBinder;
import manager.ReportCentreManager;
import models.Person;
import models.User;
import models.exports.ReportFromJson;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import play.Logger;
import play.data.binding.As;
import play.libs.Mail;
import play.mvc.Controller;
import play.mvc.Http.Header;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;


public class ReportCentre extends Controller{
	
	@Inject
	static UserDao userDao;
	@Inject
	static ReportCentreManager reportCentreManager;

	public static void sendReport(@As(binder=JsonReportBinder.class) ReportFromJson body){

		Logger.debug("report....");
		if (body == null) {
			badRequest();	
		}

		try {
			User userLogged = Security.getUser().get();
			Person person = userLogged.person;
			File theDir = new File("/tmp/immagini-mail/");
			if(!theDir.exists())
			{	
				boolean success = (new File("/tmp/immagini-mail/")).mkdirs();

				if (!success) {
					Logger.error("Errore in creazione della cartella");
				}
				Logger.debug("Creata cartella ");
			}
			else{
				Logger.debug("Cartella esistente");
			}
			String path = person != null ? "/tmp/immagini-mail/image"+person.id+".png"
					: "/tmp/immagini-mail/image"+userLogged.username+".png";

			FileOutputStream imageOutFile = new FileOutputStream(path);
			imageOutFile.write(body.image); 

			imageOutFile.close();

			EmailAttachment attachment = new EmailAttachment();
			attachment.setPath(path);
			attachment.setDisposition(EmailAttachment.ATTACHMENT);
			attachment.setDescription("Foto anomalia");
			attachment.setName("Foto");

			MultiPartEmail email = new MultiPartEmail();

			email.addTo("epas@iit.cnr.it");
			//			FIXME rendere configurabile quest'indirizzo!!

			if(person != null && !person.email.equals(""))
				email.addReplyTo(person.email);
			email.attach(attachment);

			email.setSubject("Segnalazione malfunzionamento ");

			String sender = person != null ? person.fullName() : userLogged.username;

			email.setMsg("E' stata riscontrata una anomalia dalla pagina: "+body.url+" visitata da: "+sender+'\n'+"Con il seguente messaggio: "+body.note);
			Mail.send(email); 

		} catch (EmailException e) {
			Logger.error("Errore in invio mail. %s", e.toString());

		} catch (FileNotFoundException e) {
			Logger.error("Errore nel caricamento del file immagine da inviare. %s", e.toString());

		} catch (IOException e) {
			Logger.error("Errore di I/O. %s", e.toString());

		}		

	}


	public static void generateReport(){
		User userLogged = Security.getUser().get();		
		render(userLogged);
	}
	
	
	public static void sendProblem(Long userId, String report, String mese, String anno){
		User user = userDao.getUserByIdAndPassword(userId, Optional.<String>absent());
		if(user == null)
			notFound();
		
		HashMap<String, Header> map = (HashMap<String, Header>) request.headers;
		
		String action = reportCentreManager.getActionFromRequest(map);
		SimpleEmail email = new SimpleEmail();
		String sender = user.person != null ? user.person.fullName() : user.username;
		try {
			email.addTo("epas@iit.cnr.it");
			//email.setFrom("epas@iit.cnr.it");
			if(user.person != null && !user.person.email.equals(""))
				email.addReplyTo(user.person.email);
			email.setSubject("Segnalazione malfunzionamento ");
			email.setMsg("E' stata riscontrata una anomalia dalla pagina: "+action+'\n'
					+" con mese uguale a: "+mese+'\n'
					+" con anno uguale a: "+anno+'\n'
					+" visitata da: "+sender+'\n'
					+" in data: "+LocalDate.now()+'\n'
					+" con il seguente messaggio: "+report);
			Mail.send(email); 
			flash.success("Mail inviata con successo");
			Application.index();
			
		} catch (EmailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			flash.error("Errore durante l'invio della mail");
			Application.index();
		}	
		
	}

}
