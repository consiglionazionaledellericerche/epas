package controllers;

import helpers.attestati.AttestatiClient;
import helpers.attestati.AttestatiClient.LoginResponse;
import helpers.attestati.AttestatiException;
import helpers.attestati.Dipendente;
import helpers.attestati.RispostaElaboraDati;

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

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import models.Absence;
import models.CertificatedData;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonMonthRecap;
import models.User;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperFactory;

/**
 * Contiene in metodi necessari per l'interazione tra utente, ePAS e 
 * sistema centrale del CNR per gli attestati.
 * 
 * @author cristian
 *
 */
@With( {Resecure.class, RequestInit.class} )
public class UploadSituation extends Controller{

	public static final String LOGIN_RESPONSE_CACHED = "loginResponse";
	public static final String LISTA_DIPENTENTI_CNR_CACHED = "listaDipendentiCnr";

	@Inject
	private static SecurityRules rules;
	@Inject
	private static ConfGeneralManager confGeneralManager;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static PersonDayDao personDayDao;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static AbsenceDao absenceDao;
	@Inject
	private static CompetenceDao competenceDao;
	@Inject
	private static AttestatiClient attestatiClient;
	@Inject
	private static PersonDayManager personDayManager;
	@Inject
	private static PersonMonthRecapDao personMonthRecapDao;
	@Inject
	private static IWrapperFactory factory;

	public static void show(){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		LocalDate lastMonth = LocalDate.now().minusMonths(1);

		int month = lastMonth.getMonthOfYear();
		int year = lastMonth.getYear();

		render(year, month);
	}

	public static void loginAttestati(Integer year, Integer month) {

		Office office = Security.getUser().get().person.office;
		rules.checkIfPermitted(office);

		String urlToPresence = confGeneralManager.getFieldValue(Parameter.URL_TO_PRESENCE, office);
		String userToPresence = confGeneralManager.getFieldValue(Parameter.USER_TO_PRESENCE, office);

		String attestatiLogin = params.get("attestatiLogin") == null ? userToPresence : params.get("attestatiLogin"); 

		render(year, month, urlToPresence, attestatiLogin);
	}


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

		User user = Security.getUser().get();

		rules.checkIfPermitted(user.person.office);

		List<Person> personList = personDao.getPersonsByNumber();

		Logger.debug("La lista di nomi è composta da %s persone ", personList.size());
		List<Absence> absenceList = null;
		List<Competence> competenceList = null;

		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("situazioneMensile"+year.toString()+month.toString(), ".txt" );
		inputStream = new FileInputStream( tempFile );

		FileWriter writer = new FileWriter(tempFile, true);
		try {
			BufferedWriter out = new BufferedWriter(writer);
			out.write(user.person.office.codeId);
			out.write(' ');
			out.write(new String(month.toString()+year.toString()));
			out.newLine();
			for(Person p : personList){

				absenceList = absenceDao.getAbsencesNotInternalUseInMonth(p, year, month);
				for(Absence abs : absenceList){
					out.write(p.number.toString());
					out.append(' ').append('A').append(' ')
					.append(abs.absenceType.code).append(' ')
					.append(new Integer(abs.personDay.date.getDayOfMonth()).toString()).append(' ')
					.append(new Integer(abs.personDay.date.getDayOfMonth()).toString()).append(' ')
					.append('0');
					out.newLine();
				}

				//competenceList = pm.getCompetenceInMonthForUploadSituation();
				competenceList = competenceDao.getCompetenceInMonthForUploadSituation(p, year, month);

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
			User user = Security.getUser().get();
			rules.checkIfPermitted(user.person.office);
			Cache.set(LOGIN_RESPONSE_CACHED + user.username, null);
			Cache.set(LISTA_DIPENTENTI_CNR_CACHED + user.username, null);

			if (params.get("back") != null) {
				show();
			}

			if (params.get("home") != null) {
				redirect("Application.indexAdmin");
			}

			String urlToPresence = confGeneralManager.getFieldValue(Parameter.URL_TO_PRESENCE, user.person.office); 

			try {
				//1) LOGIN

				loginResponse = attestatiClient.login(attestatiLogin, attestatiPassword, year, month); 
				if (!loginResponse.isLoggedIn()) {
					flash.error("Errore durante il login sul sistema degli attestati.");
					UploadSituation.loginAttestati(year, month);
					return;
				} 

				//2) CARICO LISTA DIPENDENTI CNR CENTRALE (ANNO-MESE)
				Logger.debug("Prendo lista dipendenti da %s. Anno = %d, mese = %d", urlToPresence, year, month);


				listaDipendenti = attestatiClient.listaDipendenti(loginResponse.getCookies(), year, month);


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

		final List<Person> activePersons = personDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, new LocalDate(year,month,1), new LocalDate(year,month,1).dayOfMonth().withMaximumValue(), true).list();

		final Set<Dipendente> activeDipendenti = FluentIterable.from(activePersons).transform(new Function<Person, Dipendente>() {
			@Override
			public Dipendente apply(Person person) {
				Dipendente dipendente = 
						new Dipendente(person, Joiner.on(" ").skipNulls().join(person.surname, person.othersSurnames, person.name));
				return dipendente;
			}
		}).toSet();
		Logger.trace("Lista dipendenti attivi nell'anno %d, mese %d e': %s", year, month, activeDipendenti);


		Set<Dipendente> dipendentiNonInEpas = getDipendenteNonInEpas(year, month, listaDipendenti, activeDipendenti);
		Set<Dipendente> dipendentiNonInCNR = getDipendenteNonInCnr(year, month, listaDipendenti, activeDipendenti);

		memAttestatiIntoCache(loginResponse, listaDipendenti);
		
		final IWrapperFactory wrapper = factory;
		
		render(year, month, activeDipendenti, dipendentiNonInEpas, dipendentiNonInCNR, loginResponse,wrapper);

	}

	public static void processAllPersons(int year, int month) throws MalformedURLException, URISyntaxException
	{
		if (params.get("back") != null) {
			UploadSituation.loginAttestati(year, month);
		}
		rules.checkIfPermitted(Security.getUser().get().person.office);
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

	public static void processSinglePerson(String matricola, int year, int month) throws MalformedURLException, URISyntaxException
	{
		if(matricola==null)
		{
			flash.error("Errore caricamento dipendente da elaborare. Riprovare o effettuare una segnalazione.");
			UploadSituation.processAttestati(null, null, year, month);
		}
		rules.checkIfPermitted(Security.getUser().get().person.office);
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

	public static void showProblems(Long certificatedDataId)
	{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CertificatedData cd = personMonthRecapDao.getCertificatedDataById(certificatedDataId);
		//CertificatedData cd = CertificatedData.findById(certificatedDataId);
		if(cd==null)
		{
			renderText("L'elaborazione attestati richiesta è inesistente.");
		}
		render(cd);
	}

	public static void showCertificatedData(Long certificatedDataId)
	{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CertificatedData cd = personMonthRecapDao.getCertificatedDataById(certificatedDataId);
		//CertificatedData cd = CertificatedData.findById(certificatedDataId);
		if(cd==null)
		{
			renderText("L'elaborazione attestati richiesta è inesistente.");
		}
		render(cd);
	}

	private static List<RispostaElaboraDati> elaboraDatiDipendenti(Map<String, String> cookies, Set<Dipendente> dipendenti, int year, int month) throws MalformedURLException, URISyntaxException {
		List<RispostaElaboraDati> checks = Lists.newLinkedList();
		Person person = null;

		for (Dipendente dipendente : dipendenti) {

			person = personDao.getPersonByNumber(Integer.parseInt(dipendente.getMatricola()));

			List<PersonMonthRecap> pmList = personMonthRecapDao.getPersonMonthRecapInYearOrWithMoreDetails(person, year, Optional.fromNullable(month), Optional.<Boolean>absent());

			//Numero di buoni mensa da passare alla procedura di invio attestati
			List<PersonDay> personDays = personDayDao
					.getPersonDayInMonth(person, new YearMonth(year, month));
			Integer mealTicket = personDayManager.numberOfMealTicketToUse(personDays);

			//vedere se l'ho gia' inviato con successo
			CertificatedData cert = personMonthRecapDao.getCertificatedDataByPersonMonthAndYear(person, month, year);
			//CertificatedData cert = CertificatedData.find("Select cert from CertificatedData cert where cert.person = ? and cert.year = ? and cert.month = ?", person, year, month).first();


			RispostaElaboraDati rispostaElaboraDati = attestatiClient.elaboraDatiDipendente(
					cookies, dipendente, year, month, 
					absenceDao.getAbsencesNotInternalUseInMonth(person, year, month),
					competenceDao.getCompetenceInMonthForUploadSituation(person, year, month),
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
			Logger.info("Inizio creazione record certificated_data");
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
		Cache.set(LOGIN_RESPONSE_CACHED+Security.getUser().get().username, loginResponse);
		Cache.set(LISTA_DIPENTENTI_CNR_CACHED+Security.getUser().get().username, listaDipendenti);
	}

	/**
	 * Carica in cache lo stato della connessione con attestati.cnr
	 * @return
	 */
	private static LoginResponse loadAttestatiLoginCached()
	{
		return (LoginResponse)Cache.get(LOGIN_RESPONSE_CACHED+Security.getUser().get().username);
	}

	/**
	 * Carica in cache la lista dipendenti abilitati in attestati.cnr
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static List<Dipendente> loadAttestatiListaCached()
	{

		return (List<Dipendente>)Cache.get(LISTA_DIPENTENTI_CNR_CACHED+Security.getUser().get().username);
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

	private static Set<Dipendente> getActiveDipendenti(int year, int month){
		
		final List<Person> activePersons = 
				personDao.list(Optional.<String>absent(),
						officeDao.getOfficeAllowed(Security.getUser().get()), false, new LocalDate(year,month,1), new LocalDate(year,month,1).dayOfMonth().withMaximumValue(), true).list();

		final Set<Dipendente> activeDipendenti = FluentIterable.from(activePersons).transform(new Function<Person, Dipendente>() {
			@Override
			public Dipendente apply(Person person) {
				Dipendente dipendente = 
						new Dipendente(person, Joiner.on(" ").skipNulls().join(person.surname, person.othersSurnames, person.name));
				return dipendente;
			}
		}).toSet();
		Logger.trace("Lista dipendenti attivi nell'anno %d, mese %d e': %s", year, month, activeDipendenti);

		return activeDipendenti;
	}

}
