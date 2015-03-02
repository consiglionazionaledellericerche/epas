package manager;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Contract;
import models.ContractStampProfile;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.User;
import models.enumerate.ConfigurationFields;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.db.jpa.JPAPlugin;
import play.libs.Mail;

import com.google.common.base.Optional;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonDayInTroubleDao;

/**
 * Manager che gestisce la consistenza e la coerenza dei dati in Epas.
 * Contiene gli algoritmi per le procedure notturne di chiusura giorno, 
 * per invio email, per check di giorni con problemi.
 * @author alessandro
 *
 */
public class ConsistencyManager {
	
	@Inject
	public OfficeDao officeDao;
	
	@Inject
	public ContractYearRecapManager contractYearRecapManager;
	
	@Inject
	public PersonDayManager personDayManager;
	
	@Inject
	public PersonDayDao personDayDao;
	
	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone attive alla data di ieri
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 * @param userLogged
	 * @throws EmailException 
	 */
	public void fixPersonSituation(Long personId, int year, int month, User userLogged, boolean sendEmail){

		if(userLogged==null)
			return;

		// (0) Costruisco la lista di persone su cui voglio operare
		List<Person> personList = new ArrayList<Person>();
		if(personId==-1)
			personId=null;
		if(personId==null) {
			
			LocalDate begin = new LocalDate(year, month, 1);
			LocalDate end = new LocalDate().minusDays(1);
			personList = PersonDao.list(Optional.<String>absent(), 
					officeDao.getOfficeAllowed(Optional.fromNullable(userLogged)), false, begin, end, true).list();
		}
		else {
			
			//TODO controllare che personLogged abbia i diritti sulla persona
			personList.add(PersonDao.getPersonById(personId));
		}
		
		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		JPAPlugin.startTx(false);
		for(Person person : personList) {
				checkHistoryError(person, year, month);
		}
		JPAPlugin.closeTx(false);

		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		int i = 1;
		for(Person p : personList){
			Logger.info("Update person situation %s (%s di %s) dal %s-%s-01 a oggi", p.surname, i++, personList.size(), year, month);

			LocalDate actualMonth = new LocalDate(year, month, 1);
			LocalDate endMonth = new LocalDate().withDayOfMonth(1);
			JPAPlugin.startTx(false);
			while(!actualMonth.isAfter(endMonth)) {

				List<PersonDay> pdList = personDayDao
						.getPersonDayInPeriod(p, actualMonth,
								Optional.fromNullable(actualMonth.dayOfMonth().withMaximumValue()), true);

				for(PersonDay pd : pdList){
					JPAPlugin.closeTx(false);
					JPAPlugin.startTx(false);
					PersonDay pd1 = personDayDao.getPersonDayById(pd.id);
					personDayManager.populatePersonDay(pd1);
					JPAPlugin.closeTx(false);
					JPAPlugin.startTx(false);
				}

				actualMonth = actualMonth.plusMonths(1);
			}
			JPAPlugin.closeTx(false);
		}
		
		//(3) 
		JPAPlugin.startTx(false);
		i = 1;
		for(Person p : personList) {
			
			JPAPlugin.closeTx(false);	
			JPAPlugin.startTx(false);
			p = PersonDao.getPersonById(p.id);
			
			Logger.info("Update residui %s (%s di %s) dal %s-%s-01 a oggi", p.surname, i++, personList.size(), year, month);
			List<Contract> contractList = ContractDao.getPersonContractList(p);

			for(Contract contract : contractList) {

				contractYearRecapManager.buildContractYearRecap(contract);
			}
		}
		JPAPlugin.closeTx(false);		
		
		
		//(4) Invio mail per controllo timbrature da farsi solo nei giorni feriali
		if( sendEmail ) {
			
			if( LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY 
					&& LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY){

				JPAPlugin.startTx(false);
				LocalDate begin = new LocalDate().minusMonths(1);
				LocalDate end = new LocalDate().minusDays(1);

				for(Person p : personList){
					
					Logger.debug("Chiamato controllo sul giorni %s %s", begin, end);
					if(p.wantEmail)
						checkPersonDayForSendingEmail(p, begin, end, "timbratura");
					else
						Logger.info("Non verrà inviata la mail a %s %s in quanto il campo di invio mail è false", p.name, p.surname);

				}
				JPAPlugin.closeTx(false);
			}
		}


	}
	
	/**
	 * Metodo che controlla i giorni con problemi dei dipendenti che non hanno timbratura fixed
	 *  e invia mail nel caso in cui esistano timbrature disaccoppiate.
	 * @param p
	 * @param begin
	 * @param end
	 * @throws EmailException
	 */
	private static void checkPersonDayForSendingEmail(Person p, LocalDate begin, LocalDate end, String cause) {

		if(p.surname.equals("Conti") && p.name.equals("Marco")) {
			
			Logger.debug("Trovato Marco Conti, capire cosa fare con la sua situazione...");
			return;
		}
		
		List<PersonDayInTrouble> pdList = PersonDayInTroubleDao.getPersonDayInTroubleInPeriod(p, begin, end, false);

		List<LocalDate> dateTroubleStampingList = new ArrayList<LocalDate>();

		for(PersonDayInTrouble pdt : pdList){
			
			Contract contract = ContractDao.getContract(pdt.personDay.date, pdt.personDay.person);
			if(contract == null) {
				
				Logger.error("Individuato PersonDayInTrouble al di fuori del contratto. Person: %s %s - Data: %s",
						p.surname, p.name, pdt.personDay.date);
				continue;
			}
			
			ContractStampProfile csp = ContractManager.getContractStampProfileFromDate(contract, pdt.personDay.date);
			if(csp.fixedworkingtime == true) {
				continue;
			}
			
			if(pdt.cause.contains(cause) && !pdt.personDay.isHoliday() && pdt.fixed == false) { 
				dateTroubleStampingList.add(pdt.personDay.date);
			}
		}

		boolean flag;
		try {

			flag = sendEmailToPerson(dateTroubleStampingList, p, cause);

		} catch (Exception e) {

			Logger.debug("sendEmailToPerson(dateTroubleStampingList, p, cause): fallito invio email per %s %s", p.name, p.surname); 
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
	public void checkNoAbsenceNoStamping(int year, int month, User userLogged) throws EmailException{

		LocalDate begin = new LocalDate(year, month, 1);
		LocalDate end = new LocalDate().minusDays(1);

		List<Person> personList = PersonDao.list(Optional.<String>absent(),
					officeDao.getOfficeAllowed(Optional.fromNullable(userLogged)), false, begin, end, true).list();
		
		for(Person p : personList){
		
			Logger.debug("Chiamato controllo sul giorni %s %s", begin, end);
			
			if(p.wantEmail) {
				checkPersonDayForSendingEmail(p, begin, end, "no assenze");
			}
			else {
				Logger.info("Non verrà inviata la mail a %s %s in quanto il campo di invio mail è false", p.name, p.surname);
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
	public void checkPersonDay(Long personid, LocalDate dayToCheck)
	{
		Person personToCheck = PersonDao.getPersonById(personid);

		if(!PersonManager.isActiveInDay(dayToCheck, personToCheck)){
			return;
		}
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getSinglePersonDay(personToCheck, dayToCheck);

		if(pd.isPresent()){
			personDayManager.checkForPersonDayInTrouble(pd.get()); 
			return;
		}
		else {
			personDay = new PersonDay(personToCheck, dayToCheck);
			if(personDay.isHoliday()) {
				return;
			}
			personDay.create();
			personDayManager.populatePersonDay(personDay);
			personDay.save();
			personDayManager.checkForPersonDayInTrouble(personDay);
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
	private void checkHistoryError(Person person, int year, int month)
	{
		Logger.info("Check history error %s dal %s-%s-1 a oggi", person.surname, year, month);
		LocalDate date = new LocalDate(year,month,1);
		LocalDate today = new LocalDate();
		
		while(true) {
			
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			
			checkPersonDay(person.id, date);
			date = date.plusDays(1);
			if(date.isEqual(today))
				break;
			
		}

	}
	
	/**
	 * Invia la mail alla persona specificata in firma con la lista dei giorni in cui ha timbrature disaccoppiate
	 * @param date, person
	 * @throws EmailException 
	 */
	private static boolean sendEmailToPerson(List<LocalDate> dateList, Person person, String cause) throws EmailException{
		if(dateList.size() == 0){
			return false;
		}
		Logger.info("Preparo invio mail per %s %s", person.name, person.surname);
		SimpleEmail simpleEmail = new SimpleEmail();
		try {
			simpleEmail.setFrom("epas@iit.cnr.it");
			//simpleEmail.addReplyTo("segreteria@iit.cnr.it");
			simpleEmail.addReplyTo(ConfGeneralManager.getConfGeneralByField(
							ConfigurationFields.EmailToContact.description, 
							person.office).fieldValue);
		} catch (EmailException e1) {

			e1.printStackTrace();
		}
		try {
			simpleEmail.addTo(person.email);
			//simpleEmail.addTo("dario.tagliaferri@iit.cnr.it");
		} catch (EmailException e) {

			e.printStackTrace();
		}
		List<LocalDate> dateFormat = new ArrayList<LocalDate>();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-YYYY");		
		String date = "";
		for(LocalDate d : dateList){
			if(!DateUtility.isGeneralHoliday(person.office, d)){
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

		Logger.info("Inviata mail a %s %s contenente le date da controllare : %s", person.name, person.surname, date);
		return true;

	}
	
}
