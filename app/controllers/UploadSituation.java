package controllers;

import helpers.attestati.AttestatiClient;
import helpers.attestati.AttestatiClient.LoginResponse;
import helpers.attestati.AttestatiException;
import helpers.attestati.Dipendente;
import helpers.attestati.RispostaElaboraDati;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.Join;

import lombok.Data;
import models.Absence;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.ConfGeneral;
import models.Person;
import models.PersonMonthRecap;

import org.hibernate.ejb.criteria.path.AbstractFromImpl.JoinScope;
import org.hibernate.type.OrderedSetType;
import org.joda.time.LocalDate;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Contiene in metodi necessari per l'interazione tra utente, ePAS e 
 * sistema centrale del CNR per gli attestati.
 * 
 * @author cristian
 *
 */
@With( {Secure.class, NavigationMenu.class} )
public class UploadSituation extends Controller{

	@Check(Security.UPLOAD_SITUATION)
	public static void show(final Integer year, final Integer month){
		
		render();
		/*
		MainMenu mainMenu = null;
		if (month == null || year == null) {
			LocalDate prevMonth = LocalDate.now().minusMonths(1);
			mainMenu = new MainMenu(prevMonth.getYear(), prevMonth.getMonthOfYear());
		} else 
			mainMenu = new MainMenu(year, month, 1);
		render(mainMenu);
		*/
	}

	@Check(Security.UPLOAD_SITUATION)
	public static void loginAttestati(Integer year, Integer month) {
		//Configuration conf = Configuration.getCurrentConfiguration();
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		String urlToPresence = conf.urlToPresence;
		String attestatiLogin = params.get("attestatiLogin") == null ? conf.userToPresence : params.get("attestatiLogin"); 

		render(year, month, urlToPresence, attestatiLogin);
	}

	@Check(Security.UPLOAD_SITUATION)
	public static void checkAttestati(final String attestatiLogin, final String attestatiPassword, final Integer year, final Integer month) 
			throws AttestatiException, MalformedURLException, URISyntaxException {

		if (params.get("back") != null) {
			loginAttestati(year, month);
		}

		if (params.get("home") != null) {
			redirect("Application.indexAdmin");
		}

		
		//String urlToPresence = Configuration.getCurrentConfiguration().urlToPresence;
		String urlToPresence = ConfGeneral.getConfGeneral().urlToPresence;

		List<String> actions = Lists.newLinkedList();

		List<Dipendente> listaDipendenti = null;
		LoginResponse loginResponse = null;
		try {
			loginResponse = AttestatiClient.login(attestatiLogin, attestatiPassword); 
			if (!loginResponse.isLoggedIn()) {
				flash.error("Errore durante il login sul sistema degli attestati");
				actions.add("Login sul sistema degli attestati fallito");
				render(attestatiLogin, attestatiPassword, year, month);
				return;
			} 
			actions.add(String.format("Login effettuato con successo su %s", urlToPresence));

			Logger.debug("Prendo lista dipendenti da %s. Anno = %d, mese = %d", urlToPresence, year, month);

			listaDipendenti = AttestatiClient.listaDipendenti(loginResponse.getCookies(), year, month);

		} catch (AttestatiException e) {
			flash.error(
					String.format("Errore durante il login e/o prelevamento della lista dei dipendenti dal sistema degli attestati. Eccezione: %s", e));
			render(attestatiLogin, attestatiPassword, year, month, actions);
		}

		if (listaDipendenti == null || listaDipendenti.isEmpty()) {
			flash.error("Errore durante il prelevamento della lista dei dipendenti dal sistema degli attestati.");
			actions.add("Prelevamento della lista dei dipendenti fallito");
			render(attestatiLogin, attestatiPassword, year, month, actions);			
		}

		actions.add(String.format("Prelevata la lista dei dipendenti per l'anno %d e mese %d, trovati %d dipendenti", 
				year, month, listaDipendenti.size()));

		//Lista delle persone con un contratto attivo questo mese
		final List<Person> activePersons = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		
		final Set<Dipendente> activeDipendenti = FluentIterable.from(activePersons).transform(new Function<Person, Dipendente>() {
			@Override
			public Dipendente apply(Person person) {
				Dipendente dipendente = 
						new Dipendente(person.number == null ? "" : person.number.toString(), Joiner.on(" ").skipNulls().join(person.surname, person.othersSurnames, person.name));
				return dipendente;
			}
		}).toSet();

		Logger.trace("Lista dipendenti attivi nell'anno %d, mese %d e': %s", year, month, activeDipendenti);

		Set<Dipendente> dipendentiNonInEpas = Sets.difference(ImmutableSet.copyOf(listaDipendenti), activeDipendenti);
		if (dipendentiNonInEpas.size() > 0)
			Logger.info("I seguenti dipendenti sono nell'anagrafica CNR ma non in ePAS. %s", dipendentiNonInEpas);

		Set<Dipendente> dipendentiNonInCNR = Sets.difference(activeDipendenti, ImmutableSet.copyOf(listaDipendenti));
		if (dipendentiNonInCNR.size() > 0)
			Logger.info("I seguenti dipendenti sono nell'anagrafica di ePAS ma non in quella del CNR. %s", dipendentiNonInCNR);

		List<RispostaElaboraDati> checks = 
				elaboraDatiDipendenti(
						loginResponse.getCookies(), 
						Sets.intersection(ImmutableSet.copyOf(listaDipendenti), activeDipendenti), 
						year, month);

		Predicate<RispostaElaboraDati> rispostaOk = new Predicate<RispostaElaboraDati>() {
			@Override
			public boolean apply(RispostaElaboraDati risposta) {
				return risposta.getProblems() == null || risposta.getProblems().isEmpty();
			}
		};
		List<RispostaElaboraDati> risposteOk = FluentIterable.from(checks).filter(rispostaOk).toList();
		List<RispostaElaboraDati> risposteNotOk = FluentIterable.from(checks).filter(Predicates.not(rispostaOk)).toList();

		render(attestatiLogin, attestatiPassword, year, month, actions, dipendentiNonInEpas, dipendentiNonInCNR, risposteOk, risposteNotOk);

	}

	@Check(Security.UPLOAD_SITUATION)
	public static void uploadSituation(Integer year, Integer month) throws IOException{
		if (params.get("loginAttestati") != null) {
			loginAttestati(year, month);
			return;
		}

		if (params.get("back") != null) {
			redirect("Application.indexAdmin");
		}
		
		if(month == null || year == null){
			flash.error("Il valore dei parametri su cui fare il caricamento dei dati non può essere nullo");
			Application.indexAdmin();
		}
		//Configuration conf = Configuration.getCurrentConfiguration();
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		List<Person> personList = Person.find("Select p from Person p where p.number <> ? and p.number is not null order by p.number", 0).fetch();
		Logger.debug("La lista di nomi è composta da %s persone ", personList.size());
		List<Absence> absenceList = null;
		List<Competence> competenceList = null;

		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("situazioneMensile"+year.toString()+month.toString(), ".txt" );
		inputStream = new FileInputStream( tempFile );

		FileWriter writer = new FileWriter(tempFile, true);
		try {
			BufferedWriter out = new BufferedWriter(writer);
			out.write(conf.seatCode.toString());
			out.write(' ');
			out.write(new String(month.toString()+year.toString()));
			out.newLine();
			for(Person p : personList){

				PersonMonthRecap pm = new PersonMonthRecap(p, year, month);
				absenceList = pm.getAbsencesNotInternalUseInMonth();
				for(Absence abs : absenceList){
					out.write(p.number.toString());
					out.append(' ').append('A').append(' ')
					.append(abs.absenceType.code).append(' ')
					.append(new Integer(abs.personDay.date.getDayOfMonth()).toString()).append(' ')
					.append(new Integer(abs.personDay.date.getDayOfMonth()).toString()).append(' ')
					.append('0');
					out.newLine();
				}

				competenceList = pm.getCompetenceInMonthForUploadSituation();

				for(Competence comp : competenceList){
					Logger.trace(
							"Inserisco nel file per gli attestati per %d/%d: matricola %d, compCode=%s, ore=%d",
							month, year, p.number,comp.competenceCode.code, comp.valueApproved);
					out.append(p.number.toString())
					.append(' ').append('C').append(' ')
					.append(comp.competenceCode.code).append(' ')
					.append(new Integer(comp.valueApproved).toString()).append(' ')
					.append('0').append(' ').append('0');
					out.newLine();
				}

			}

			out.close();

			renderBinary(inputStream, "situazioneMensile"+year.toString()+month.toString());
			Application.indexAdmin();
		} catch (IOException e) {
			Logger.warn("Errore nella creazione del file per gli attestati. Eccezione=%s", e);
			flash.error("Il file non è stato creato correttamente, accedere al file di log.");
			Application.indexAdmin();
		}
	}


	private static List<RispostaElaboraDati> elaboraDatiDipendenti(Map<String, String> cookies, Set<Dipendente> dipendenti, int year, int month) throws MalformedURLException, URISyntaxException {
		List<RispostaElaboraDati> checks = Lists.newLinkedList();
		Person person = null;
		PersonMonthRecap pm = null;
		for (Dipendente dipendente : dipendenti) {
			person = Person.findByNumber(Integer.parseInt(dipendente.getMatricola()));
			pm = new PersonMonthRecap(person, year, month);
			
			List<PersonMonthRecap> pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.person = ? and pm.month = ? and pm.year = ?",
					 person, month, year).fetch();
			//Numero di buoni mensa da passare alla procedura di invio attestati
			Integer mealTicket = PersonUtility.numberOfMealTicketToUse(person, year, month);
			
			//vedere se l'ho gia' inviato con successo
			CertificatedData cert = CertificatedData.find("Select cert from CertificatedData cert where cert.person = ? and cert.year = ? and cert.month = ?", person, year, month).first();
			
			
			RispostaElaboraDati rispostaElaboraDati = AttestatiClient.elaboraDatiDipendente(
					cookies, dipendente, year, month, 
					pm.getAbsencesNotInternalUseInMonth(),
					pm.getCompetenceInMonthForUploadSituation(),
					pmList, mealTicket);
			if(rispostaElaboraDati.isOk()){
				for(PersonMonthRecap personMonth : pmList){
					personMonth.hoursApproved = true;
					personMonth.save();
				}
			}
			
			if(cert==null)
			{
				cert = new CertificatedData(person, dipendente.getCognomeNome(), dipendente.getMatricola(), year, month);				
			}
			cert.absencesSent = rispostaElaboraDati.getAbsencesSent();
			cert.competencesSent = rispostaElaboraDati.getCompetencesSent();
			cert.mealTicketSent = rispostaElaboraDati.getMealTicketSent();
			cert.trainingHoursSent = rispostaElaboraDati.getTrainingHoursSent();
			cert.problems = rispostaElaboraDati.getProblems();
			cert.isOk = rispostaElaboraDati.isOk();
			cert.save();
			
			checks.add(rispostaElaboraDati);
		}
		
		return checks;
	}

}
