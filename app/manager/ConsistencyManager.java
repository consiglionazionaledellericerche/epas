package manager;

import it.cnr.iit.epas.DateInterval;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.cache.StampTypeManager;
import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.Parameter;

import org.apache.commons.mail.EmailException;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPA;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperPersonDay;

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
			StampTypeManager stampTypeManager,
			AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao, 
			VacationsRecapFactory vacationsFactory,
			ConfGeneralManager confGeneralManager) {

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
		this.absenceDao = absenceDao;
		this.absenceTypeDao = absenceTypeDao;
		this.vacationsFactory = vacationsFactory;
		this.confGeneralManager = confGeneralManager;
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
	private final AbsenceDao absenceDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final VacationsRecapFactory vacationsFactory;
	private final ConfGeneralManager confGeneralManager;

	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone attive alla data di ieri
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 * @param userLogged
	 * @throws EmailException 
	 */
	public void fixPersonSituation(Optional<Person> person ,Optional<User> user,
			LocalDate fromDate, boolean sendMail, boolean onlyRecap){

		Set<Office> offices = user.isPresent() ? 
				officeDao.getOfficeAllowed(user.get()) 
				: Sets.newHashSet(officeDao.getAllOffices());

				//  (0) Costruisco la lista di persone su cui voglio operare
				List<Person> personList = Lists.newArrayList();

				if(person.isPresent() && user.isPresent()){
					//if(personManager.isAllowedBy(user.get(), person.get()))
					personList.add(person.get());
				} else {
					personList = personDao.list(Optional.<String>absent(), offices,
							false, fromDate, LocalDate.now().minusDays(1), true).list();
				}

				for(Person p : personList) {
					
					if (onlyRecap) {
						updatePersonRecaps(p.id, fromDate);
					} else {
						updatePersonSituation(p.id, fromDate);
					}
					//attenzione quando si forzano le transazioni o si invalida la cache dell'entityManager,
					//possono esserci effetti collaterali....vedi il blocco try sotto
					JPA.em().flush();
					JPA.em().clear();
				}

				if(sendMail && LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY 
						&& LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY){

					LocalDate begin = new LocalDate().minusMonths(1);
					LocalDate end = new LocalDate().minusDays(1);
					
					personList = personDao.list(Optional.<String>absent(), offices,
							false, fromDate, LocalDate.now().minusDays(1), true).list();
					
					try {
						//A questo punto del codice le Persone della personList sono detached a 
						//causa della chiusura delle transazioni e mi tocca rifare la query prima di passarla, altrimenti schianta
						personDayInTroubleManager.sendMail(personList, begin, end, "timbratura");
					}
					catch(EmailException e){
						e.printStackTrace();
					}
				}
	}

	/**
	 * Controlla la presenza di errori nelle timbrature. 
	 * Gestisce i giorni problematici nella tabella PersonDayInTrouble.
	 * Effettua i ricalcoli giornialieri e mensili.
	 * @param person
	 * @param from
	 */
	public void updatePersonSituation(Long personId, LocalDate from) {
		updatePersonSituationEngine(personId, from, false);
	}
	
	public void updatePersonRecaps(Long personId, LocalDate from) {
		updatePersonSituationEngine(personId, from, true);
	}

	
	private void updatePersonSituationEngine(Long personId, LocalDate from, 
			boolean updateOnlyRecaps) {
		
		final Person person = personDao.fetchPersonForComputation(personId, 
				Optional.fromNullable(from), 
				Optional.<LocalDate>absent());
		
		log.info("Lanciato aggiornamento situazione {} da {} a oggi", person.getFullname(), from);

		if (person.qualification == null) {
			log.info("... annullato ricalcolo per {} in quanto priva di qualifica", person.getFullname());
			return;
		}
		
		IWrapperPerson wPerson = wrapperFactory.create(person);

		//Gli intervalli di ricalcolo dei person day.
		LocalDate lastPersonDayToCompute = LocalDate.now();
		
		LocalDate date = personFirstDateForEpasComputation(person,
				Optional.fromNullable(from));
				
		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date, 
				Optional.fromNullable(lastPersonDayToCompute));
		
		//Costruire la tabella hash
		HashMap<LocalDate, PersonDay> personDaysMap = Maps.newHashMap();
		for(PersonDay personDay : personDays) {
			personDaysMap.put(personDay.date, personDay);
		}

		log.info("... fetch dei dati conclusa, inizio dei ricalcoli.");

		PersonDay previous = null;

		if (!updateOnlyRecaps) {

			while ( !date.isAfter(lastPersonDayToCompute) ) {

				if(! wPerson.isActiveInDay(date) ) {
					date = date.plusDays(1);
					previous = null;
					continue;
				}

				//Prendere da map
				PersonDay personDay = personDaysMap.get(date);
				if(personDay == null) {
					personDay = new PersonDay(person, date);
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

			log.info("... ricalcolo dei giorni lavorativi conclusa.");
		}
		// (3) Ricalcolo dei residui per mese
		populateContractMonthRecapByPerson(person, new YearMonth(from));

		log.info("... ricalcolo dei riepiloghi conclusa.");
	}
	
	/**
	 * Il primo giorno di ricalcolo ePAS per la persona. <br>
	 * La data più recente fra: 
	 * 1) from <br>
	 * 2) inizio utilizzo software per l'office della persona <br> 
	 * 3) creazione della persona. 
	 *
	 * @param person
	 * @param from
	 * @return
	 */
	private LocalDate personFirstDateForEpasComputation(
			Person person, Optional<LocalDate> from) {
		
		Optional<LocalDate> officeLimit = confGeneralManager
				.getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, person.office);
		
		Preconditions.checkState(officeLimit.isPresent());
		
		//Calcolo a partire da
		LocalDate lowerBoundDate = new LocalDate(person.createdAt);

		if (officeLimit.get().isAfter(lowerBoundDate)) {
			lowerBoundDate = officeLimit.get();
		}
		
		if (from.isPresent() && from.get().isAfter(lowerBoundDate)) {
			lowerBoundDate = from.get();
		}
		
		return lowerBoundDate;
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
				&& pd.getPersonDayContract().get().sourceDateResidual != null
				&& ! pd.getValue().date.isAfter(pd.getPersonDayContract().get().sourceDateResidual) ) {

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

		//controllo uscita notturna
		handlerNightStamp(pd);

		personDayManager.updateTimeAtWork(pd);

		personDayManager.updateDifference(pd);

		personDayManager.updateProgressive(pd);

		personDayManager.updateTicketAvailable(pd);
		
		//controllo problemi strutturali del person day
		if( pd.getValue().date.isBefore(LocalDate.now()) ) {
			personDayManager.checkForPersonDayInTrouble(pd);
		}

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

	/**
	 * Costruisce i riepiloghi mensili dei contratti della persona 
	 * a partire da yeraMonthFrom.
	 * 
	 * @param person
	 * @param yearMonthFrom
	 */
	private void populateContractMonthRecapByPerson( Person person, 
			YearMonth yearMonthFrom) {

		for( Contract contract : person.contracts ){

			IWrapperContract wcontract = wrapperFactory.create(contract);
			DateInterval contractDateInterval = wcontract.getContractDateInterval();
			YearMonth endContractYearMonth = new YearMonth(contractDateInterval.getEnd());

			//Se yearMonthFrom non è successivo alla fine del contratto...
			if ( !yearMonthFrom.isAfter(endContractYearMonth) ) {

				if( contract.vacationPeriods.isEmpty() ) {
					log.info("No vacation period {}", contract.toString());
					continue;
				}

				populateContractMonthRecap(wcontract, Optional.fromNullable(yearMonthFrom));
			} 
		}
	}
	
	/**
	 * Costruzione di un ContractMonthRecap pulito. <br>
	 * Se esiste già un oggetto per il contract e yearMonth specificati viene azzerato.
	 * 
	 * @param contract
	 * @param yearMonth
	 * @return
	 */
	private ContractMonthRecap buildContractMonthRecap(IWrapperContract contract, 
			YearMonth yearMonth ) {

		Optional<ContractMonthRecap> cmrOld = contract.getContractMonthRecap(yearMonth);

		if ( cmrOld.isPresent() ) {
			cmrOld.get().clean();
			return cmrOld.get();
		}

		ContractMonthRecap cmr = new ContractMonthRecap();
		cmr.year = yearMonth.getYear();
		cmr.month = yearMonth.getMonthOfYear();
		cmr.contract = contract.getValue();

		return cmr;
	}

	/**
	 * Costruttore dei riepiloghi mensili per contract. <br>
	 * Se il contratto non è inizializzato correttamente non effettua alcun ricalcolo. <br>
	 * Se yearMonthFrom è absent() calcola tutti i riepiloghi dall'inizio del contratto. <br>
	 * Se yearMonthFrom è present() calcola tutti i riepiloghi da yearMonthFrom. <br>
	 * Se non esiste il riepilogo precedente a yearMonthFrom chiama la stessa procedura
	 * dall'inizio del contratto. (Capire se questo caso si verifica mai).   
	 * 
	 * @param contract
	 * @param yearMonthFrom
	 */
	private void populateContractMonthRecap(IWrapperContract contract, 
			Optional<YearMonth> yearMonthFrom) {
 
		//Conterrà il riepilogo precedente di quello da costruire all'iterazione n.
		Optional<ContractMonthRecap> previousMonthRecap = 
				Optional.<ContractMonthRecap>absent(); 
		
		Optional<YearMonth> firstContractMonthRecap = contract.getFirstMonthToRecap();
		if (!firstContractMonthRecap.isPresent()) {
			//Non ho inizializzazione.
			return;
		}

		//Analisi della richiesta. Inferisco il primo mese da riepilogare.
		
		//Di default costruisco il primo mese da riepilogare del contratto.
		YearMonth yearMonthToCompute = firstContractMonthRecap.get();
		
		//Se ho specificato un mese in particolare 
		//che necessita di un riepilogo precedente verifico che esso esista!
		if(yearMonthFrom.isPresent() 
				&& yearMonthFrom.get().isAfter(firstContractMonthRecap.get())) {

			yearMonthToCompute = yearMonthFrom.get();
			previousMonthRecap = contract
					.getContractMonthRecap(yearMonthFrom.get().minusMonths(1));
			if( !previousMonthRecap.isPresent() ) {
				//Ho chiesto un mese specifico ma non ho il riepilogo precedente
				//per costruirlo. Soluzione: costruisco tutti i riepiloghi del contratto.
				populateContractMonthRecap(contract, Optional.<YearMonth>absent());
			}
		}

		//Il calcolo del riepilogo del mese che ricade nel sourceDateResidual
		//è particolare e va gestito con un metodo dedicato.
		else if( contract.getValue().sourceDateResidual != null 
				&& yearMonthToCompute.isEqual(
						new YearMonth(contract.getValue().sourceDateResidual)) ) {
			
			previousMonthRecap = Optional.fromNullable(
					populateContractMonthFromSource(contract, yearMonthToCompute));
			yearMonthToCompute = yearMonthToCompute.plusMonths(1);
		}
		
		//Ciclo sui mesi successivi fino all'ultimo mese da costruire
		YearMonth lastMonthToCompute = contract.getLastMonthToRecap();

		//Contiene il riepilogo da costruire.	
		ContractMonthRecap currentMonthRecap;
		
		while ( !yearMonthToCompute.isAfter(lastMonthToCompute) ) {

			currentMonthRecap = buildContractMonthRecap(contract, yearMonthToCompute);

			// (1) FERIE E PERMESSI 

			// TODO: per il calcolo delle ferie e permessi ho bisogno solo del
			// riepilogo di dicembre. Una ottimizzazione è calcolare questi campi
			// solo nel caso di dicembre. Però i dati dei mesi intermedi potrebbero 
			// essere usati per report. Decidere. 

			LocalDate lastDayInYearMonth = new LocalDate(yearMonthToCompute.getYear(), 
					yearMonthToCompute.getMonthOfYear(), 1).dayOfMonth().withMaximumValue();

			Optional<VacationsRecap> vacationRecap = vacationsFactory
					.create(yearMonthToCompute.getYear(), contract.getValue(), lastDayInYearMonth, true);

			if( !vacationRecap.isPresent() ) {

				//Siccome non ci sono i riepiloghi quando vado a fare l'update della
				// timbratura schianta. Soluzioni? Se yeraMonthFrom.present() fare una
				// missingRecap()??
				if( yearMonthFrom.isPresent() ) {
					//provvisorio.
					populateContractMonthRecap(contract, Optional.<YearMonth>absent());
				}
				return;
			}

			currentMonthRecap.vacationLastYearUsed = vacationRecap.get().vacationDaysLastYearUsed;
			currentMonthRecap.vacationCurrentYearUsed = vacationRecap.get().vacationDaysCurrentYearUsed;
			currentMonthRecap.permissionUsed = vacationRecap.get().permissionUsed;

			// (2) RESIDUI
			List<Absence> otherCompensatoryRest = Lists.newArrayList();
			Optional<ContractMonthRecap> recap = contractMonthRecapManager
					.computeResidualModule(currentMonthRecap, previousMonthRecap, 
							yearMonthToCompute, lastDayInYearMonth, otherCompensatoryRest);

			recap.get().save();
			contract.getValue().contractMonthRecaps.add(recap.get());
			contract.getValue().save();

			previousMonthRecap = Optional.fromNullable(currentMonthRecap);
			yearMonthToCompute = yearMonthToCompute.plusMonths(1);
		}
	}

	/**
	 * Costruzione del riepilogo mensile nel caso particolare di inzializzazione 
	 * del contratto nel mese<br>
	 * 1) Se l'inizializzazione è l'ultimo giorno del mese costruisce il riepilogo 
	 *    copiando le informazioni in esso contenute. <br>
	 * 2) Se l'inizializzazione non è l'ultimo giorno del mese combina le informazioni 
	 *    presenti in inizializzazione e quelle in database (a partire dal giorno 
	 *    successivo alla inizializzazione).<br>  
	 * @param contract
	 * @param yearMonthToCompute
	 * @return
	 */
	private ContractMonthRecap populateContractMonthFromSource(IWrapperContract contract, 
			YearMonth yearMonthToCompute) {

		//Caso semplice ultimo giorno del mese
		LocalDate lastDayInSourceMonth = contract.getValue()
				.sourceDateResidual.dayOfMonth().withMaximumValue();
		
		if(lastDayInSourceMonth.isEqual(contract.getValue().sourceDateResidual)) {
			ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

			cmr.remainingMinutesCurrentYear = contract.getValue().sourceRemainingMinutesCurrentYear;
			cmr.remainingMinutesLastYear = contract.getValue().sourceRemainingMinutesLastYear;
			cmr.vacationLastYearUsed = contract.getValue().sourceVacationLastYearUsed;
			cmr.vacationCurrentYearUsed = contract.getValue().sourceVacationCurrentYearUsed;
			cmr.recoveryDayUsed = contract.getValue().sourceRecoveryDayUsed;
			cmr.permissionUsed = contract.getValue().sourcePermissionUsed;
			
			if(contract.getValue().sourceDateMealTicket != null && 
					contract.getValue().sourceDateResidual.isEqual(contract.getValue().sourceDateMealTicket)) {
				cmr.buoniPastoDaInizializzazione = contract.getValue().sourceRemainingMealTicket;
				cmr.remainingMealTickets = contract.getValue().sourceRemainingMealTicket;
			} else {
				//Non hanno significato, il riepilogo dei residui dei buoni pasto
				//inizia successivamente.
				cmr.buoniPastoDaInizializzazione = 0;
				cmr.remainingMealTickets = 0;
			}
			cmr.save();
			contract.getValue().contractMonthRecaps.add(cmr);
			contract.getValue().save();
			return cmr;
		}

		//Caso complesso, combinare inizializzazione e database.
		
		ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

		AbsenceType ab31 = absenceTypeDao.getAbsenceTypeByCode(
				AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).orNull();
		AbsenceType ab32 = absenceTypeDao.getAbsenceTypeByCode(
				AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()).orNull();
		AbsenceType ab37 = absenceTypeDao.getAbsenceTypeByCode(
				AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()).orNull(); 
		AbsenceType ab94 = absenceTypeDao.getAbsenceTypeByCode(
				AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).orNull(); 

		DateInterval monthInterSource = new DateInterval(
				contract.getValue().sourceDateResidual.plusDays(1), lastDayInSourceMonth);
		List<Absence> abs32 = absenceDao.getAbsenceDays(monthInterSource, contract.getValue(), ab32);
		List<Absence> abs31 = absenceDao.getAbsenceDays(monthInterSource, contract.getValue(), ab31);
		List<Absence> abs37 = absenceDao.getAbsenceDays(monthInterSource, contract.getValue(), ab37);
		List<Absence> abs94 = absenceDao.getAbsenceDays(monthInterSource, contract.getValue(), ab94);

		cmr.vacationLastYearUsed = contract.getValue().sourceVacationLastYearUsed + abs31.size() + abs37.size();
		cmr.vacationCurrentYearUsed = contract.getValue().sourceVacationCurrentYearUsed + abs32.size();
		cmr.permissionUsed = contract.getValue().sourcePermissionUsed + abs94.size();

		contract.getValue().contractMonthRecaps.add(cmr);
		cmr.save();

		// Informazioni relative ai residui		
		List<Absence> otherCompensatoryRest = Lists.newArrayList();
		contractMonthRecapManager.computeResidualModule(cmr, Optional.<ContractMonthRecap>absent(), 
				yearMonthToCompute, new LocalDate().minusDays(1), otherCompensatoryRest);

		cmr.save();

		contract.getValue().save();
		return cmr;

	}
}
