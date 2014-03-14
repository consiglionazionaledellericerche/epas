package controllers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import it.cnr.iit.epas.JsonReportBinder;
import it.cnr.iit.epas.JsonStampingBinder;
import models.Person;
import models.exports.ReportFromJson;
import models.exports.StampingFromClient;
import play.Logger;
import play.Play;
import play.data.binding.As;
import play.libs.Mail;
import play.mvc.Controller;

public class ReportCentre extends Controller{

	public static void sendReport(@As(binder=JsonReportBinder.class) ReportFromJson body){
		
		Logger.debug("report....");
		if (body == null) {
			badRequest();	
		}
		
		try {
	        FileOutputStream imageOutFile = new FileOutputStream(
                    "/home/dario/git/epas/image.png");
            imageOutFile.write(body.image); 
            
            imageOutFile.close();
   
            EmailAttachment attachment = new EmailAttachment();
            attachment.setPath("/home/dario/git/epas/image.png");
            attachment.setDisposition(EmailAttachment.ATTACHMENT);
            attachment.setDescription("Foto anomalia");
            attachment.setName("Foto");

            MultiPartEmail email = new MultiPartEmail();

            email.addTo("epas@iit.cnr.it");
            email.setFrom("segnalazioni@epas.tools.iit.cnr.it");

            email.attach(attachment);
            
	        email.setSubject("Segnalazione malfunzionamento ");
	        email.setMsg("E' stata riscontrata una anomalia dalla pagina: "+body.url+'\n'+"Con il seguente messaggio: "+body.note);
	        Mail.send(email); 
			
			
		
		} catch (EmailException e) {
			
			e.printStackTrace();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}		
		
		
	}
	
	
}
