package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.StampModificationTypeValue;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

public class PersonDayManager {

	@Inject
	public PersonDayManager(PersonDayDao personDayDao,
			StampingDao stampingDao,
			ContractDao contractDao,
			IWrapperFactory wrapperFactory,
			AbsenceDao absenceDao,
			ConfGeneralManager confGeneralManager,
			PersonDayInTroubleManager personDayInTroubleManager,
			ConfYearManager confYearManager) {

		this.personDayDao = personDayDao;
		this.stampingDao = stampingDao;
		this.contractDao = contractDao;
		this.wrapperFactory = wrapperFactory;
		this.absenceDao = absenceDao;
		this.confGeneralManager = confGeneralManager;
		this.personDayInTroubleManager = personDayInTroubleManager;
		this.confYearManager = confYearManager;
	}

	private final static Logger log = LoggerFactory.getLogger(PersonDayManager.class);

	private final PersonDayDao personDayDao;
	private final StampingDao stampingDao;
	private final ContractDao contractDao;
	private final IWrapperFactory wrapperFactory;
	private final AbsenceDao absenceDao;
	private final ConfGeneralManager confGeneralManager;
	private final PersonDayInTroubleManager personDayInTroubleManager;
	private final ConfYearManager confYearManager;


	/**
	 *
	 * @param abt
	 * @return true se nella lista assenze esiste un'assenza  che appartenga
	 *  a un gruppo il cui codice di rimpiazzamento non sia nullo
	 */
	private boolean checkHourlyAbsenceCodeSameGroup(AbsenceType abt, PersonDay pd) {

		return absenceDao.getAbsenceWithReplacingAbsenceTypeNotNull(abt, pd);

	}
	
	/**
	 * True se il giorno passato come argomento è festivo per la persona. False altrimenti.
	 * @param date
	 * @return
	 */
	public boolean isHoliday(Person person, LocalDate date) {
		
		if(DateUtility.isGeneralHoliday(confGeneralManager.officePatron(person.office), date))
			return true;

		//Contract contract = this.getContract(date);
		Contract contract = contractDao.getContract(date, person);
		if(contract == null)
		{
			//persona fuori contratto
			return false;
		}

		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek()-1).holiday;
			}
		}

		return false;	//se il db è consistente non si verifica mai

	}

	/**
	 * @return true se nel giorno vi e' una assenza giornaliera
	 */
	public boolean isAllDayAbsences(PersonDay pd)
	{
		for(Absence ab : pd.absences)
		{
			if(ab.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay) &&
					!checkHourlyAbsenceCodeSameGroup(ab.absenceType, pd))
				return true;
		}
		return false;
	}

	/**
	 *
	 * @return true se nel giorno c'è un'assenza oraria che giustifica una
	 * quantità oraria sufficiente a decretare la persona "presente" a lavoro
	 */
	public boolean isEnoughHourlyAbsences(PersonDay pd) {

		if(pd.person.qualification.qualification > 3){
			for(Absence abs : pd.absences){
				if(abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHours) ||
						abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHours) ||
						abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHours) ||
						abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHours))
					return true;
			}
			return false;
		}
		else{
			if(pd.absences.size() >= 1)
				return true;
			else
				return false;
		}

	}


	/**
	 * True se la persona ha uno dei WorkingTime abilitati al buono pasto.
	 * 
	 * @return
	 */
	private boolean isTicketAvailableForWorkingTime(IWrapperPersonDay pd){

		Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

		if( pd.getWorkingTimeTypeDay().get().mealTicketEnabled() ) {
			return true;
		}
		return false;
	}

	/**
	 * Calcola i minuti lavorati nel person day. Assegna il campo isTicketAvailable.
	 *
	 * @return il numero di minuti trascorsi a lavoro
	 */
	public int getCalculatedTimeAtWork(IWrapperPersonDay pd) {

		Preconditions.checkState( pd.getWorkingTimeTypeDay().isPresent() );

		int justifiedTimeAtWork = 0;

		//Se hanno il tempo di lavoro fissato non calcolo niente
		if ( pd.isFixedTimeAtWork() ) {

			if( pd.getValue().isHoliday ) 
				return 0;

			return pd.getWorkingTimeTypeDay().get().workingTime;

		}

		//assenze all day piu' altri casi di assenze
		for(Absence abs : pd.getValue().absences) {

			if((abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay
					&& !checkHourlyAbsenceCodeSameGroup(abs.absenceType, pd.getValue() ))) {

				setIsTickeAvailable(pd, false);
				return 0;
			}

			if(!abs.absenceType.code.equals("89") && abs.absenceType.justifiedTimeAtWork.minutesJustified != null) {

				//TODO CASO STRANO qua il buono mensa non si capisce se ci deve essere o no
				justifiedTimeAtWork = justifiedTimeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				continue;
			}

			if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.HalfDay){

				justifiedTimeAtWork = justifiedTimeAtWork + pd.getWorkingTimeTypeDay().get().workingTime / 2;
				continue;
			}
		}

		//se non c'è almeno una coppia di timbrature considero il justifiedTimeAtwork
		//(che però non contribuisce all'attribuzione del buono mensa che quindi è certamente non assegnato)
		if (pd.getValue().stampings.size() < 2)
		{
			setIsTickeAvailable(pd, false);
			return justifiedTimeAtWork;
		}

		Collections.sort(pd.getValue().stampings);
		//TODO se è festa si dovrà capire se il tempo di lavoro deve essere assegnato oppure no
		if(pd.getValue().isHoliday){

			List<PairStamping> validPairs = getValidPairStamping(pd.getValue().stampings);

			int holidayWorkTime=0;
			for(PairStamping validPair : validPairs) {

				holidayWorkTime = holidayWorkTime - DateUtility.toMinute(validPair.in.date);
				holidayWorkTime = holidayWorkTime + DateUtility.toMinute(validPair.out.date);
			}

			setIsTickeAvailable(pd,false);
			return justifiedTimeAtWork + holidayWorkTime;
		}

		List<PairStamping> validPairs = getValidPairStamping(pd.getValue().stampings);

		int workTime=0;
		for(PairStamping validPair : validPairs) {

			workTime = workTime - DateUtility.toMinute(validPair.in.date);
			workTime = workTime + DateUtility.toMinute(validPair.out.date);
		}

		//Il pranzo e' servito??
		WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay().get();

		//se mealTicketTime è zero significa che il dipendente nel giorno non ha diritto al calcolo del buono pasto
		if( ! wttd.mealTicketEnabled() ) {

			setIsTickeAvailable(pd, false);
			return workTime + justifiedTimeAtWork;
		}

		int mealTicketTime = wttd.mealTicketTime;					//6 ore
		int breakTicketTime = wttd.breakTicketTime;					//30 minuti
		int breakTimeDiff = breakTicketTime;
		pd.getValue().stampModificationType = null;
		List<PairStamping> gapLunchPairs = getGapLunchPairs(pd.getValue(), validPairs);

		if(gapLunchPairs.size() > 0 ){
			//	recupero la durata della pausa pranzo fatta
			int minTimeForLunch = gapLunchPairs.get(0).timeInPair;
			//Calcolo l'eventuale differenza tra la pausa fatta e la pausa minima
			breakTimeDiff = (breakTicketTime - minTimeForLunch<=0) ? 0 : (breakTicketTime - minTimeForLunch);
		}

		if(workTime - breakTimeDiff >= mealTicketTime){
			setIsTickeAvailable(pd,true);

			if(!pd.getValue().isTicketForcedByAdmin || 
					pd.getValue().isTicketForcedByAdmin && pd.getValue().isTicketAvailable ) {//TODO decidere la situazione intricata se l'amministratore forza a true
				workTime = workTime - breakTimeDiff;
			}

			// caso in cui non sia stata effettuata una pausa pranzo
			if(breakTimeDiff == breakTicketTime) {

				pd.getValue().stampModificationType = stampingDao
						.getStampModificationTypeByCode(StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
			}

			// Caso in cui la pausa pranzo fatta è inferiore a quella minima
			else if(breakTimeDiff > 0 && breakTimeDiff != breakTicketTime) {

				pd.getValue().stampModificationType = stampingDao
						.getStampModificationTypeByCode(StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
			}
		}

		else {
			setIsTickeAvailable(pd, false);
		}

		return workTime + justifiedTimeAtWork;

	}


	/**
	 * Calcola i minuti lavorati nel person day. Assegna il campo
	 * isTicketAvailable.
	 *
	 * @return il numero di minuti trascorsi a lavoro
	 */
	public int workingMinutes(IWrapperPersonDay pd) {

		Preconditions.checkState(pd.getWorkingTimeTypeDay().isPresent());

		int workTime = 0;

		// Se hanno il tempo di lavoro fissato non calcolo niente
		if (pd.isFixedTimeAtWork()) {
			return pd.getWorkingTimeTypeDay().get().workingTime;
		}

		if (!pd.getValue().isHoliday && pd.getValue().stampings.size() >= 2) {

			Collections.sort(pd.getValue().stampings);

			List<PairStamping> validPairs = getValidPairStamping(pd.getValue().stampings);

			for (PairStamping validPair : validPairs) {

				workTime = workTime - DateUtility.toMinute(validPair.in.date);
				workTime = workTime + DateUtility.toMinute(validPair.out.date);
			}

			// Il pranzo e' servito??
			WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay().get();

			// se mealTicketTime è zero significa che il dipendente nel giorno
			// non ha diritto al calcolo del buono pasto
			if (!wttd.mealTicketEnabled()) {

				setIsTickeAvailable(pd, false);
				return workTime;
			}

			int mealTicketTime = wttd.mealTicketTime; // 6 ore
			int breakTicketTime = wttd.breakTicketTime; // 30 minuti
			int breakTimeDiff = breakTicketTime;
			pd.getValue().stampModificationType = null;
			List<PairStamping> gapLunchPairs = getGapLunchPairs(pd.getValue(),
					validPairs);

			if (gapLunchPairs.size() > 0) {
				// recupero la durata della pausa pranzo fatta
				int minTimeForLunch = gapLunchPairs.get(0).timeInPair;
				// Calcolo l'eventuale differenza tra la pausa fatta e la pausa
				// minima
				breakTimeDiff = (breakTicketTime - minTimeForLunch <= 0) ? 0
						: (breakTicketTime - minTimeForLunch);
			}

			if (workTime - breakTimeDiff >= mealTicketTime) {

				if (!pd.getValue().isTicketForcedByAdmin
						|| pd.getValue().isTicketForcedByAdmin
						&& pd.getValue().isTicketAvailable) {// TODO decidere la
					// situazione
					// intricata se
					// l'amministratore
					// forza a true
					workTime = workTime - breakTimeDiff;
				}

				// caso in cui non sia stata effettuata una pausa pranzo
				if (breakTimeDiff == breakTicketTime) {

					pd.getValue().stampModificationType = stampingDao
							.getStampModificationTypeByCode(StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
				}

				// Caso in cui la pausa pranzo fatta è inferiore a quella minima
				else if (breakTimeDiff > 0 && breakTimeDiff != breakTicketTime) {

					pd.getValue().stampModificationType = stampingDao
							.getStampModificationTypeByCode(StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
				}
			}

			setIsTickeAvailable(pd, false);

		}

		return workTime;
	}

//	/**
//	 *
//	 * @return lo stamp modification type relativo al tempo di lavoro fisso
//	 */
//	public StampModificationType getFixedWorkingTime() {
//
//		//TODO usato solo in PersonStampingDayRecap bisogna metterlo nella cache
//		return stampingDao.getStampModificationTypeById(StampModificationTypeValue.FIXED_WORKINGTIME.getId());
//	}

	/**
	 * Setta il campo valid per ciascuna stamping contenuta in orderedStampings
	 */
	public void computeValidStampings(PersonDay pd) {

		getValidPairStamping(pd.stampings);
	}

	/**
	 * Questo metodo ritorna una lista di coppie di timbrature (uscita/entrata) che rappresentano le potenziali uscite per pranzo.
	 * L'algoritmo filtra le coppie che appartengono alla fascia pranzo in configurazione.
	 * Nel caso in cui una sola timbratura appartenga alla fascia pranzo, l'algoritmo provvede a ricomputare il timeInPair della coppia
	 * assumendo la timbratura al di fuori della fascia uguale al limite di tale fascia. (Le timbrature vengono tuttavia mantenute originali
	 * per garantire l'usabilità anche ai controller che gestiscono reperibilità e turni)
	 * @param validPairs le coppie di timbrature ritenute valide all'interno del giorno
	 * @return
	 */
	private List<PairStamping> getGapLunchPairs(PersonDay pd, List<PairStamping> validPairs)
	{
		//Assumo che la timbratura di uscita e di ingresso debbano appartenere alla finestra 12:00 - 15:00
		Integer mealTimeStartHour = confGeneralManager.getIntegerFieldValue(Parameter.MEAL_TIME_START_HOUR, pd.person.office);
		Integer mealTimeStartMinute = confGeneralManager.getIntegerFieldValue(Parameter.MEAL_TIME_START_MINUTE, pd.person.office);
		Integer mealTimeEndHour = confGeneralManager.getIntegerFieldValue(Parameter.MEAL_TIME_END_HOUR, pd.person.office);
		Integer mealTimeEndMinute = confGeneralManager.getIntegerFieldValue(Parameter.MEAL_TIME_END_MINUTE, pd.person.office);
		LocalDateTime startLunch = new LocalDateTime()
		.withYear(pd.date.getYear())
		.withMonthOfYear(pd.date.getMonthOfYear())
		.withDayOfMonth(pd.date.getDayOfMonth())
		.withHourOfDay(mealTimeStartHour)
		.withMinuteOfHour(mealTimeStartMinute);

		LocalDateTime endLunch = new LocalDateTime()
		.withYear(pd.date.getYear())
		.withMonthOfYear(pd.date.getMonthOfYear())
		.withDayOfMonth(pd.date.getDayOfMonth())
		.withHourOfDay(mealTimeEndHour)
		.withMinuteOfHour(mealTimeEndMinute);

		List<PairStamping> allGapPairs = new ArrayList<PairStamping>();

		//1) Calcolare tutte le gapPair
		Stamping outForLunch = null;
		for(PairStamping validPair : validPairs)
		{
			if(outForLunch==null)
			{
				outForLunch = validPair.out;
			}
			else
			{
				allGapPairs.add( new PairStamping(outForLunch, validPair.in) );
				outForLunch = validPair.out;
			}
		}

		//2) selezionare quelle che appartengono alla fascia pranzo, nel calcolo del tempo limare gli estremi a tale fascia se necessario
		List<PairStamping> gapPairs = new ArrayList<PairStamping>();
		for(PairStamping gapPair : allGapPairs)
		{
			LocalDateTime out = gapPair.out.date;
			LocalDateTime in = gapPair.in.date;
			boolean isInIntoMealTime = in.isAfter(startLunch.minusMinutes(1)) && in.isBefore(endLunch.plusMinutes(1));
			boolean isOutIntoMealTime = out.isAfter(startLunch.minusMinutes(1)) && out.isBefore(endLunch.plusMinutes(1));

			if( isInIntoMealTime || isOutIntoMealTime  )
			{
				LocalDateTime inForCompute = gapPair.in.date;
				LocalDateTime outForCompute = gapPair.out.date;
				if(!isInIntoMealTime)
					inForCompute = startLunch;
				if(!isOutIntoMealTime)
					outForCompute = endLunch;
				int timeInPair = 0;
				timeInPair = timeInPair - DateUtility.toMinute(inForCompute);
				timeInPair = timeInPair + DateUtility.toMinute(outForCompute);
				gapPair.timeInPair = timeInPair;
				gapPairs.add(gapPair);
			}

		}

		return gapPairs;
	}

	/**
	 * Ritorna l'ultima timbratura in ordine di tempo nel giorno
	 * @return
	 */
	private Stamping getLastStamping(PersonDay pd)
	{
		Stamping last = null;
		for(Stamping s : pd.stampings)
		{
			if(last == null)
				last = s;
			else if(last.date.isBefore(s.date))
				last = s;
		}
		return last;
	}

	 /**
	 * Assegna il numero di minuti in cui una persona è stata a lavoro in quella data
	 * 
	 * @param pd
	 */
	private void updateTimeAtWork(IWrapperPersonDay pd) {
		
		pd.getValue().timeAtWork = getCalculatedTimeAtWork(pd);
	}

	/**
	 * Popola il campo difference del PersonDay.
	 * 
	 * @param pd
	 */
	private void updateDifference(IWrapperPersonDay pd) {

		Preconditions.checkState( pd.getWorkingTimeTypeDay().isPresent() );

		int worktime =  pd.getWorkingTimeTypeDay().get().workingTime;

		//persona fixed
		if( pd.isFixedTimeAtWork() && pd.getValue().timeAtWork == 0 ){
			pd.getValue().difference = 0;
			return;
		}

		//festivo
		if( pd.getValue().isHoliday ) {
			if(pd.getValue().acceptedHolidayWorkingTime){
				pd.getValue().difference = pd.getValue().timeAtWork;
			} else {
				pd.getValue().difference = 0;
			}
			return;
		}

		//assenze giornaliere
		if( isAllDayAbsences(pd.getValue()) ){
			pd.getValue().difference = 0;
			return;
		}

		//feriale
		pd.getValue().difference = pd.getValue().timeAtWork - worktime;
	}


	/**
	 * Popola il campo progressive del PersonDay.
	 * 
	 * @param pd
	 */
	private void updateProgressive(IWrapperPersonDay pd) {

		//primo giorno del mese o del contratto
		if( ! pd.getPreviousForProgressive().isPresent() ) {

			pd.getValue().progressive = pd.getValue().difference;
			return;
		}

		//caso generale
		pd.getValue().progressive = pd.getValue().difference + pd.getPreviousForProgressive().get().progressive;

	}

	/**
	 * Popola il campo isTicketAvailable.
	 */
	private void updateTicketAvailable(IWrapperPersonDay pd, boolean persist) {

		//caso forced by admin
		if(pd.getValue().isTicketForcedByAdmin) {
			if(persist) { pd.getValue().save(); }
			return;
		}

		//caso persone fixed
		if(pd.isFixedTimeAtWork())
		{
			if(pd.getValue().isHoliday) {

				pd.getValue().isTicketAvailable = false;
				if(persist) { pd.getValue().save(); }
			}
			else if(!pd.getValue().isHoliday && !isAllDayAbsences(pd.getValue())) {

				pd.getValue().isTicketAvailable = true;
				if(persist) { pd.getValue().save(); }
			}
			else if(!pd.getValue().isHoliday && isAllDayAbsences(pd.getValue()))
			{
				pd.getValue().isTicketAvailable = false;
				if(persist) { pd.getValue().save(); }
			}
			return;
		}

		//caso persone normali
		pd.getValue().isTicketAvailable = 
				pd.getValue().isTicketAvailable && isTicketAvailableForWorkingTime(pd);
		return;
	}


	/**
	 * Setta il valore della variabile isTicketAvailable solo se 
	 * isTicketForcedByAdmin è false.
	 * 
	 * @param value
	 */
	private void setIsTickeAvailable(IWrapperPersonDay pd, boolean isTicketAvailable) {

		if( ! pd.getValue().isTicketForcedByAdmin) {

			pd.getValue().isTicketAvailable = isTicketAvailable;
		}
	}

	/**
	 * (1) Controlla che il personDay sia ben formato 
	 * 		(altrimenti lo inserisce nella tabella PersonDayInTrouble)
	 * (2) Popola i valori aggiornati del person day e li persiste nel db.
	 * 
	 * @param pd 
	 */
	public void populatePersonDay(IWrapperPersonDay pd) {

		//isHoliday = personManager.isHoliday(this.value.person, this.value.date);
		
		//il contratto non esiste più nel giorno perchè è stata inserita data terminazione
		if( !pd.getPersonDayContract().isPresent()) {
			pd.getValue().isHoliday = false;
			pd.getValue().timeAtWork = 0;
			pd.getValue().progressive = 0;
			pd.getValue().difference = 0;
			setIsTickeAvailable(pd,false); //TODO calcolarlo se ci sono timbrature
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
			setIsTickeAvailable(pd,false); //TODO calcolarlo se ci sono timbrature
			pd.getValue().stampModificationType = null;
			pd.getValue().save();
			return;
		}
		
		// decido festivo / lavorativo
		pd.getValue().isHoliday = isHoliday(pd.getValue().person, pd.getValue().date);
		pd.getValue().save();

		//controllo problemi strutturali del person day
		if( pd.getValue().date.isBefore(LocalDate.now()) ) {
			pd.getValue().save();
			checkForPersonDayInTrouble(pd);
		}

		//controllo uscita notturna
		handlerNightStamp(pd);

		updateTimeAtWork(pd);

		updateDifference(pd);

		updateProgressive(pd);

		updateTicketAvailable(pd, false);

		pd.getValue().save();

	}

	/**
	 * Stessa logica di populatePersonDay ma senza persistere i calcoli (usato per il giorno di oggi)
	 */
	public void queSeraSera(IWrapperPersonDay pd) {
		//Strutture dati transienti necessarie al calcolo
		if( ! pd.getPersonDayContract().isPresent() ) {
			return;
		}

		updateTimeAtWork(pd);
		updateDifference(pd);
		updateProgressive(pd);
		updateTicketAvailable(pd, false);
	}

	/**
	 * Aggiorna tutti i personday a partire dalla data specificata fino al giorno corrente
	 * @param person
	 * @param date
	 */
	public void updatePersonDaysFromDate(Person person, LocalDate date){

		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(date);

		person.merge();

		//Verifico se la data è passata, in caso contrario non è necessario ricalcolare nulla
		if(date.isAfter(LocalDate.now())){
			return;
		}

		//FIXME: i personDay potrebbero essere mancanti. 
		// Questo è il caso in cui il job che chiude il giorno sia inattivo per più
		// giorni. La procedura di fix risolve questo problema.
		// In questo caso andrebbe fatto un controllo all'application Start??
		
		//Prendo la lista ordinata di tutti i personday della persona fino ad oggi e effettuo il ricalcolo su tutti
		LocalDate currentMonthEnd = LocalDate.now().dayOfMonth().withMaximumValue();
		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date, 
				Optional.of(currentMonthEnd), true);

		for(PersonDay pd : personDays){
			populatePersonDay(wrapperFactory.create(pd));
		}
	}
	
	/**
	 * Aggiorna i personDay limitatamente ad un mese. ATTENZIONE da usare unitamente
	 * ad un job asincrono che completa la procedura per i rimanenti giorni.
	 * @param person
	 * @param date
	 */
	public void updatePersonDaysInMonth(Person person, LocalDate date) {
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(date);
		LocalDate endMonth = date.dayOfMonth().withMaximumValue();
		
		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date, 
				Optional.fromNullable(endMonth), true);

		for(PersonDay pd : personDays){
			populatePersonDay(wrapperFactory.create(pd));
		}

		
	}

	/**
	 * Verifica che nel person day vi sia una situazione coerente di timbrature. 
	 * Situazioni errate si verificano nei casi:
	 *  (1) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura
	 * 	(2) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature.
	 * In caso di situazione errata viene aggiunto un record nella tabella PersonDayInTrouble.
	 * Se il PersonDay era presente nella tabella PersonDayInTroubled 
	 * ed è stato fixato, viene settato a true il campo fixed.
	 * 
	 * @param pd
	 * @param person
	 */
	public void checkForPersonDayInTrouble(IWrapperPersonDay pd) {

		Preconditions.checkState( pd.getPersonDayContract().isPresent() );

		//se prima o uguale a source contract il problema è fixato
		if( pd.getPersonDayContract().get().sourceDate != null ) {

			if( ! pd.getValue().date.isAfter( pd.getPersonDayContract().get().sourceDate ) ) {

				for(PersonDayInTrouble pdt : pd.getValue().troubles) {
					if(pdt.fixed == false) {
						pdt.fixed = true;
						pdt.save();
						log.info("Fixato {} perchè precedente a sourceContract({})",
								pd.getValue().date, pd.getPersonDayContract().get().sourceDate);
					}
				}
				return;
			}
		}

		//persona fixed
		if( pd.isFixedTimeAtWork() ) {

			if(pd.getValue().stampings.size()!=0) {

				computeValidStampings(pd.getValue());

				for(Stamping s : pd.getValue().stampings) {

					if(!s.valid) {

						personDayInTroubleManager.insertPersonDayInTrouble(
								pd.getValue(), PersonDayInTrouble.UNCOUPLED_FIXED);
						return;
					}
				}
			}
		}

		//persona not fixed
		else {

			//caso no festa, no assenze, no timbrature
			if(!isAllDayAbsences(pd.getValue()) && pd.getValue().stampings.size()==0 
					&& !pd.getValue().isHoliday && !isEnoughHourlyAbsences(pd.getValue())) {

				personDayInTroubleManager.insertPersonDayInTrouble(
						pd.getValue(), PersonDayInTrouble.NO_ABS_NO_STAMP);
				return;
			}

			//caso no festa, no assenze, timbrature disaccoppiate
			if(!isAllDayAbsences(pd.getValue()) && !pd.getValue().isHoliday)
			{
				computeValidStampings(pd.getValue());

				for(Stamping s : pd.getValue().stampings) {

					if(!s.valid) {

						personDayInTroubleManager.insertPersonDayInTrouble(
								pd.getValue(), PersonDayInTrouble.UNCOUPLED_WORKING);
						return;
					}
				}
			}

			//caso festa, no assenze, timbrature disaccoppiate
			else if( !isAllDayAbsences(pd.getValue()) && pd.getValue().isHoliday)
			{
				computeValidStampings(pd.getValue());

				for(Stamping s : pd.getValue().stampings) {

					if(!s.valid) {

						personDayInTroubleManager.insertPersonDayInTrouble(
								pd.getValue(), PersonDayInTrouble.UNCOUPLED_HOLIDAY);
						return;
					}
				}
			}
		}

		//giorno senza problemi, se era in trouble lo fixo
		if( pd.getValue().troubles != null && pd.getValue().troubles.size()>0) {

			//per adesso no storia, unico record
			PersonDayInTrouble pdt = pd.getValue().troubles.get(0);
			pdt.fixed = true;
			pdt.save();
			pd.getValue().save();
		}

	}

	/**
	 *
	 * @return true se il person day è in trouble
	 */
	public boolean isInTrouble(PersonDay pd)
	{
		for(PersonDayInTrouble pdt : pd.troubles)
		{
			if(pdt.fixed==false)
				return true;
		}
		return false;
	}


	/**
	 * la lista delle timbrature del person day modificata con
	 * (1) l'inserimento di una timbratura null nel caso in cui esistano due timbrature consecutive di ingresso o di uscita,
	 * mettendo tale timbratura nulla in mezzo alle due
	 * (2) l'inserimento di una timbratura di uscita fittizia nel caso di today per calcolare il tempo di lavoro provvisorio
	 * (3) l'inserimento di timbrature null per arrivare alla dimensione del numberOfInOut
	 * @param stampings
	 * @return
	 */
	public List<Stamping> getStampingsForTemplate(PersonDay pd, int numberOfInOut, boolean today) {

		if(today)
		{
			//aggiungo l'uscita fittizia 'now' nel caso risulti dentro il cnr non di servizio
			boolean lastStampingIsIn = false;

			Collections.sort(pd.stampings);

			for(Stamping stamping : pd.stampings)
			{
				if(stamping.stampType!= null && stamping.stampType.identifier.equals("s"))
					continue;
				if(stamping.isIn())
				{
					lastStampingIsIn = true;
					continue;
				}
				if(stamping.isOut())
				{
					lastStampingIsIn = false;
					continue;
				}
			}
			if(lastStampingIsIn)
			{
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = new LocalDateTime();
				stamping.markedByAdmin = false;
				stamping.exitingNow = true;
				pd.stampings.add(stamping);
			}
		}
		List<Stamping> stampingsForTemplate = new ArrayList<Stamping>();
		boolean isLastIn = false;

		for (Stamping s : pd.stampings) {
			//sono dentro e trovo una uscita
			if (isLastIn && s.way == WayType.out)
			{
				//salvo l'uscita
				stampingsForTemplate.add(s);
				isLastIn=false;
				continue;
			}
			//sono dentro e trovo una entrata
			if (isLastIn && s.way == WayType.in)
			{
				//creo l'uscita fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				//salvo l'entrata
				stampingsForTemplate.add(s);
				isLastIn=true;
				continue;
			}

			//sono fuori e trovo una entrata
			if (!isLastIn && s.way == WayType.in)
			{
				//salvo l'entrata
				stampingsForTemplate.add(s);
				isLastIn = true;
				continue;
			}

			//sono fuori e trovo una uscita
			if (!isLastIn && s.way == WayType.out)
			{
				//creo l'entrata fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.in;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				//salvo l'uscita
				stampingsForTemplate.add(s);
				isLastIn = false;
				continue;
			}
		}
		while(stampingsForTemplate.size()<numberOfInOut*2)
		{
			if(isLastIn)
			{
				//creo l'uscita fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				isLastIn = false;
				continue;
			}
			if(!isLastIn)
			{
				//creo l'entrata fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.in;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				isLastIn = true;
				continue;
			}
		}

		return stampingsForTemplate;
	}

	/**
	 * Ritorna le coppie di stampings valide al fine del calcolo del time at work. All'interno del metodo
	 * viene anche settato il campo valid di ciascuna stampings contenuta nel person day
	 * @return
	 */
	public List<PairStamping> getValidPairStamping(List<Stamping> stampings)	{

		Collections.sort(stampings);
		//(1)Costruisco le coppie valide per calcolare il worktime
		List<PairStamping> validPairs = new ArrayList<PairStamping>();
		List<Stamping> serviceStampings = new ArrayList<Stamping>();
		Stamping stampEnter = null;
		for(Stamping stamping : stampings)
		{
			//le stampings di servizio non entrano a far parte del calcolo del work time ma le controllo successivamente
			//per segnalare eventuali errori di accoppiamento e appartenenza a orario di lavoro valido
			if(stamping.stampType!= null && stamping.stampType.identifier.equals("s"))
			{
				serviceStampings.add(stamping);
				continue;
			}
			//cerca l'entrata
			if(stampEnter==null)
			{
				if(stamping.isIn())
				{
					stampEnter = stamping;
					continue;
				}
				if(stamping.isOut())
				{
					//una uscita prima di una entrata e' come se non esistesse
					stamping.valid = false;
					continue;
				}

			}
			//cerca l'uscita
			if(stampEnter!=null)
			{
				if(stamping.isOut())
				{
					validPairs.add(new PairStamping(stampEnter, stamping));
					stampEnter.valid = true;
					stamping.valid = true;
					stampEnter = null;
					continue;
				}
				//trovo un secondo ingresso, butto via il primo
				if(stamping.isIn())
				{
					stampEnter.valid = false;
					stampEnter = stamping;
					continue;
				}
			}
		}
		//(2) scarto le stamping di servizio che non appartengono ad alcuna coppia valida
		List<Stamping> serviceStampingsInValidPair = new ArrayList<Stamping>();
		for(Stamping stamping : serviceStampings)
		{
			boolean belongToValidPair = false;
			for(PairStamping validPair : validPairs)
			{
				LocalDateTime outTime = validPair.out.date;
				LocalDateTime inTime = validPair.in.date;
				if(stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime))
				{
					belongToValidPair = true;
					break;
				}
			}
			if(belongToValidPair)
			{
				serviceStampingsInValidPair.add(stamping);
			}
			else
			{
				stamping.valid = false;
			}
		}

		//(3)aggrego le stamping di servizio per coppie valide ed eseguo il check di sequenza valida
		for(PairStamping validPair : validPairs)
		{
			LocalDateTime outTime = validPair.out.date;
			LocalDateTime inTime = validPair.in.date;
			List<Stamping> serviceStampingsInSinglePair = new ArrayList<Stamping>();
			for(Stamping stamping : serviceStampingsInValidPair)
			{
				if(stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime))
				{
					serviceStampingsInSinglePair.add(stamping);
				}
			}
			//check
			Stamping serviceExit = null;
			for(Stamping stamping : serviceStampingsInSinglePair)
			{
				//cerca l'uscita di servizio
				if(serviceExit==null)
				{
					if(stamping.isOut())
					{
						serviceExit = stamping;
						continue;
					}
					if(stamping.isIn())
					{
						//una entrata di servizio prima di una uscita di servizio e' come se non esistesse
						stamping.valid = false;
						continue;
					}
				}
				//cerca l'entrata di servizio
				if(serviceExit!=null)
				{
					if(stamping.isIn())
					{
						stamping.valid = true;
						serviceExit.valid = true;
						serviceExit = null;
						continue;
					}
					//trovo una seconda uscita di servizio, butto via la prima
					if(stamping.isOut())
					{
						serviceExit.valid = false;
						serviceExit = stamping;
						continue;
					}
				}
			}
		}

		return validPairs;
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

		Stamping lastStampingPreviousDay = getLastStamping( previous );

		if( lastStampingPreviousDay != null && lastStampingPreviousDay.isIn() ) {

			String hourMaxToCalculateWorkTime = confYearManager
					.getFieldValue(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, 
							pd.getValue().person.office, pd.getValue().date.getYear());

			Integer maxHour = Integer.parseInt(hourMaxToCalculateWorkTime);

			Collections.sort(pd.getValue().stampings);

			if( pd.getValue().stampings.size() > 0 
					&& pd.getValue().stampings.get(0).way == WayType.out 
					&& maxHour > pd.getValue().stampings.get(0).date.getHourOfDay()) {

				StampModificationType smtMidnight = stampingDao
						.getStampModificationTypeById(StampModificationTypeValue
								.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getId());

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
	 * Lo stampModificationType relativo alla timbratura aggiunta dal
	 * sistema nel caso di timbrature aggiunte automaticamente a cavallo della
	 * mezzanotte.
	 * 
	 * @return 
	 */
	public Optional<StampModificationType> checkMissingExitStampBeforeMidnight(Stamping st) {

		if(st.stampModificationType != null &&
				st.stampModificationType.id
				.equals(StampModificationTypeValue
						.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getId())) {

			return Optional.fromNullable(st.stampModificationType);
		}

		return Optional.absent();
	}


	/**
	 * Utilizzata nel metodo delete del controller Persons per cancellare tutti i personDays relativi alla persona person
	 * @param person
	 */
	public void deletePersonDays(Person person){

		List<PersonDay> helpPdList = personDayDao.getAllPersonDay(person);
		for(PersonDay pd : helpPdList){

			pd.delete();
			person.personDays.remove(pd);
			person.save();
		}
	}

	/**
	 * Calcola il numero massimo di coppie di colonne ingresso/uscita da stampare nell'intero mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public int getMaximumCoupleOfStampings(Person person, int year, int month){

		LocalDate begin = new LocalDate(year, month, 1);
		if(begin.isAfter(new LocalDate()))
			return 0;
		List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(person, begin, Optional.fromNullable(begin.dayOfMonth().withMaximumValue()), false);
		//List<PersonDay> pdList = PersonDay.find("Select pd From PersonDay pd where pd.person = ? and pd.date between ? and ?", person,begin,begin.dayOfMonth().withMaximumValue() ).fetch();

		int max = 0;
		for(PersonDay pd : pdList)
		{
			int coupleOfStampings = numberOfInOutInPersonDay(pd);

			if(max<coupleOfStampings)
				max = coupleOfStampings;
		}

		return max;
	}

	/**
	 * Genera una lista di PersonDay aggiungendo elementi fittizzi per coprire ogni giorno del mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public List<PersonDay> getTotalPersonDayInMonth(Person person, int year, int month) {
		
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> totalDays = new ArrayList<PersonDay>();
		List<PersonDay> workingDays = personDayDao.getPersonDayInPeriod(person, beginMonth, Optional.fromNullable(endMonth), true);

		int currentWorkingDays = 0;
		LocalDate currentDate = beginMonth;
		while(!currentDate.isAfter(endMonth))
		{
			if(currentWorkingDays<workingDays.size() && workingDays.get(currentWorkingDays).date.isEqual(currentDate))
			{
				totalDays.add(workingDays.get(currentWorkingDays));
				currentWorkingDays++;
			}
			else
			{
				PersonDay previusPersonDay = null;
				if(totalDays.size()>0)
					previusPersonDay = totalDays.get(totalDays.size()-1);

				PersonDay newPersonDay;
				//primo giorno del mese festivo
				if(previusPersonDay==null)
					newPersonDay = new PersonDay(person, new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0, 0);
				//altri giorni festivi
				else
				{
					newPersonDay = new PersonDay(person, new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0, previusPersonDay.progressive);
				}

				totalDays.add(newPersonDay);

			}
			currentDate = currentDate.plusDays(1);
		}
		return totalDays;
	}

	/**
	 * Il numero di buoni pasto usabili all'interno della lista di person day passata come parametro
	 * @return
	 */
	public int numberOfMealTicketToUse(Person person, int year, int month){

		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> workingDays = personDayDao.getPersonDayForTicket(person, beginMonth, endMonth, true);

		int number = 0;
		for(PersonDay pd : workingDays)
		{
			if(!pd.isHoliday )
				number++;
		}
		return number;
	}



	/**
	 * Il numero di buoni pasto da restituire all'interno della lista di person day passata come parametro
	 * @return
	 */
	public int numberOfMealTicketToRender(Person person, int year, int month){
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> pdListNoTicket = personDayDao.getPersonDayForTicket(person, beginMonth, endMonth, false);

		int ticketTorender = pdListNoTicket.size();

		for(PersonDay pd : pdListNoTicket) {

			//tolgo da ticket da restituire i giorni festivi e oggi e i giorni futuri
			if(pd.isHoliday || pd.isToday() )
			{
				ticketTorender--;
				continue;
			}

			//tolgo da ticket da restituire i giorni futuri in cui non ho assenze
			if(pd.date.isAfter(LocalDate.now()) && pd.absences.isEmpty())
				ticketTorender--;
		}

		return ticketTorender;
	}

	public boolean isOnMission(PersonDay personDay){
		return !FluentIterable.from(personDay.absences).filter(
				new Predicate<Absence>() {
					@Override
					public boolean apply(Absence absence) {
						return absence.absenceType.code.equals(AbsenceTypeMapping.MISSIONE.getCode());
					}}).isEmpty();
	}

	/**
	 * Il numero di coppie ingresso/uscita da stampare per il personday
	 * @param pd
	 * @return
	 */
	public int numberOfInOutInPersonDay(PersonDay pd)
	{
		if(pd == null)
			return 0;
		Collections.sort(pd.stampings);

		int coupleOfStampings = 0;

		String lastWay = null;
		for(Stamping s : pd.stampings)
		{
			if(lastWay==null)
			{
				//trovo out chiudo una coppia
				if(s.way.description.equals("out"))
				{
					coupleOfStampings++;
					lastWay = null;
					continue;
				}
				//trovo in lastWay diventa in
				if(s.way.description.equals("in"))
				{
					lastWay = s.way.description;
					continue;
				}

			}
			//lastWay in
			if(lastWay.equals("in"))
			{
				//trovo out chiudo una coppia
				if(s.way.description.equals("out"))
				{
					coupleOfStampings++;
					lastWay = null;
					continue;
				}
				//trovo in chiudo una coppia e lastWay resta in
				if(s.way.description.equals("in"))
				{
					coupleOfStampings++;
					continue;
				}
			}
		}
		//l'ultima stampings e' in chiudo una coppia
		if(lastWay!=null)
			coupleOfStampings++;

		return coupleOfStampings;
	}


}
