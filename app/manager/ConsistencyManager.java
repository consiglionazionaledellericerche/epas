package manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.cache.StampTypeManager;
import models.Office;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.Parameter;

import org.apache.commons.mail.EmailException;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPAPlugin;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

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
			ContractMonthRecapManager contractMonthRecapManager,
			PersonDayInTroubleManager personDayInTroubleManager,
			IWrapperFactory wrapperFactory,
			PersonDayDao personDayDao, ConfYearManager confYearManager, 
			StampTypeManager stampTypeManager) {

		this.officeDao = officeDao;
		this.personManager = personManager;
		this.personDao = personDao;
		this.personDayManager = personDayManager;
		this.contractMonthRecapManager = contractMonthRecapManager;
		this.personDayInTroubleManager = personDayInTroubleManager;
		this.wrapperFactory = wrapperFactory;
		this.personDayDao = personDayDao;
		this.confYearManager = confYearManager;
		this.stampTypeManager = stampTypeManager;
	}

	private final static Logger log = LoggerFactory.getLogger(ConsistencyManager.class);

	private final OfficeDao officeDao;
	private final PersonManager personManager;
	private final PersonDao personDao;
	private final PersonDayManager personDayManager;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final PersonDayInTroubleManager personDayInTroubleManager;
	private final IWrapperFactory wrapperFactory;
	private final ConfYearManager confYearManager;
	private final PersonDayDao personDayDao;
	private final StampTypeManager stampTypeManager;

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

		Set<Office> offices = user.isPresent() ? 
				officeDao.getOfficeAllowed(user.get()) 
				: Sets.newHashSet(officeDao.getAllOffices());

		//  (0) Costruisco la lista di persone su cui voglio operare
		List<Person> personList = Lists.newArrayList();

		if(person.isPresent() && user.isPresent()){
			if(personManager.isAllowedBy(user.get(), person.get()))
				personList.add(person.get());
		} else {
			personList = personDao.list(Optional.<String>absent(), offices,
					false, fromDate, LocalDate.now().minusDays(1), true).list();
		}

		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		
		for(Person p : personList) {
			
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			p = personDao.getPersonById(p.id);
			
			updatePersonSituation(p, fromDate);
		}
		
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);

		if(sendMail && LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY 
				&& LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY){

			LocalDate begin = new LocalDate().minusMonths(1);
			LocalDate end = new LocalDate().minusDays(1);

			try {
				personDayInTroubleManager.sendMail(personList, begin, end, "timbratura");
			}
			catch(EmailException e){
				e.printStackTrace();
			}
		}
		JPAPlugin.closeTx(false);	
	}

	

	/**
	 * Controlla la presenza di errori nelle timbrature. 
	 * Gestisce i giorni problematici nella tabella PersonDayInTrouble.
	 * Effettua i ricalcoli giornialieri e mensili.
	 * @param person
	 * @param from
	 */
	public void updatePersonSituation(Person person, LocalDate from){
		log.info("Check history error {} dal {} a oggi", person.getFullname(), from);

		LocalDate date = from;
		LocalDate today = LocalDate.now();
		
		person = personDao.fetchPersonForComputation(person.id, 
				Optional.fromNullable(from), 
				Optional.fromNullable(today));
		
		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, from, 
				Optional.of(today));

		//Costruire la tabella hash
		HashMap<LocalDate, PersonDay> personDaysMap = Maps.newHashMap();
		for(PersonDay personDay : personDays) {
			personDaysMap.put(personDay.date, personDay);
		}
		
		PersonDay previous = null;
		
		while(date.isBefore(today)) {
			
			if(!personManager.isActiveInDay(date, person)) {
				date = date.plusDays(1);
				previous = null;
				continue;
			}
			
			//Prendere da map
			PersonDay personDay = personDaysMap.get(date);
			if(personDay == null) {
				personDay = new PersonDay(person, date);
				
//				if (!personDayInList.isHoliday) {
//					date = date.plusDays(1);
//					// populate??
//			
//					continue;
//				}
			}
				
			IWrapperPersonDay wPersonDay = wrapperFactory.create(personDay);
			
			//set previous for progressive
			if(previous != null) {
				wPersonDay.setPreviousForProgressive(Optional.fromNullable(previous));	
			}
			//set previous for night stamp
			if(previous != null) {
				wPersonDay.setPreviousForNightStamp(Optional.fromNullable(previous));
			}
			
			populatePersonDay(wPersonDay);

			previous = personDay;
			date = date.plusDays(1);

		}
		
		// (3) Ricalcolo dei residui per mese
		contractMonthRecapManager.populateContractMonthRecapByPerson(person,
							new YearMonth(from));
	}
	
	/**
	 * (1) Controlla che il personDay sia ben formato 
	 * 		(altrimenti lo inserisce nella tabella PersonDayInTrouble)
	 * (2) Popola i valori aggiornati del person day e li persiste nel db.
	 * 
	 * @param pd 
	 */
	private void populatePersonDay(IWrapperPersonDay pd) {

		//isHoliday = personManager.isHoliday(this.value.person, this.value.date);
		
		//il contratto non esiste più nel giorno perchè è stata inserita data terminazione
		if( !pd.getPersonDayContract().isPresent()) {
			pd.getValue().isHoliday = false;
			pd.getValue().timeAtWork = 0;
			pd.getValue().progressive = 0;
			pd.getValue().difference = 0;
			personDayManager.setIsTickeAvailable(pd,false);
			pd.getValue().stampModificationType = null;
			pd.getValue().save();
			return;
		}

		//Nel caso in cui il personDay non sia successivo a sourceContract imposto i valori a 0
		if(pd.getPersonDayContract().isPresent()
				&& pd.getPersonDayContract().get().sourceDate != null
				&& ! pd.getValue().date.isAfter(pd.getPersonDayContract().get().sourceDate) ) {
			
			pd.getValue().isHoliday = false;			
			pd.getValue().timeAtWork = 0;
			pd.getValue().progressive = 0;
			pd.getValue().difference = 0;
			personDayManager.setIsTickeAvailable(pd,false);
			pd.getValue().stampModificationType = null;
			pd.getValue().save();
			return;
		}
		
		// decido festivo / lavorativo
		pd.getValue().isHoliday = personManager.isHoliday(pd.getValue().person,
				pd.getValue().date);
		pd.getValue().save();

		//controllo problemi strutturali del person day
		if( pd.getValue().date.isBefore(LocalDate.now()) ) {
			pd.getValue().save();
			personDayManager.checkForPersonDayInTrouble(pd);
		}

		//controllo uscita notturna
		handlerNightStamp(pd);

		personDayManager.updateTimeAtWork(pd);

		personDayManager.updateDifference(pd);

		personDayManager.updateProgressive(pd);

		personDayManager.updateTicketAvailable(pd);

		pd.getValue().save();

	}
	
	/**
	 * Se al giorno precedente l'ultima timbratura è una entrata disaccoppiata e nel
	 * giorno attuale vi è una uscita nei limiti notturni in configurazione, allora 
	 * vengono aggiunte le timbrature default a 00:00
	 *
	 * @param pd
	 */
	private void handlerNightStamp(IWrapperPersonDay pd){

		if( pd.isFixedTimeAtWork() ) {
			return;
		}

		if( ! pd.getPreviousForNightStamp().isPresent() ) {
			return;
		}

		PersonDay previous = pd.getPreviousForNightStamp().get();

		Stamping lastStampingPreviousDay = pd.getLastStamping();

		if( lastStampingPreviousDay != null && lastStampingPreviousDay.isIn() ) {

			String hourMaxToCalculateWorkTime = confYearManager
					.getFieldValue(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, 
							pd.getValue().person.office, pd.getValue().date.getYear());

			Integer maxHour = Integer.parseInt(hourMaxToCalculateWorkTime);

			Collections.sort(pd.getValue().stampings);

			if( pd.getValue().stampings.size() > 0 
					&& pd.getValue().stampings.get(0).way == WayType.out 
					&& maxHour > pd.getValue().stampings.get(0).date.getHourOfDay()) {

				StampModificationType smtMidnight = stampTypeManager
						.getStampMofificationType(StampModificationTypeCode
								.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT);

				//timbratura chiusura giorno precedente
				Stamping correctStamp = new Stamping();
				correctStamp.date = new LocalDateTime(previous.date.getYear(), 
						previous.date.getMonthOfYear(), previous.date.getDayOfMonth(), 23, 59);

				correctStamp.way = WayType.out;
				correctStamp.markedByAdmin = false;
				correctStamp.stampModificationType = smtMidnight;
				correctStamp.note = 
						"Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				correctStamp.personDay = previous;
				correctStamp.save();
				previous.stampings.add(correctStamp);
				previous.save();

				populatePersonDay(wrapperFactory.create(previous));

				//timbratura apertura giorno attuale
				Stamping newEntranceStamp = new Stamping();
				newEntranceStamp.date = new LocalDateTime(pd.getValue().date.getYear(),
						pd.getValue().date.getMonthOfYear(), pd.getValue().date.getDayOfMonth(),0,0);

				newEntranceStamp.way = WayType.in;
				newEntranceStamp.markedByAdmin = false;

				newEntranceStamp.stampModificationType = smtMidnight;



				newEntranceStamp.note = 
						"Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				newEntranceStamp.personDay = pd.getValue();
				newEntranceStamp.save();

				pd.getValue().stampings.add( newEntranceStamp );
				pd.getValue().save();
			}
		}
	}	
	
	
	
	

	

}
