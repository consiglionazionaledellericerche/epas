package controllers;

import helpers.attestati.AttestatiClient;
import helpers.attestati.AttestatiClient.LoginResponse;
import helpers.attestati.AttestatiException;
import helpers.attestati.Dipendente;
import helpers.attestati.RispostaElaboraDati;
import it.cnr.iit.epas.PersonUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.util.HSSFColor.ROSE;
import org.joda.time.LocalDate;

import models.Absence;
import models.CertificatedData;
import models.Competence;
import models.ConfGeneral;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.enumerate.ConfigurationFields;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
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
@With( {Secure.class, RequestInit.class} )
public class UploadSituation extends Controller{
	
	public static final String LOGIN_RESPONSE_CACHED = "loginResponse";
	public static final String LISTA_DIPENTENTI_CNR_CACHED = "listaDipendentiCnr";
	
	@Check(Security.UPLOAD_SITUATION)
	public static void show(){
		LocalDate lastMonth = LocalDate.now().minusMonths(1);
		
		int month = lastMonth.getMonthOfYear();
		int year = lastMonth.getYear();
			
		render(year, month);
	}

	@Check(Security.UPLOAD_SITUATION)
	public static void loginAttestati(Integer year, Integer month) {

		Office office = Security.getUser().person.office;
		
		String urlToPresence = ConfGeneral.getFieldValue(ConfigurationFields.UrlToPresence.description, office);
		String userToPresence = ConfGeneral.getFieldValue(ConfigurationFields.UserToPresence.description, office);
		
		String attestatiLogin = params.get("attestatiLogin") == null ? userToPresence : params.get("attestatiLogin"); 

		render(year, month, urlToPresence, attestatiLogin);
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

//		ConfGeneral conf = ConfGeneral.getConfGeneral();
		Integer seatCode = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.SeatCode.description, Security.getUser().person.office));
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
			out.write(seatCode.toString());
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


	
	
	@Check(Security.UPLOAD_SITUATION)
	public static void processAttestati(final String attestatiLogin, final String attestatiPassword, Integer year, Integer month) throws MalformedURLException, URISyntaxException
	{
		
		LoginResponse loginResponse = null;
		List<Dipendente> listaDipendenti = null;

		if(attestatiLogin==null && attestatiPassword==null)
		{
			loginResponse = loadAttestatiLoginCached();
			listaDipendenti = loadAttestatiListaCached();
			if(loginResponse==null || !loginResponse.isLoggedIn() || listaDipendenti == null || listaDipendenti.size()==0)
			{
				flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
				UploadSituation.loginAttestati(year, month);
			}
		}
		else
		{
			Cache.set(LOGIN_RESPONSE_CACHED+Security.getUser().username, null);
			Cache.set(LISTA_DIPENTENTI_CNR_CACHED+Security.getUser().username, null);
			
			if (params.get("back") != null) {
				show();
			}

			if (params.get("home") != null) {
				redirect("Application.indexAdmin");
			}

			String urlToPresence = ConfGeneral.getFieldValue(ConfigurationFields.UrlToPresence.description, Security.getUser().person.office);
			
			try {
				//1) LOGIN
				
				loginResponse = AttestatiClient.login(attestatiLogin, attestatiPassword, year, month); 
				if (!loginResponse.isLoggedIn()) {
					flash.error("Errore durante il login sul sistema degli attestati.");
					UploadSituation.loginAttestati(year, month);
					return;
				} 
				
				//2) CARICO LISTA DIPENDENTI CNR CENTRALE (ANNO-MESE)
				Logger.debug("Prendo lista dipendenti da %s. Anno = %d, mese = %d", urlToPresence, year, month);

				
				listaDipendenti = AttestatiClient.listaDipendenti(loginResponse.getCookies(), year, month);
				

			} catch (AttestatiException e) {
				flash.error(
						String.format("Errore durante il login e/o prelevamento della lista dei dipendenti dal sistema degli attestati. Eccezione: %s", e));
				UploadSituation.loginAttestati(year, month);
			}

			if (listaDipendenti == null || listaDipendenti.isEmpty()) {
				flash.error("Errore durante il prelevamento della lista dei dipendenti dal sistema degli attestati.");
				UploadSituation.loginAttestati(year, month);
			}
		}
		
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


		Set<Dipendente> dipendentiNonInEpas = getDipendenteNonInEpas(year, month, listaDipendenti, activeDipendenti);
		Set<Dipendente> dipendentiNonInCNR = getDipendenteNonInCnr(year, month, listaDipendenti, activeDipendenti);

		memAttestatiIntoCache(loginResponse, listaDipendenti);
		

		render(year, month, activeDipendenti, dipendentiNonInEpas, dipendentiNonInCNR, loginResponse);
		
	}

	@Check(Security.UPLOAD_SITUATION)
	public static void processAllPersons(int year, int month) throws MalformedURLException, URISyntaxException
	{
		if (params.get("back") != null) {
			UploadSituation.loginAttestati(year, month);
		}
		
		LoginResponse loginResponse = loadAttestatiLoginCached();
		List<Dipendente> listaDipendenti = loadAttestatiListaCached();
		
		if(loginResponse==null || !loginResponse.isLoggedIn() || listaDipendenti == null || listaDipendenti.size()==0)
		{
			flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
			UploadSituation.loginAttestati(year, month);
		}

		Set<Dipendente> activeDipendenti = getActiveDipendenti(year, month);

		List<RispostaElaboraDati> checks = elaboraDatiDipendenti( 
						loginResponse.getCookies(), 
						Sets.intersection(ImmutableSet.copyOf(listaDipendenti), activeDipendenti), 
						year, month);
		
		Predicate<RispostaElaboraDati> rispostaOk = new Predicate<RispostaElaboraDati>() {
			@Override
			public boolean apply(RispostaElaboraDati risposta) {
				return risposta.getProblems() == null || risposta.getProblems().isEmpty();
			}
		};

		List<RispostaElaboraDati> risposteNotOk = FluentIterable.from(checks).filter(Predicates.not(rispostaOk)).toList();
		
		if(risposteNotOk.isEmpty())
			flash.success("Elaborazione dipendenti effettuata senza errori.");
		else if(risposteNotOk.size()==1)
			flash.error("Elaborazione dipendenti effettuata. Sono stati riscontrati problemi per 1 dipendente. Controllare l'esito.");
		else
			flash.error("Elaborazione dipendenti effettuata. Sono stati riscontrati problemi per %s dipendenti. Controllare l'esito.",
					risposteNotOk.size());
			
		UploadSituation.processAttestati(null, null, year, month);


	}
	
	@Check(Security.UPLOAD_SITUATION)
	public static void processSinglePerson(String matricola, int year, int month) throws MalformedURLException, URISyntaxException
	{
		if(matricola==null)
		{
			flash.error("Errore caricamento dipendente da elaborare. Riprovare o effettuare una segnalazione.");
			UploadSituation.processAttestati(null, null, year, month);
		}
		
		LoginResponse loginResponse = loadAttestatiLoginCached();
		List<Dipendente> listaDipendenti = loadAttestatiListaCached();
		
		if(loginResponse==null || !loginResponse.isLoggedIn() || listaDipendenti == null || listaDipendenti.size()==0)
		{
			flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
			UploadSituation.loginAttestati(year, month);
		}

		Set<Dipendente> activeDipendentiCached = getActiveDipendenti(year, month);
		
		Dipendente dipendente = null;
		for(Dipendente dip : activeDipendentiCached)
		{
			if(dip.getMatricola().equals(matricola))
			{
				dipendente = dip;
				break;
			}
		}
		
		if(dipendente==null)
		{
			flash.error("Errore caricamento dipendente da elaborare. Riprovare o effettuare una segnalazione.");
			UploadSituation.processAttestati(null, null, year, month);
		}

		Set<Dipendente> activeDipendenti = new HashSet<Dipendente>();
		activeDipendenti.add(dipendente);



		List<RispostaElaboraDati> checks = elaboraDatiDipendenti( 
				loginResponse.getCookies(), 
				Sets.intersection(ImmutableSet.copyOf(listaDipendenti), activeDipendenti), 
				year, month);

		Predicate<RispostaElaboraDati> rispostaOk = new Predicate<RispostaElaboraDati>() {
			@Override
			public boolean apply(RispostaElaboraDati risposta) {
				return risposta.getProblems() == null || risposta.getProblems().isEmpty();
			}
		};

		List<RispostaElaboraDati> risposteNotOk = FluentIterable.from(checks).filter(Predicates.not(rispostaOk)).toList();

		if(risposteNotOk.isEmpty())
			flash.success("Elaborazione dipendente effettuata senza errori.");
		else 
			flash.error("Elaborazione dipendente effettuata. Sono stati riscontrati problemi per 1 dipendente. Controllare l'esito.");



		UploadSituation.processAttestati(null, null, year, month);

	}

	@Check(Security.UPLOAD_SITUATION)
	public static void showProblems(Long certificatedDataId)
	{
		CertificatedData cd = CertificatedData.findById(certificatedDataId);
		if(cd==null)
		{
			renderText("L'elaborazione attestati richiesta è inesistente.");
		}
		render(cd);
	}
	
	@Check(Security.UPLOAD_SITUATION)
	public static void showCertificatedData(Long certificatedDataId)
	{
		CertificatedData cd = CertificatedData.findById(certificatedDataId);
		if(cd==null)
		{
			renderText("L'elaborazione attestati richiesta è inesistente.");
		}
		render(cd);
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
				//FIXME
				//queste variabili di appoggio sono state inserite perchè richiamandole direttamente nel costruttore veniva lanciata l'eccezione
				//play.exceptions.JavaExecutionException: models.CertificatedData.<init>(Lmodels/Person;Ljava/lang/String;Ljava/lang/String;II)V
				int anno = year;
				int mese = month;
				String cognomeNome = dipendente.getCognomeNome();
				String matricola = dipendente.getMatricola();
				cert = new CertificatedData(person, cognomeNome, matricola, anno, mese);				
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
	
	private static void memAttestatiIntoCache(LoginResponse loginResponse, List<Dipendente> listaDipendenti)
	{
		Cache.set(LOGIN_RESPONSE_CACHED+Security.getUser().username, loginResponse);
		Cache.set(LISTA_DIPENTENTI_CNR_CACHED+Security.getUser().username, listaDipendenti);
	}
	
	/**
	 * Carica in cache lo stato della connessione con attestati.cnr
	 * @return
	 */
	private static LoginResponse loadAttestatiLoginCached()
	{
		return (LoginResponse)Cache.get(LOGIN_RESPONSE_CACHED+Security.getUser().username);
	}
	
	/**
	 * Carica in cache la lista dipendenti abilitati in attestati.cnr
	 * @return
	 */
	private static List<Dipendente> loadAttestatiListaCached()
	{
		return (List<Dipendente>)Cache.get(LISTA_DIPENTENTI_CNR_CACHED+Security.getUser().username);
	}
	
	private static Set<Dipendente> getDipendenteNonInEpas(int year, int month, List<Dipendente> listaDipendenti,  Set<Dipendente> activeDipendenti)
	{
		Set<Dipendente> dipendentiNonInEpas = Sets.difference(ImmutableSet.copyOf(listaDipendenti), activeDipendenti);
		if (dipendentiNonInEpas.size() > 0)
			Logger.info("I seguenti dipendenti sono nell'anagrafica CNR ma non in ePAS. %s", dipendentiNonInEpas);

		return dipendentiNonInEpas;
	}

	private static Set<Dipendente> getDipendenteNonInCnr(int year, int month, List<Dipendente> listaDipendenti, Set<Dipendente> activeDipendenti)
	{		
		Set<Dipendente> dipendentiNonInCNR = Sets.difference(activeDipendenti, ImmutableSet.copyOf(listaDipendenti));
		if (dipendentiNonInCNR.size() > 0)
			Logger.info("I seguenti dipendenti sono nell'anagrafica di ePAS ma non in quella del CNR. %s", dipendentiNonInCNR);

		return dipendentiNonInCNR;
	}
	
	private static Set<Dipendente> getActiveDipendenti(int year, int month)
	{
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
		
		return activeDipendenti;
	}

	
}
