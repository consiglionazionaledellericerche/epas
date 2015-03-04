package manager;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.StampModificationType;
import models.StampModificationTypeCode;
import models.StampModificationTypeValue;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.WorkingTimeTypeDao;

public class PersonDayManager {

	private final static Logger log = LoggerFactory.getLogger(PersonDayManager.class);

	@Inject
	public PersonDayDao personDayDao;
	
	@Inject
	public StampingDao stampingDao;
	
	/**
	 * 
	 * @param abt
	 * @return true se nella lista assenze esiste un'assenza  che appartenga
	 *  a un gruppo il cui codice di rimpiazzamento non sia nullo
	 */
	private static boolean checkHourlyAbsenceCodeSameGroup(AbsenceType abt, PersonDay pd) {
		
		return AbsenceDao.getAbsenceWithReplacingAbsenceTypeNotNull(abt, pd);

	}
	
	/**
	 * @return true se nel giorno vi e' una assenza giornaliera
	 */
	public static boolean isAllDayAbsences(PersonDay pd)
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
	 * True se la persona ha uno dei WorkingTime abilitati al buono pasto
	 * @return 
	 */
	private boolean isTicketAvailableForWorkingTime(PersonDay pd){
		
		if( pd.getWorkingTimeTypeDay().mealTicketEnabled() )
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Algoritmo definitivo per il calcolo dei minuti lavorati nel person day.
	 * Ritorna i minuti di lavoro per la persona nel person day 
	 * ed in base ad essi assegna il campo isTicketAvailable.
	 * 
	 * @return il numero di minuti trascorsi a lavoro
	 */
	public int getCalculatedTimeAtWork(PersonDay pd) {
		int justifiedTimeAtWork = 0;
		
		//Se hanno il tempo di lavoro fissato non calcolo niente
		if (pd.isFixedTimeAtWork()) 
		{
			if(pd.isHoliday())
				return 0;
			return pd.getWorkingTimeTypeDay().workingTime;
		} 

		//assenze all day piu' altri casi di assenze
		for(Absence abs : pd.absences){
			if((abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay 
					&& !checkHourlyAbsenceCodeSameGroup(abs.absenceType, pd)))
			{
				setIsTickeAvailable(pd,false);
				return 0;
			}
			
			if(!abs.absenceType.code.equals("89") && abs.absenceType.justifiedTimeAtWork.minutesJustified != null)
			{
				//TODO CASO STRANO qua il buono mensa non si capisce se ci deve essere o no
				justifiedTimeAtWork = justifiedTimeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				continue;
			}
			
			if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.HalfDay){
				justifiedTimeAtWork = justifiedTimeAtWork + ContractDao.getCurrentWorkingTimeType(pd.person).workingTimeTypeDays.get(pd.date.getDayOfWeek()).workingTime / 2;
				continue;
			}
		}

		//se non c'è almeno una coppia di timbrature considero il justifiedTimeAtwork 
		//(che però non contribuisce all'attribuzione del buono mensa che quindi è certamente non assegnato)
		if (pd.stampings.size() < 2) 
		{
			setIsTickeAvailable(pd,false);
			return justifiedTimeAtWork;
		}
		
		//TODO se è festa si dovrà capire se il tempo di lavoro deve essere assegnato oppure no 
		if(pd.isHoliday()){
			orderStampings(pd);
			
			List<PairStamping> validPairs = getValidPairStamping(pd.stampings);
			
			int holidayWorkTime=0;
			{
				for(PairStamping validPair : validPairs)
				{
					holidayWorkTime = holidayWorkTime - DateUtility.toMinute(validPair.in.date);
					holidayWorkTime = holidayWorkTime + DateUtility.toMinute(validPair.out.date);
				}
			}
			setIsTickeAvailable(pd,false);
			return justifiedTimeAtWork + holidayWorkTime;
		}
			
		orderStampings(pd);
		List<PairStamping> validPairs = getValidPairStamping(pd.stampings);
	
		int workTime=0;
		{
			for(PairStamping validPair : validPairs)
			{
				workTime = workTime - DateUtility.toMinute(validPair.in.date);
				workTime = workTime + DateUtility.toMinute(validPair.out.date);
			}
		}
	
		//Il pranzo e' servito??		
		WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay();
		
		
		//se mealTicketTime è zero significa che il dipendente nel giorno non ha diritto al calcolo del buono pasto
		if( ! wttd.mealTicketEnabled() ) {
					
			setIsTickeAvailable(pd,false);
			return workTime + justifiedTimeAtWork;
		}
		
		int mealTicketTime = wttd.mealTicketTime;					//6 ore
		int breakTicketTime = wttd.breakTicketTime;					//30 minuti
		int breakTimeDiff = breakTicketTime;
		pd.stampModificationType = null;
		List<PairStamping> gapLunchPairs = getGapLunchPairs(pd,validPairs);
		
		if(gapLunchPairs.size()>0){
			//	recupero la durata della pausa pranzo fatta		
			int minTimeForLunch = gapLunchPairs.get(0).timeInPair;
			//Calcolo l'eventuale differenza tra la pausa fatta e la pausa minima
			breakTimeDiff = (breakTicketTime-minTimeForLunch<=0) ? 0 : (breakTicketTime-minTimeForLunch);
		}
		
		if(workTime - breakTimeDiff >= mealTicketTime){
			setIsTickeAvailable(pd,true);
			
			if(!pd.isTicketForcedByAdmin || pd.isTicketForcedByAdmin&&pd.isTicketAvailable ) //TODO decidere la situazione intricata se l'amministratore forza a true
				workTime -= breakTimeDiff;
			
			// caso in cui non sia stata effettuata una pausa pranzo
			if(breakTimeDiff == breakTicketTime) {
				
				pd.stampModificationType = stampingDao
							.getStampModificationTypeByCode(StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
			}
			// Caso in cui la pausa pranzo fatta è inferiore a quella minima
			else if(breakTimeDiff > 0 && breakTimeDiff != breakTicketTime) {
				
				pd.stampModificationType = stampingDao
						.getStampModificationTypeByCode(StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
			}
		}
		
		else{
			setIsTickeAvailable(pd,false);
		}
				
		return workTime + justifiedTimeAtWork;

	}
	
	/**
	 * 
	 * @return lo stamp modification type relativo al tempo di lavoro fisso 
	 */
	public StampModificationType getFixedWorkingTime() {
		
		//TODO usato solo in PersonStampingDayRecap bisogna metterlo nella cache
		return stampingDao.getStampModificationTypeById(StampModificationTypeValue.FIXED_WORKINGTIME.getId());
	}

	/**
	 * Ordina per orario la lista delle stamping nel person day
	 */
	public static void orderStampings(PersonDay pd) {
		
		Collections.sort(pd.stampings);
	}
		
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
		Integer mealTimeStartHour = Integer.parseInt(ConfGeneralManager.getFieldValue("meal_time_start_hour", pd.person.office));
		Integer mealTimeStartMinute = Integer.parseInt(ConfGeneralManager.getFieldValue("meal_time_start_minute", pd.person.office));
		Integer mealTimeEndHour = Integer.parseInt(ConfGeneralManager.getFieldValue("meal_time_end_hour", pd.person.office));
		Integer mealTimeEndMinute = Integer.parseInt(ConfGeneralManager.getFieldValue("meal_time_end_minute", pd.person.office));
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
			if(last==null)
				last = s;
			else if(last.date.isBefore(s.date))
				last = s;
		}
		return last;
	}
	
	/**
	 * 
	 * importa il  numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork(PersonDay pd)
	{
		pd.timeAtWork = getCalculatedTimeAtWork(pd);
	}

	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */
	private void updateDifference(PersonDay pd){
		
		int worktime =  WorkingTimeTypeDao
				.getWorkingTimeTypeStatic(pd.date, pd.person)
				.workingTimeTypeDays.get(pd.date.getDayOfWeek()-1).workingTime;
		
		//persona fixed
		if(pd.isFixedTimeAtWork() && pd.timeAtWork == 0){
			pd.difference = 0;
			return;
		}
	
		//festivo
		if(pd.isHoliday()){
			pd.difference = pd.timeAtWork;
			return;
		}
		
		//assenze giornaliere
		if(isAllDayAbsences(pd)){
			pd.difference = 0;
			return;
		}
		
		//feriale
		pd.difference = pd.timeAtWork - worktime;
	
	}	


	/**
	 * calcola il valore del progressivo giornaliero e lo salva sul db
	 */
	private void updateProgressive(PersonDay pd)
	{

		//primo giorno del mese
		if(pd.previousPersonDayInMonth==null)
		{
			pd.progressive = pd.difference;
			return;
		}
		
		//primo giorno del contratto
		if(pd.previousPersonDayInMonth.getPersonDayContract() == null || pd.previousPersonDayInMonth.getPersonDayContract().id != pd.personDayContract.id)
		{
			pd.progressive = pd.difference;
			return;
		}
		
		//caso generale
		pd.progressive = pd.difference + pd.previousPersonDayInMonth.progressive;

	}
	
	
	/**
	 * Assegna ad ogni person day del mese il primo precedente esistente.
	 * Assegna null al primo giorno del mese.
	 */
	private void associatePreviousInMonth(PersonDay pd)
	{
		LocalDate beginMonth = pd.date.dayOfMonth().withMinimumValue();
		LocalDate endMonth = pd.date.dayOfMonth().withMaximumValue();
		
		List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(pd.person, beginMonth, Optional.fromNullable(endMonth), true);
		for(int i=1; i<pdList.size(); i++)
		{
			pdList.get(i).previousPersonDayInMonth = pdList.get(i-1);
		}
	}

	/**
	 * Aggiorna il campo ticket available e persiste il dato. Controllare per le persone fixed nel giorno di festa.
	 */
	private void updateTicketAvailable(PersonDay pd)
	{
		//caso forced by admin
		if(pd.isTicketForcedByAdmin)
		{
			pd.save();
			return;
		}
		
		//caso persone fixed
		if(pd.isFixedTimeAtWork())
		{
			if(pd.isHoliday())
			{
				pd.isTicketAvailable = false;
				pd.save();
			}
			else if(!pd.isHoliday() && !isAllDayAbsences(pd))
			{
				pd.isTicketAvailable = true;
				pd.save();
			}
			else if(!pd.isHoliday() && isAllDayAbsences(pd))
			{
				pd.isTicketAvailable = false;
				pd.save();
			}
			return;
		}

		//caso persone normali
		pd.isTicketAvailable = pd.isTicketAvailable && isTicketAvailableForWorkingTime(pd);
		return; 
	}

	
	/**
	 * Setta il valore della variabile isTicketAvailable solo se isTicketForcedByAdmin è false
	 * @param value
	 */
	private void setIsTickeAvailable(PersonDay pd, boolean isTicketAvailable)
	{
		if(!pd.isTicketForcedByAdmin)
			pd.isTicketAvailable = isTicketAvailable;
	}
	
	
	/**
	 * Il personDay precedente 
	 * @return
	 */
	public PersonDay previousPersonDay(PersonDay pd)
	{
		//TODO usato solo in PersonStampingDayRecap, vedere come ottimizzarlo
		PersonDay lastPreviousPersonDayInMonth = 
				personDayDao.getPersonDayForRecap(pd.person, Optional.fromNullable(pd.date.dayOfMonth().withMinimumValue()), pd.date);
		return lastPreviousPersonDayInMonth;
	}


	/**
	 * (1) Controlla che il personDay sia ben formato (altrimenti lo inserisce nella tabella PersonDayInTrouble.
	 * (2) Popola i valori aggiornati del person day e li persiste nel db
	 */
	public void populatePersonDay(PersonDay pd)
	{

		//il contratto non esiste più nel giorno perchè è stata inserita data terminazione
		if(pd.getPersonDayContract() == null){
			pd.timeAtWork = 0;
			pd.progressive = 0;
			pd.difference = 0;
			setIsTickeAvailable(pd,false); //TODO calcolarlo se ci sono timbrature
			pd.stampModificationType = null;
			pd.save();
			return;
		}
		
		//controllo problemi strutturali del person day
		if(pd.date.isBefore(new LocalDate())){
			pd.save();
			checkForPersonDayInTrouble(pd);
		}
		
		//Strutture dati transienti necessarie al calcolo
		if(pd.getPersonDayContract()==null)
		{
			return;
		}
		
		if(pd.previousPersonDayInMonth==null)
		{
			associatePreviousInMonth(pd);
		}
		
		if(pd.previousPersonDayInMonth!=null && pd.previousPersonDayInMonth.personDayContract==null)
		{
			//this.previousPersonDayInMonth.personDayContract = this.person.getContract(this.previousPersonDayInMonth.date);
			pd.previousPersonDayInMonth.personDayContract = ContractDao.getContract(pd.previousPersonDayInMonth.date, pd.person);
		}
	
		//controllo uscita notturna
		checkExitStampNextDay(pd);
		
		updateTimeAtWork(pd);
		
		updateDifference(pd);
	
		updateProgressive(pd);
		
		updateTicketAvailable(pd);
		

		//Nel caso in cui il personDay sia precedente a sourceContract imposto i valori a 0
		if(pd.personDayContract != null 
				&& pd.personDayContract.sourceDate != null 
				&& pd.date.isBefore(pd.personDayContract.sourceDate)) {
			pd.timeAtWork = 0;
			pd.progressive = 0;
			pd.difference = 0;
			setIsTickeAvailable(pd,false); //TODO calcolarlo se ci sono timbrature
			pd.stampModificationType = null;
		}
		
		pd.save();
		
	}	
	
	/** 
	 * Aggiorna tutti i personday a partire dalla data specificata fino al giorno corrente
	 * @param person
	 * @param date
	 */
	public void updatePersonDaysFromDate(Person person, LocalDate date){
		
		Preconditions.checkNotNull(person);
		Preconditions.checkState(person.isPersistent());
		Preconditions.checkNotNull(date);
		
		//Verifico se la data è passata, in caso contrario non è necessario ricalcolare nulla
		if(date.isAfter(LocalDate.now())){
			return;
		}
		
		//Prendo la lista ordinata di tutti i personday della persona fino ad oggi e effettuo il ricalcolo su tutti
		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date, Optional.of(LocalDate.now()), true);

		for(PersonDay pd : personDays){
			populatePersonDay(pd);
		}
	}

	/**
	 * Stessa logica di populatePersonDay ma senza persistere i calcoli (usato per il giorno di oggi)
	 */
	public void queSeraSera(PersonDay pd)
	{
		//Strutture dati transienti necessarie al calcolo
		if(pd.getPersonDayContract() == null) {
			
			return;
		}
		
		if(pd.previousPersonDayInMonth==null) {
			
			associatePreviousInMonth(pd);
		}
		
		if(pd.previousPersonDayInMonth!=null && pd.previousPersonDayInMonth.personDayContract==null) {
			
			pd.previousPersonDayInMonth.personDayContract = ContractDao.getContract(pd.previousPersonDayInMonth.date, pd.person);
		}
		
		updateTimeAtWork(pd);
		updateDifference(pd);
		updateProgressive(pd);
		updateTicketAvailable(pd);

	}
	
	
	/**
	 * Verifica che nel person day vi sia una situazione coerente di timbrature. Situazioni errate si verificano nei casi 
	 *  (1) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 	(2) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature. 
	 * In caso di situazione errata viene aggiunto un record nella tabella PersonDayInTrouble.
	 * Se il PersonDay era presente nella tabella PersonDayInTroubled ed è stato fixato, viene settato a true il campo
	 * fixed.
	 * @param pd
	 * @param person
	 */
	public void checkForPersonDayInTrouble(PersonDay pd)
	{
		//se prima o uguale a source contract il problema è fixato
		if(pd.getPersonDayContract().sourceDate != null) {
			
			if( ! pd.date.isAfter( pd.getPersonDayContract().sourceDate ) ) {
				
				for(PersonDayInTrouble pdt : pd.troubles) {
					if(pdt.fixed == false) {
						pdt.fixed = true;
						pdt.save();
						log.info("Fixato {} perchè precedente a sourceContract({})",
								pd.date, pd.getPersonDayContract().sourceDate);
					}
				}
				return;
			}
		}
		
		//persona fixed
		if(pd.isFixedTimeAtWork())
		{
			if(pd.stampings.size()!=0)
			{
				computeValidStampings(pd);
				for(Stamping s : pd.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTroubleManager.insertPersonDayInTrouble(pd, "timbratura disaccoppiata persona fixed");
						return;
					}
				}
			}			
		}
		//persona not fixed
		else
		{
			//caso no festa, no assenze, no timbrature
			if(!isAllDayAbsences(pd) && pd.stampings.size()==0 && !pd.isHoliday() && !isEnoughHourlyAbsences(pd))
			{
				PersonDayInTroubleManager.insertPersonDayInTrouble(pd, "no assenze giornaliere e no timbrature");
				return;
			}
			
			//caso no festa, no assenze, timbrature disaccoppiate
			if(!isAllDayAbsences(pd) && !pd.isHoliday())
			{
				computeValidStampings(pd);
				for(Stamping s : pd.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTroubleManager.insertPersonDayInTrouble(pd, "timbratura disaccoppiata giorno feriale");
						return;
					}
				}
			}
			
			//caso festa, no assenze, timbrature disaccoppiate
			else if(!isAllDayAbsences(pd) && pd.isHoliday())
			{
				computeValidStampings(pd);
				for(Stamping s : pd.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTroubleManager.insertPersonDayInTrouble(pd, "timbratura disaccoppiata giorno festivo");
						return;
					}
				}
			}
		}
		
		//giorno senza problemi, se era in trouble lo fixo
		if(pd.troubles!=null && pd.troubles.size()>0)
		{
			//per adesso no storia, unico record
			PersonDayInTrouble pdt = pd.troubles.get(0);	
			pdt.fixed = true;
			pdt.save();
			//this.troubles.add(pdt);
			pd.save();
		}

	}
	
	/**
	 * 
	 * @return true se il person day è in trouble
	 */
	public static boolean isInTrouble(PersonDay pd)
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
			orderStampings(pd);
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
	 * 
	 * @param pd
	 * controlla che esistano timbrature di ingresso relative al giorno precedente non accoppiate poichè la corrispondente timbratura 
	 * di uscita è stata effettuata dopo la mezzanotte del giorno precedente, ricadendo così sul personday attuale 
	 */
	private void checkExitStampNextDay(PersonDay pd){
		
		if(pd.isFixedTimeAtWork())
			return;
		
		if(pd.date.getDayOfMonth()==1) {
			pd.previousPersonDayInMonth = personDayDao.getPersonDayForRecap(pd.person, Optional.<LocalDate>absent(), pd.date);
			if(pd.previousPersonDayInMonth != null && pd.previousPersonDayInMonth.date.isBefore( 
					new LocalDate(pd.previousPersonDayInMonth.date.getYear(), 
							pd.previousPersonDayInMonth.date.getMonthOfYear(), 
							pd.previousPersonDayInMonth.date.getDayOfMonth()).dayOfMonth().withMaximumValue()))
				pd.previousPersonDayInMonth = null;	
		}
		
		if(pd.previousPersonDayInMonth==null) //primo giorno del contratto
			return;
		if(!pd.previousPersonDayInMonth.date.plusDays(1).isEqual(pd.date)){
			return;//giorni non consecutivi
		}
			
		
		Stamping lastStampingPreviousDay = getLastStamping(pd.previousPersonDayInMonth);
		
		if(lastStampingPreviousDay != null && lastStampingPreviousDay.isIn())
		{
			orderStampings(pd);
			String hourMaxToCalculateWorkTime = ConfYearManager.getFieldValue("hour_max_to_calculate_worktime", pd.date.getYear(), pd.person.office);
			Integer maxHour = Integer.parseInt(hourMaxToCalculateWorkTime);
			if(pd.stampings.size() > 0 && pd.stampings.get(0).way == WayType.out && maxHour > pd.stampings.get(0).date.getHourOfDay())
			{
				Stamping correctStamp = new Stamping();
				correctStamp.date = new LocalDateTime(pd.previousPersonDayInMonth.date.getYear(), pd.previousPersonDayInMonth.date.getMonthOfYear(), pd.previousPersonDayInMonth.date.getDayOfMonth(), 23, 59);
				correctStamp.way = WayType.out;
				correctStamp.markedByAdmin = false;
				correctStamp.stampModificationType = stampingDao.getStampModificationTypeById(4l);
				correctStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				correctStamp.personDay = pd.previousPersonDayInMonth;
				correctStamp.save();
				pd.previousPersonDayInMonth.stampings.add(correctStamp);
				pd.previousPersonDayInMonth.save();

				populatePersonDay(pd.previousPersonDayInMonth);
				Stamping newEntranceStamp = new Stamping();
				newEntranceStamp.date = new LocalDateTime(pd.date.getYear(), pd.date.getMonthOfYear(), pd.date.getDayOfMonth(),0,0);
				newEntranceStamp.way = WayType.in;
				newEntranceStamp.markedByAdmin = false;
				newEntranceStamp.stampModificationType = stampingDao.getStampModificationTypeById(4l);
				newEntranceStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				newEntranceStamp.personDay = pd;
				newEntranceStamp.save();
				pd.stampings.add(newEntranceStamp);
				pd.save();
			}

			if(pd.date.getDayOfMonth() == 1){
				pd.previousPersonDayInMonth = null;
				pd.save();
			}
		}
		
		if(pd.date.getDayOfMonth() == 1){
			pd.previousPersonDayInMonth = null;
			pd.save();
		}

	}
	
	
	/**
	 * @return lo stamp modification type relativo alla timbratura aggiunta dal sistema nel caso mancasse la timbratura d'uscita prima
	 * della mezzanotte del giorno in questione
	 */
	public StampModificationType checkMissingExitStampBeforeMidnight(PersonDay pd)
	{
		//FIXME renderlo efficiente
		StampModificationType smt = null;
		for(Stamping st : pd.stampings){
			if(st.stampModificationType != null && st.stampModificationType.equals(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getStampModificationType()))
				smt = stampingDao.getStampModificationTypeById(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getId());
			}
		return smt;
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
			int coupleOfStampings = PersonUtility.numberOfInOutInPersonDay(pd);

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
	public List<PersonDay> getTotalPersonDayInMonth(Person person, int year, int month)
	{
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
			if(!pd.isHoliday() )
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
			if(pd.isHoliday() || pd.isToday() ) 
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

}
