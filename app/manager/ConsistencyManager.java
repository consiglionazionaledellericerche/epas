package manager;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import models.Contract;
import models.ContractStampProfile;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.User;
import models.enumerate.Parameter;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.db.jpa.JPAPlugin;
import play.libs.Mail;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonDayInTroubleDao;
import dao.wrapper.IWrapperFactory;

/**
 * Manager che gestisce la consistenza e la coerenza dei dati in Epas.
 * Contiene gli algoritmi per le procedure notturne di chiusura giorno, 
 * per invio email, per check di giorni con problemi.
 * @author alessandro
 *
 */
public class ConsistencyManager {

	@Inject
	public ConsistencyManager(OfficeDao officeDao, 
			PersonManager personManager,
			PersonDao personDao, 
			PersonDayManager personDayManager,
			ContractDao contractDao,
			ContractMonthRecapManager contractMonthRecapManager,
			PersonDayInTroubleDao personDayInTroubleDao,
			IWrapperFactory wrapperFactory,
			ConfGeneralManager confGeneralManager, 
			PersonDayDao personDayDao) {

		this.officeDao = officeDao;
		this.personManager = personManager;
		this.personDao = personDao;
		this.personDayManager = personDayManager;
		this.contractDao = contractDao;
		this.contractMonthRecapManager = contractMonthRecapManager;
		this.personDayInTroubleDao = personDayInTroubleDao;
		this.wrapperFactory = wrapperFactory;
		this.confGeneralManager = confGeneralManager;
		this.personDayDao = personDayDao;
	}

	private final static Logger log = LoggerFactory.getLogger(ConsistencyManager.class);

	private final OfficeDao officeDao;
	private final PersonManager personManager;
	private final PersonDao personDao;
	private final PersonDayManager personDayManager;
	private final ContractDao contractDao;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final PersonDayInTroubleDao personDayInTroubleDao;
	private final IWrapperFactory wrapperFactory;
	private final ConfGeneralManager confGeneralManager;
	private final PersonDayDao personDayDao;

	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone attive alla data di ieri
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 * @param userLogged
	 * @throws EmailException 
	 */
	@SuppressWarnings("deprecation")
	public void fixPersonSituation(Optional<Person> person,Optional<User> user,
			LocalDate fromDate, boolean sendMail){

		Set<Office> offices = user.isPresent() ? officeDao.getOfficeAllowed(user.get()) : Sets.newHashSet(officeDao.getAllOffices());

		//  (0) Costruisco la lista di persone su cui voglio operare
		List<Person> personList = Lists.newArrayList();

		if(person.isPresent() && user.isPresent()){
			if(personManager.isAllowedBy(user.get(), person.get()))
				personList.add(person.get());
		}
		else {
			personList = personDao.list(Optional.<String>absent(), offices,
					false, fromDate, LocalDate.now().minusDays(1), true).list();
		}

		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		
		for(Person p : personList) {
			
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			p = personDao.getPersonById(p.id);
			
			// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
			checkHistoryError(p, fromDate);
			
			// (2) Ricalcolo i valori dei person day	
			log.info("Update person situation {} dal {} a oggi", p.getFullname(), fromDate);
			personDayManager.updatePersonDaysFromDate(p, fromDate);
			
			// (3) Ricalcolo dei residui per mese
			log.info("Update residui mensili {} dal {} a oggi", p.getFullname(), fromDate);
			contractMonthRecapManager.populateContractMonthRecapByPerson(p,
					new YearMonth(fromDate));

		}
		
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);

		if(sendMail && LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY 
				&& LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY){

			LocalDate begin = new LocalDate().minusMonths(1);
			LocalDate end = new LocalDate().minusDays(1);

			try {
				sendMail(personList, begin, end, "timbratura");
			}
			catch(EmailException e){
				e.printStackTrace();
			}
		}
		JPAPlugin.closeTx(false);	
	}

	/**
	 * Metodo che controlla i giorni con problemi dei dipendenti che non hanno timbratura fixed
	 *  e invia mail nel caso in cui esistano timbrature disaccoppiate.
	 * @param p
	 * @param begin
	 * @param end
	 * @throws EmailException
	 */
	private void checkPersonDayForSendingEmail(Person p, LocalDate begin, LocalDate end, String cause) {

		if(p.surname.equals("Conti") && p.name.equals("Marco")) {

			log.debug("Trovato Marco Conti, capire cosa fare con la sua situazione...");
			return;
		}

		List<PersonDayInTrouble> pdList = personDayInTroubleDao.getPersonDayInTroubleInPeriod(p, begin, end, false);

		List<LocalDate> dateTroubleStampingList = new ArrayList<LocalDate>();

		for(PersonDayInTrouble pdt : pdList){

			Contract contract = contractDao.getContract(pdt.personDay.date, pdt.personDay.person);
			if(contract == null) {

				log.error("Individuato PersonDayInTrouble al di fuori del contratto. Person: {} - Data: {}",
						p.getFullname(), pdt.personDay.date);
				continue;
			}

			Optional<ContractStampProfile> csp = contract.getContractStampProfileFromDate(pdt.personDay.date);

			Preconditions.checkState(csp.isPresent());

			if(csp.get().fixedworkingtime == true) {
				continue;
			}

			if(pdt.cause.contains(cause) && !pdt.personDay.isHoliday
					&& pdt.fixed == false) { 
				dateTroubleStampingList.add(pdt.personDay.date);
			}
		}

		boolean flag;
		try {

			flag = sendEmailToPerson(dateTroubleStampingList, p, cause);

		} catch (Exception e) {

			log.error("sendEmailToPerson({}, {}, {}): fallito invio email per {}",
					new Object[] {dateTroubleStampingList, p, cause,p.getFullname()}); 
			e.printStackTrace();
			return;
		}

		//se ho inviato mail devo andare a settare 'true' i campi emailSent dei personDayInTrouble relativi 
		if(flag){
			for(PersonDayInTrouble pd : pdList){
				pd.emailSent = true;
				pd.save();
			}
		}
	}

	/**
	 * Controlla ogni due giorni la presenza di giorni in cui non ci siano
	 *  nè assenze nè timbrature per tutti i dipendenti (invocato nell'expandableJob)
	 * @param personId
	 * @param year
	 * @param month
	 * @param userLogged
	 * @throws EmailException 
	 */
	public void sendMail(List<Person> personList, LocalDate fromDate,LocalDate toDate,String cause) throws EmailException{

		for(Person p : personList){

			log.debug("Chiamato controllo sul giorni {}-{}", fromDate, toDate);

			boolean officeMail = confGeneralManager.getBooleanFieldValue(Parameter.SEND_EMAIL, p.office);

			if(p.wantEmail && officeMail) {
				checkPersonDayForSendingEmail(p, fromDate, toDate, cause);
			}
			else {
				log.info("Non verrà inviata la mail a {} in quanto il campo di invio mail è false", p.getFullname());
			}
		}
	}

	/**
	 * Verifica per la persona (se attiva) che alla data 
	 * 	(1) in caso di giorno lavorativo il person day esista. 
	 * 		Altrimenti viene creato e persistito un personday vuoto e 
	 *      inserito un record nella tabella PersonDayInTrouble.
	 * 	(2) il person day presenti una situazione di timbrature corretta dal punto di vista logico. 
	 * 		In caso contrario viene inserito un record nella tabella PersonDayInTrouble. 
	 *      Situazioni di timbrature errate si verificano nei casi 
	 *  	(a) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 		(b) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature.
	 *  
	 * @param personid la persona da controllare
	 * @param dayToCheck il giorno da controllare
	 */
	public void checkPersonDay(Person person, LocalDate dayToCheck){

		if(!personManager.isActiveInDay(dayToCheck, person)){
			return;
		}
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getSinglePersonDay(person, dayToCheck);

		if(pd.isPresent()){
			personDayManager.checkForPersonDayInTrouble(wrapperFactory.create(pd.get())); 
			return;
		}
		else {
			personDay = new PersonDay(person, dayToCheck);
			if (personDay.isHoliday) {
				return;
			}
			personDay.create();
			personDayManager.populatePersonDay(wrapperFactory.create(personDay));
			personDay.save();
			personDayManager.checkForPersonDayInTrouble(wrapperFactory.create(personDay));
			return;
		}
	}


	/**
	 * A partire dal mese e anno passati al metodo fino al giorno di ieri (yesterday)
	 * controlla la presenza di errori nelle timbrature, inserisce i giorni problematici nella tabella PersonDayInTrouble
	 * e setta a fixed true quelli che in passato avevano problemi e che invece sono stati risolti.
	 * @param personid la persona da controllare
	 * @param year l'anno di partenza
	 * @param month il mese di partenza
	 */
	private void checkHistoryError(Person person, LocalDate from){
		log.info("Check history error {} dal {} a oggi", person.getFullname(), from);

		LocalDate date = from;
		LocalDate today = LocalDate.now();

		while(date.isBefore(today)) {

			checkPersonDay(person, date);
			date = date.plusDays(1);

		}
	}

	/**
	 * Invia la mail alla persona specificata in firma con la lista dei giorni in cui ha timbrature disaccoppiate
	 * @param date, person
	 * @throws EmailException 
	 */
	private boolean sendEmailToPerson(List<LocalDate> dateList, Person person, String cause) throws EmailException{
		if(dateList.size() == 0){
			return false;
		}
		log.info("Preparo invio mail per {}", person.getFullname());
		SimpleEmail simpleEmail = new SimpleEmail();
		try {
			simpleEmail.setFrom(Play.configuration.getProperty("application.mail.address"));
			simpleEmail.addReplyTo(confGeneralManager.getFieldValue(Parameter.EMAIL_TO_CONTACT, person.office) );
		} catch (EmailException e1) {
			e1.printStackTrace();
		}
		try {
			simpleEmail.addTo(person.email);
		} catch (EmailException e) {

			e.printStackTrace();
		}
		List<LocalDate> dateFormat = new ArrayList<LocalDate>();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-YYYY");		
		String date = "";
		for(LocalDate d : dateList){
			if(!DateUtility.isGeneralHoliday(confGeneralManager.officePatron(person.office), d)){
				dateFormat.add(d);
				String str = fmt.print(d);
				date = date+str+", ";
			}
		}
		String incipit = "";
		if(dateFormat.size() == 0)
			return false;
		if(dateFormat.size() > 1)
			incipit = "Nei giorni: ";
		if(dateFormat.size() == 1)
			incipit = "Nel giorno: ";	

		simpleEmail.setSubject("ePas Controllo timbrature");
		String message = "";
		if(cause.equals("timbratura")){
			message = "Gentile " +person.name+" "+person.surname+ 
					"\r\n" + incipit+date+ " il sistema ePAS ha rilevato un caso di timbratura disaccoppiata. \r\n " +
					"La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione. \r\n" +
					"Saluti \r\n"+
					"Il team di ePAS";

		}
		if(cause.equals("no assenze")){
			message = "Gentile " +person.name+" "+person.surname+ 
					"\r\n" + incipit+date+ " il sistema ePAS ha rilevato un caso di mancanza di timbrature e di codici di assenza. \r\n " +
					"La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione. \r\n" +
					"Saluti \r\n"+
					"Il team di ePAS";
		}

		simpleEmail.setMsg(message);

		Mail.send(simpleEmail);

		log.info("Inviata mail a {} contenente le date da controllare : {}", person.getFullname(), date);
		return true;

	}

}
