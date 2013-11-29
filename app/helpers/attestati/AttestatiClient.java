/**
 * 
 */
package helpers.attestati;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import models.Absence;
import models.Competence;
import models.Configuration;
import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

import play.Logger;

/**
 * Incapsula le funzionalità necessarie per l'interazione via HTTP GET/POST
 * con il sistema degli attestati del CNR di Roma.
 * 
 * @author cristian
 *
 */
public class AttestatiClient {

	private static String CLIENT_USER_AGENT = "ePAS"; 

	private static String BASE_LOGIN_URL = "LoginLDAP";
	private static String BASE_LISTA_DIPENDENTI_URL = "ListaDip";
	private static String BASE_ELABORA_DATI_URL = "HostDip";
	
	/**
	 * Contenitore dei dati necessari per l'invio dei periodi di assenza
	 * del personale tramite il sistema degli attestati del CNR.
	 * 
	 * @author cristian
	 *
	 */
	private final static class AssenzaPerPost {
		private final String codice;
		private final Integer ggInizio;
		private Integer ggFine;
		public AssenzaPerPost(String codice, Integer ggInizio) { 
			this.codice = codice;
			this.ggInizio = ggInizio;
			this.ggFine = ggInizio;
		}
		public String getCodice() {return codice; }
		public Integer getGgInizio() { return ggInizio; }
		public Integer getGgFine() { return ggFine; }
		public void setGgFine(Integer ggFine) { this.ggFine = ggFine; }

	}
	
	public final static class LoginResponse {
		private final boolean loggedIn;
		private final Map<String, String> cookies;
		public LoginResponse(boolean loggedIn, Map<String, String> cookies) {
			this.loggedIn = loggedIn;
			this.cookies = cookies;
		}
		public boolean isLoggedIn() { return loggedIn; }
		public Map<String, String> getCookies() { return cookies; }
	}
	
	
	/**
	 * Effettua la login sul sistema degli attestati facendo una post con i parametri passati.
	 * 
	 * @param connection
	 * @param attestatiLogin nome utente 
	 * @param attestatiPassword password
	 * @return la LoginResponse contiene l'ok se la login è andata a buon fine ed in questo caso anche
	 * 	i cookies necessari per le richieste successive.
	 * 
	 * @throws AttestatiException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public static LoginResponse login(String attestatiLogin, String attestatiPassword) throws AttestatiException, MalformedURLException, URISyntaxException {
		
		URI baseUri = new URI(Configuration.getCurrentConfiguration().urlToPresence);
		URL loginUrl = baseUri.resolve(BASE_LOGIN_URL).toURL();
		
		Connection connection = Jsoup.connect(loginUrl.toString());
		Response loginResponse;
		try {
			loginResponse = connection
					.data("utente", attestatiLogin)
					.data("login", attestatiPassword)
					.userAgent(CLIENT_USER_AGENT)
					.url(loginUrl)
					.method(Method.POST).execute();

			Logger.debug("Effettuata la richiesta di login come utente %s, codice di risposta http = %s", 
					attestatiLogin, loginResponse.statusCode());

			Document loginDoc = loginResponse.parse();
			Logger.debug("Risposta alla login = \n%s", loginDoc);


			Elements loginMessages = loginDoc.select("h5[align=center]>font");
			
			if (loginResponse.statusCode() != 200 || 
					loginMessages.isEmpty() || 
					! loginMessages.first().ownText().contains("Login completata con successo.")) {
				return new LoginResponse(false, loginResponse.cookies());
			} else {
				return new LoginResponse(true, loginResponse.cookies());
			}
			
		} catch (IOException e) {
			Logger.error("Errore durante la login sul sistema di invio degli attestati. Eccezione = %s", e);
			return new LoginResponse(false, null);
		}
	}
	
	/**
	 * @param cookies i cookies da utilizzare per inviare una richiesta "autenticata"
	 * @param year
	 * @param month
	 * @return la lista dei dipendenti estratta dall'HTML della apposita pagina prevista nel sistema
	 * 	degli attestati di Roma
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public static List<Dipendente> listaDipendenti(Map<String, String> cookies, Integer year, Integer month) throws URISyntaxException, MalformedURLException {
		Response listaDipendentiResponse;
		Configuration conf = Configuration.getCurrentConfiguration();
		URI baseUri = new URI(conf.urlToPresence);
		final URL listaDipendentiUrl = baseUri.resolve(BASE_LISTA_DIPENDENTI_URL).toURL();
		Connection connection = Jsoup.connect(listaDipendentiUrl.toString());
		connection.cookies(cookies);
		
		try {
			listaDipendentiResponse = connection
					.data("sede_id", conf.seatCode.toString())
					.data("anno", year.toString())
					.data("mese", month.toString())
					.userAgent(CLIENT_USER_AGENT)
					//.url(listaDipendentiUrl)
					.method(Method.POST).execute();

			Logger.debug("Effettuata la richiesta per avere la lista dei dipendenti, codice di risposta http = %d", 
					listaDipendentiResponse.statusCode());

			if (listaDipendentiResponse.statusCode() != 200) {
				throw new AttestatiException(
						String.format("Impossibile prelevare la lista dei dipendenti da %s. "
								+ "Il sistema remote ha restituito il codice di errore http = %d."
								+ "Contattare l'amministratore di ePAS per maggiori informazioni.", 
								listaDipendentiUrl, listaDipendentiResponse.statusCode()));
			}

			Document listaDipendentiDoc = listaDipendentiResponse.parse();

			Logger.debug("Risposta alla richiesta della lista dei dipendenti = \n%s", listaDipendentiDoc);

			/*
			 * Snippet di codice html da parsare per avere le matricole e il nome del dipendente:
			 * 			
			 * <tr>
			 *  <td align="right"> <font size="2" color="#0000FF" face="Arial"> <b>1</b> </font> </td>
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
	
	public static RispostaElaboraDati elaboraDatiDipendente(
			Map<String, String> cookies, Dipendente dipendente, Integer year, Integer month, 
			List<Absence> absences, List<Competence> competences) 
					throws URISyntaxException, MalformedURLException {
		
		Configuration conf = Configuration.getCurrentConfiguration();
		URI baseUri = new URI(conf.urlToPresence);
		final URL elaboraDatiUrl = baseUri.resolve(BASE_ELABORA_DATI_URL).toURL();

		StringBuffer absencesSent = new StringBuffer();
		StringBuffer competencesSent = new StringBuffer();
		StringBuffer problems = new StringBuffer();
		
		boolean isOk = true;
		
		Connection connection = Jsoup.connect(elaboraDatiUrl.toString());
		connection.cookies(cookies);
		
		connection.userAgent(CLIENT_USER_AGENT)
			.data("matr", dipendente.getMatricola())
			.data("anno", year.toString())
			.data("mese", month.toString())
			.data("sede_id", conf.seatCode.toString())
			.method(Method.POST);

		int codAssAssoCounter = 0;
		for (AssenzaPerPost assenzaPerPost : getAssenzePerPost(absences)) {
			connection.data("codass" + codAssAssoCounter, assenzaPerPost.getCodice());
			connection.data("gg_inizio" + codAssAssoCounter, assenzaPerPost.getGgInizio().toString());
			connection.data("gg_fine" + codAssAssoCounter, assenzaPerPost.getGgFine().toString());
			absencesSent.append(assenzaPerPost.getCodice()).append(",")
								.append(assenzaPerPost.getGgInizio()).append(",")
								.append(assenzaPerPost.getGgFine()).append("; ");
			Logger.debug("%s, sto spedendo l'assenza di tipo %s, gg inizio = %d, gg_fine = %d", 
					dipendente.getCognomeNome(), assenzaPerPost.getCodice(),
					assenzaPerPost.getGgInizio(), assenzaPerPost.getGgInizio());
			codAssAssoCounter++;
		}

		int codComCounter = 0;
		for (Competence competence: competences) {
			connection.data("codcom" + codComCounter, competence.competenceCode.code);
			connection.data("oreatt" + codComCounter, String.valueOf(competence.valueApproved));
			competencesSent.append(competence.competenceCode.code).append(",")
							.append(competence.valueApproved).append("; ");			
			Logger.debug("%s, sto spedendo la competenza di tipo %s, ore attribuite = %d", 
					dipendente.getCognomeNome(), competence.competenceCode.code, competence.valueApproved);
			codComCounter++;
		}
		
		Response elaboraDatiResponse;
		try {
			elaboraDatiResponse = connection.execute();

			Logger.debug("Effettuata l'elaborazione dati del dipendente %s (matricola %s) per l'anno %d, mese %d. Codice di risposta http = %d", 
					dipendente.getCognomeNome(), dipendente.getMatricola(), year, month, elaboraDatiResponse.statusCode());

			if (elaboraDatiResponse.statusCode() != 200)  {
				throw new AttestatiException(String.format("Errore durante l'elaborazione dati del dipendente %s", dipendente.getCognomeNome()));
			};			

			Document elaboraDatiDoc = elaboraDatiResponse.parse();
			Logger.debug("Risposta all'elaborazione dati = \n%s", elaboraDatiDoc);
			
			/*
			 * In caso di errore nella pagina restituita compaiono degli H5 come questi:
			 *	<H5 align=center><FONT SIZE='4' FACE='Arial'>Errore in fase di controllo competenze <BR>7  ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT></H5>
			 *	<BR>Controllo Competenze --> ..Effettuato! 
			 *	<B>Non sono state inserite competenze</B>
			 *	<H5 align=center><FONT SIZE='4' FACE='Arial'>Errore in fase di controllo assenze dipendente=9535, mese=10, anno=2013, errore=7  ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT></H5> 			
			 */

			Elements errorElements = elaboraDatiDoc.select("h5[align=center]>font");
			if (errorElements.isEmpty()) {
				/*TODO: controllare anche che ci sia scritto:
				 *
				 * <BR>Controllo Competenze --> ..Effettuato! 
				 * <B>Non sono state inserite competenze</B>
				 * <BR>Controllo Assenze --> ..Effettuato! 
				 * <BR>Aggiornamento Competenze --> ..Effettuato! <B></B>
				 * <BR>Aggiornamento Assenze --> ..Effettuato! <B></B><BR>
				 */
			} else {
				//Si aggiunge il contenuto testuale degli elementi font che contengono il messaggio di errore
				for (Element el : errorElements) {
					problems.append(el.ownText());
				}
				isOk = false;
			}
		} catch (IOException e) {
			Logger.error("Errore la chiamata alla funzione \"elabora dati\" sistema di invio degli attestati. Eccezione = %s", e.getStackTrace().toString());
			throw new AttestatiException(String.format("Impossibile effettuare l'elaborazione dati su %s", elaboraDatiUrl));
		}

		RispostaElaboraDati resp = new RispostaElaboraDati(dipendente.getCognomeNome(), dipendente.getMatricola());
		resp.setAbsencesSent(absencesSent.length() > 0 ? absencesSent.toString() : null);
		resp.setProblems(problems.length() > 0 ? problems.toString() : null);
		resp.setCompetencesSent(competencesSent.length() > 0 ? competencesSent.toString() : null);
		resp.setOk(isOk);
		return resp;
	}
	
	/**
	 * La lista di assenze passate sono di tipo giornaliero ma al sistema degli attestati vanno
	 * passate le assenze come intervallo di tempo.
	 * Questo metodo accorpa le assenze dello stesso tipo effettuate in giorni conseguitivi 
	 * in un'unica assenza di tipo AssenzaPerPost e ritorna la lista delle assenze accorpate. 
	 * 
	 * @param absences la lista delle assenze giornaliere da accorpare in oggetti AssenzaPerPost
	 * @return la lista delle assenze accorpate in oggetti di tipo AssenzaPerPost
	 */
	private static List<AssenzaPerPost> getAssenzePerPost(List<Absence> absences) {
		List<AssenzaPerPost> assenze = Lists.newLinkedList();

		LocalDate previousDate = null;
		String previousAbsenceCode = null;
		AssenzaPerPost assenza = null;

		for (Absence absence : absences) {
			if (previousDate == null || previousAbsenceCode == null) { 
				assenza = new AssenzaPerPost(absence.absenceType.code, absence.personDay.date.getDayOfMonth());
				assenze.add(assenza);
				previousDate = absence.personDay.date;
				previousAbsenceCode = absence.absenceType.code;
				continue;
			} 

			if (previousDate.plusDays(1).equals(absence.personDay.date) && previousAbsenceCode.equals(absence.absenceType.code)) {
				assenza.setGgFine(absence.personDay.date.getDayOfMonth());
			} else {
				assenza = new AssenzaPerPost(absence.absenceType.code, absence.personDay.date.getDayOfMonth());
				previousAbsenceCode = absence.absenceType.code;
				assenze.add(assenza);
			}
			previousDate = absence.personDay.date;

		}

		return assenze;
	}	
}
