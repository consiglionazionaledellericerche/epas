package controllers;

import it.cnr.iit.epas.MainMenu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection.Request;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

import models.Absence;
import models.Competence;
import models.Configuration;
import models.Person;
import models.PersonMonth;
import play.Logger;
import play.mvc.Controller;

public class UploadSituation extends Controller{
	
	private static String ATTESTATI_URL = "http://lucchesi.iit.cnr.it/attestati/";
	private static String LOGIN_URL = ATTESTATI_URL + "LoginLDAP";
	private static String LISTA_DIPENDENTI_URL = ATTESTATI_URL + "ListaDip";
	
	private static String UTENTE = "claudio.baesso";
	private static String PASSWORD = "mypassword";
	private static String SEDE_ID = "223400";
	
	private final static class AttestatiException extends RuntimeException {
		String exception;
		public AttestatiException(String exception) {
			this.exception = exception;
		}
		public String toString() {
			return exception;
		}
	}
	
	public final static class Dipendente {
		public String matricola, nomeCognome;
		public Dipendente(String matricola, String nomeCognome) {
			this.matricola = matricola;
			this.nomeCognome = nomeCognome;
		}
	}
	
	public static void show(Integer month, Integer year){
		MainMenu mainMenu = new MainMenu(year, month, 1);
		render(mainMenu);
	}

	private static boolean login(Connection connection) throws AttestatiException {
		
		Response loginResponse;
		try {
			loginResponse = connection
					  .data("utente", UTENTE)
					  .data("login", PASSWORD)
					  .userAgent("ePAS")
					  .url(LOGIN_URL)
					  .method(Method.POST).execute();
			
			Logger.debug("Effettuata la richiesta di login come utente {}, codice di risposta http = %d. Cookies = %s", 
					UTENTE, loginResponse.statusCode(), loginResponse.cookies());
			
			Document loginDoc = loginResponse.parse();
			Logger.debug("Risposta alla login = \n%s", loginDoc);
			
			return loginResponse.statusCode() == 200;			
		} catch (IOException e) {
			Logger.error("Errore durante la login sul sistema di invio degli attestati. Eccezione = %s", e.getStackTrace().toString());
			throw new AttestatiException(String.format("Impossibile effettuare il login su %s", LOGIN_URL));
		}
		
		
	}
	
	private static List<Dipendente> listaDipendenti(Connection connection, Integer year, Integer month) {
		Response listaDipendentiResponse;
		try {
			listaDipendentiResponse = connection
					  .data("sede_id", SEDE_ID)
					  .data("anno", year.toString())
					  .data("mese", month.toString())
					  .userAgent("ePAS")
					  .url(LISTA_DIPENDENTI_URL)
					  .method(Method.POST).execute();
			
			Logger.debug("Effettuata la richiesta per avere la lista dei dipendenti, codice di risposta http = %s. Cookies = %s", 
					listaDipendentiResponse.statusCode(), listaDipendentiResponse.cookies());
						
			if (listaDipendentiResponse.statusCode() != 200) {
				throw new AttestatiException(
					String.format("Impossibile prelevare la lista dei dipendenti da %s. "
							+ "Il sistema remote ha restituito il codice di errore http = %d."
							+ "Contattare l'amministratore di ePAS per maggiori informazioni.", 
						LISTA_DIPENDENTI_URL, listaDipendentiResponse.statusCode()));
			}
			
			Document listaDipendentiDoc = listaDipendentiResponse.parse();
			
			Logger.debug("Risposta alla richiesta della lista dei dipendenti = \n%s", listaDipendentiDoc);
			
			/*
			 * Snippet di codice html da parsare per avere le matricole e il nome del dipendente:
			 * 			
			 * <tr>
			 *  <td align="right"> <font size="3" color="#0000FF" face="Arial"> 
			 *    <b><a href="DettDip?matr=14669&amp;anno=2013&amp;mese=10&amp;sede_id=223400&amp;ddpage=parziale">14669</a> </b> </font> 
			 *  </td> 
			 *  <td align="left"> <font size="1" color="#0000FF" face="Arial">VIVALDI ANDREA &nbsp; </font></td> 
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">1/2/2012</font></td> 
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">31/1/2014</font></td> 
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">NO</font></td> 
			* </tr>
			 */
			List<Dipendente> listaDipendenti = Lists.newArrayList();
			Elements anchorMatricole = listaDipendentiDoc.select("a[href*=DettDip?matr=]");
			for (Element e : anchorMatricole) {
				String matricola = e.ownText();
				Element tdMatricola = e.parent().parent().parent(); 
				//The HTML entity &nbsp; (Unicode character NO-BREAK SPACE U+00A0) can in Java be represented by the character \u00a0
				String nomeCognome = tdMatricola.siblingElements().get(1).text().replace("\u00a0", "").trim();
				Logger.debug("Nel html della lista delle persone individuato \"%s\", matricola=\"%s\"", nomeCognome, matricola);
				listaDipendenti.add(new Dipendente(matricola, nomeCognome));
			}
			
			return listaDipendenti;
			
		} catch (IOException e) {
			Logger.error("Errore durante il prelevamento della lista dei dipendneti. Eccezione = %s", e.getStackTrace().toString());
			throw new AttestatiException(
				String.format("Errore durante il prelevamento della lista dei dipendneti. Eccezione = %s", e.getStackTrace().toString()));
		}		

	}
	
	public static void checkAttestati(Integer year, Integer month) {
		Connection connection = Jsoup.connect(ATTESTATI_URL);
		if (!login(connection)) {
			flash.error("Errore durante il login sul sistema degli attestati");
			renderText("Errore durante il login sul sistema degli attestati");
		}
		Logger.debug("Prendo lista dipendenti da %s. Anno = %d, mese = %d", ATTESTATI_URL, year, month);
		List<Dipendente> listaDipendenti = listaDipendenti(connection, year, month);
		if (listaDipendenti == null || listaDipendenti.isEmpty()) {
			flash.error("Errore durante il prelevamento della lista dei dipendenti dal sistema degli attestati");
			renderText("Errore durante il prelevamento della lista dei dipendenti dal sistema degli attestati");			
		}
		renderText(
			String.format("Login effettuato con successo e prelevata la lista dei dipendenti per anno %s, mese %s", year, month));
	}
	
	@Check(Security.UPLOAD_SITUATION)
	public static void uploadSituation(Integer year, Integer month) throws IOException{
		if (params.get("loginAttestati") != null) {
			checkAttestati(year, month);
			return;
		}
		
		if(month == null || year == null){
			flash.error("Il valore dei parametri su cui fare il caricamento dei dati non può essere nullo");
			Application.indexAdmin();
		}
		Logger.debug("Anno: %s", year);
		Logger.debug("Mese: %s", month);
		Configuration config = Configuration.getCurrentConfiguration();
		List<Person> personList = Person.find("Select p from Person p where p.number <> ? and p.number is not null order by p.number", 0).fetch();
		Logger.debug("La lista di nomi è composta da %s persone ", personList.size());
		List<Absence> absenceList = null;
		List<Competence> competenceList = null;
		File uploadSituation = new File("situazioneMensile"+year.toString()+month.toString()+".txt");
		Logger.debug("Creato nuovo file per caricare informazioni mensili sul personale in %s", uploadSituation.getAbsolutePath());
		FileWriter writer = new FileWriter(uploadSituation, true);
		try {
			BufferedWriter out = new BufferedWriter(writer);
			out.write(config.seatCode.toString());
			out.write(' ');
			out.write(new String(month.toString()+year.toString()));
			out.newLine();
			for(Person p : personList){
				
				PersonMonth pm = new PersonMonth(p, year, month);
				absenceList = pm.getAbsenceInMonthForUploadSituation();
				if(absenceList != null){
					for(Absence abs : absenceList){
						out.write(p.number.toString());
						out.append(' ');
						out.append('A');
						out.append(' ');
						out.append(abs.absenceType.code);
						out.append(' ');
						out.append(new Integer(abs.personDay.date.getDayOfMonth()).toString());
						out.append(' ');
						out.append(new Integer(abs.personDay.date.getDayOfMonth()).toString());
						out.append(' ');
						out.append('0');
						out.newLine();
					}
				}
				competenceList = pm.getCompetenceInMonthForUploadSituation();
				if(competenceList != null){
					for(Competence comp : competenceList){
						out.append(p.number.toString());
						out.append(' ');
						out.append('C');
						out.append(' ');
						out.append(comp.competenceCode.code);
						out.append(' ');
						out.append(new Integer(comp.valueApproved).toString());
						out.append(' ');
						out.append('0');
						out.append(' ');
						out.append('0');
						out.newLine();
					}
				}
			}
			
			out.close();
			flash.success("Il file contenente le informazioni da caricare su attestati di presenza è stato creato correttamente e si trova in: %s", 
					uploadSituation.getAbsolutePath());
			renderBinary(uploadSituation, "situazioneMensile"+year.toString()+month.toString());
			Application.indexAdmin();
		} catch (IOException e) {
			
			e.printStackTrace();
			flash.error("Il file non è stato creato correttamente, accedere al file di log.");
			Application.indexAdmin();
		}
	}
}
