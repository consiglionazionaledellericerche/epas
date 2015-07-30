package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Contract;
import models.ContractStampProfile;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.enumerate.Parameter;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.libs.Mail;

import com.google.common.base.Optional;

import dao.ContractDao;
import dao.PersonDayDao;
import dao.PersonDayInTroubleDao;
import dao.wrapper.IWrapperFactory;

public class PersonDayInTroubleManager {

	@Inject
	public PersonDayInTroubleManager(
			PersonDayInTroubleDao personDayInTroubleDao,
			ConfGeneralManager confGeneralManager, 
			PersonDayDao personDayDao, ContractDao contractDao,
			IWrapperFactory factory) {

		this.contractDao = contractDao;
		this.personDayInTroubleDao = personDayInTroubleDao;
		this.confGeneralManager = confGeneralManager;
		this.factory = factory;
	}

	private final IWrapperFactory factory;
	private final ContractDao contractDao;
	private final PersonDayInTroubleDao personDayInTroubleDao;
	private final ConfGeneralManager confGeneralManager;
	
	
	private final static Logger log = LoggerFactory.getLogger(PersonDayInTroubleManager.class);
	/**
	 * 
	 * @param pd
	 * @param cause
	 */
	public void insertPersonDayInTrouble(PersonDay pd, String cause)
	{
		if(pd.troubles==null || pd.troubles.size()==0)
		{	
			//se non esiste lo creo
			log.info("Nuovo PersonDayInTrouble {} - {} - {}", 
					new Object[]{pd.person.getFullname(), pd.date, cause});
			PersonDayInTrouble trouble = new PersonDayInTrouble(pd, cause);
			trouble.save();
			pd.troubles.add(trouble);
			pd.save();
			return;
		}
		else
		{
			//se esiste lo setto fixed = false;
			pd.troubles.get(0).fixed = false;
			pd.troubles.get(0).cause = cause;
			pd.troubles.get(0).save();
			pd.save();
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
	private void checkPersonDayForSendingEmail(Person p, LocalDate begin, LocalDate end, String cause) {

		if(p.surname.equals("Conti") && p.name.equals("Marco")) {

			log.debug("Trovato Marco Conti, capire cosa fare con la sua situazione...");
			return;
		}
		
		Contract currentActiveContract = factory.create(p).getCurrentContract().orNull();
//		Se la persona e' fuori contratto non si prosegue con i controlli
		if(currentActiveContract == null){
			return;
		}
		
		DateInterval intervalToCheck = DateUtility.intervalIntersection(
				factory.create(currentActiveContract).getContractDateInterval(),
				new DateInterval(begin, end));
		
		List<PersonDayInTrouble> pdList = personDayInTroubleDao
				.getPersonDayInTroubleInPeriod(p, intervalToCheck.getBegin(),
						intervalToCheck.getEnd(), false);

		List<LocalDate> dateTroubleStampingList = new ArrayList<LocalDate>();
		

		for(PersonDayInTrouble pdt : pdList){

			Optional<ContractStampProfile> csp = currentActiveContract
					.getContractStampProfileFromDate(pdt.personDay.date);

			if(csp.isPresent() && csp.get().fixedworkingtime == true) {
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
