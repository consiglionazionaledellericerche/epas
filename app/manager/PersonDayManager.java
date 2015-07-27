package manager;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import manager.cache.StampTypeManager;
import models.Absence;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.StampModificationTypeCode;
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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperPersonDay;

public class PersonDayManager {

	@Inject
	public PersonDayManager(PersonDayDao personDayDao,
			StampTypeManager stampTypeManager,
			AbsenceDao absenceDao,
			ConfGeneralManager confGeneralManager,
			PersonDayInTroubleManager personDayInTroubleManager,
			ContractMonthRecapManager contractMonthRecapManager,
			VacationManager vacationManager) {

		this.personDayDao = personDayDao;
		this.stampTypeManager = stampTypeManager;
		this.confGeneralManager = confGeneralManager;
		this.personDayInTroubleManager = personDayInTroubleManager;
		this.contractMonthRecapManager = contractMonthRecapManager;
	}

	private final static Logger log = LoggerFactory.getLogger(PersonDayManager.class);

	private final PersonDayDao personDayDao;
	private final StampTypeManager stampTypeManager;
	private final ConfGeneralManager confGeneralManager;
	private final PersonDayInTroubleManager personDayInTroubleManager;
	private final ContractMonthRecapManager contractMonthRecapManager;
	
	/**
	 * @return true se nel giorno vi e' una assenza giornaliera
	 */
	public boolean isAllDayAbsences(PersonDay pd) {
		
		if(pd.absences.size() == 0) {
			return false;
		}
		
		if(pd.absences.size() == 1) {

			Absence abs = pd.absences.get(0);
			
			// TODO: per adesso il telelavoro lo considero come giorno lavorativo
			// normale. Chiedere ai romani.
			if (abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
				return false;
			}
			
			return abs.justifiedMinutes == null //eludo PIPE, RITING etc...
					&& abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay);
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
	 * //FIXME: questo metodo per motivi di sicurezza non dovrebbe modificare il personDay.
	 * Ma dovrebbe esclusivamente fornire risultati. Creare un Message che contenga
	 * tutte le decisioni dell'algoritmo e delegare il chiamante alla modifica dei 
	 * campi del person day. 
	 *
	 * @return il numero di minuti trascorsi a lavoro
	 */
	public int getCalculatedTimeAtWork(IWrapperPersonDay pd) {

		Preconditions.checkState( pd.getWorkingTimeTypeDay().isPresent() );

		int justifiedTimeAtWork = 0;
		pd.getValue().stampModificationType = null;

		//Se hanno il tempo di lavoro fissato non calcolo niente
		if ( pd.isFixedTimeAtWork() ) {

			if( pd.getValue().isHoliday ) {
				return 0;
			}
			return pd.getWorkingTimeTypeDay().get().workingTime;
		}
		
		for(Absence abs : pd.getValue().absences) {

			if(abs.absenceType.code.equals(AbsenceTypeMapping.TELELAVORO.getCode())) {
				return pd.getWorkingTimeTypeDay().get().workingTime;
			}
			
			// Caso di assenza giornaliera.  
			if(abs.justifiedMinutes == null && //evito i PEPE, RITING etc...
					abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
				
				setIsTickeAvailable(pd, false);
				return 0;
			}
			
			// Giustificativi grana minuti (priorità sugli altri casi)
			if( abs.justifiedMinutes != null) {
				justifiedTimeAtWork += abs.justifiedMinutes;
				continue;
			}
			// FIXME: togliere questa eccezione dal codice.
			if( !abs.absenceType.code.equals("89") && 
					abs.absenceType.justifiedTimeAtWork.minutesJustified != null) {

				justifiedTimeAtWork += abs.absenceType.justifiedTimeAtWork.minutesJustified;
				continue;
			}
			if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.HalfDay){

				justifiedTimeAtWork += pd.getWorkingTimeTypeDay().get().workingTime / 2;
				continue;
			}
		}

		//se non c'è almeno una coppia di timbrature considero il justifiedTimeAtwork
		//(che però non contribuisce all'attribuzione del buono mensa che quindi è certamente non assegnato)
		if (pd.getValue().stampings.size() < 2) {
			setIsTickeAvailable(pd, false);
			return justifiedTimeAtWork;
		}

		Collections.sort(pd.getValue().stampings);

		if(pd.getValue().isHoliday){

			List<PairStamping> validPairs = getValidPairStamping(pd.getValue().stampings);

			int holidayWorkTime = 0;
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

		//IL PRANZO E' SERVITOOOOO????
		
		// Ticket non previsto dall'orario di lavoro.
		WorkingTimeTypeDay wttd = pd.getWorkingTimeTypeDay().get();
		if( ! wttd.mealTicketEnabled() ) {

			setIsTickeAvailable(pd, false);
			return workTime + justifiedTimeAtWork;
		}

		// Calcolo ...
		
		int mealTicketTime = wttd.mealTicketTime;					//6 ore
		int minBreakTicketTime = wttd.breakTicketTime;				//30 minuti
	
		List<PairStamping> gapLunchPairs = getGapLunchPairs(pd.getValue(), validPairs);
		int effectiveTimeSpent = 0;
		
		// 1) Calcolo del tempo passato in pausa pranzo dalle timbrature.

		// DA CONFIGURARE /////////////////////////////////////////////////////
		boolean baessoAlgorithm = true;
		for(PairStamping lunchPair : gapLunchPairs) {
			if(lunchPair.prPair) {
				baessoAlgorithm = false;
			}
		}
		
		if(baessoAlgorithm) {
			if(gapLunchPairs.size() > 0 ){
				//	recupero la durata della pausa pranzo fatta
				effectiveTimeSpent = gapLunchPairs.get(0).timeInPair;
			}
		} else {
			// sommo tutte le pause marcate come pr
			for(PairStamping lunchPair : gapLunchPairs) {
				if(lunchPair.prPair) {
					effectiveTimeSpent += lunchPair.timeInPair;
				}
			}
		}
		///////////////////////////////////////////////////////////////////////
		
		//2) Calcolo l'eventuale differenza tra la pausa fatta e la pausa minima
		int missingTime = minBreakTicketTime - effectiveTimeSpent;
		if(missingTime < 0) {
			missingTime = 0;
		}
		
		//3) Decisioni
		
		// Non ho eseguito il tempo minimo per buono pasto.
		if (workTime - missingTime < mealTicketTime) {
			setIsTickeAvailable(pd, false);
			return workTime + justifiedTimeAtWork;
		}

		// Calcolo tempo decurtato per pausa troppo breve.
		int workingTimeDecurted = workTime;
		if(missingTime > 0) {
			workingTimeDecurted = workTime - missingTime;
		}
		
		// Controllo pausa pomeridiana (solo se la soglia è definita)
		if( !isAfternoonThresholdConditionSatisfied(validPairs, 
				pd.getWorkingTimeTypeDay().get()) ) {
			
			setIsTickeAvailable(pd, false);
			return workTime + justifiedTimeAtWork;
		}
		
		// Decidere quando verrà il momento di fare i conti con gianvito...
//		if( !isGianvitoConditionSatisfied(workingTimeDecurted, justifiedTimeAtWork, 
//				pd.getValue().date, pd.getPersonDayContract().get(), 
//				pd.getWorkingTimeTypeDay().get()) ){
//			
//			setIsTickeAvailable(pd, false);
//			return workTime + justifiedTimeAtWork;
//		}
	
		
		// IL BUONO PASTO E' STATO ATTRIBUITO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		setIsTickeAvailable(pd,true);

		// marcatori: versione p ed e
//		//e
//		if(effectiveTimeSpent > 0 && workingTimeDecurted < workTime) {
//			pd.getValue().stampModificationType = stampTypeManager.getStampMofificationType(
//					StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
//		}
//		//p
//		if(effectiveTimeSpent == 0) {
//			pd.getValue().stampModificationType = stampTypeManager.getStampMofificationType(
//					StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
//		}
		
		// marcatori: versione con solo e salvataggio del tempo decurtato.
		//e
		if(workingTimeDecurted < workTime) {
		
			pd.getValue().decurted = missingTime;
		}
			
		return workingTimeDecurted + justifiedTimeAtWork;

		
	}

	/**
	 * La condizione del lavoro minimo pomeridiano è soddisfatta?
	 * @param validPairs
	 * @param wttd
	 * @return
	 */
	private boolean isAfternoonThresholdConditionSatisfied(List<PairStamping> validPairs, 
			WorkingTimeTypeDay wttd) {
		
		if(wttd.ticketAfternoonThreshold == null) {
			return true;
		}
		
		int threshold = wttd.ticketAfternoonThreshold;
		int workingTimeInThreshold = 0;
		
		for(PairStamping pair : validPairs) {
			
			int inMinute = DateUtility.toMinute(pair.in.date);
			int outMinute = DateUtility.toMinute(pair.out.date);
			
			if( inMinute <= threshold && outMinute >= threshold) {
				workingTimeInThreshold += outMinute - threshold;
			}
			
			if( inMinute >= threshold) {
				workingTimeInThreshold += outMinute - inMinute;
			}
		}
		if(workingTimeInThreshold >= wttd.ticketAfternoonWorkingTime) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * La condizione Gianvito è una condizione che deteriorerà l'efficienza!
	 * Per attribuire il buono pasto oltre a soddisfare le soglie di tempo di lavoro
	 * minimo per buono pasto e il tempo di lavoro minimo pomeridiano, devo anche
	 * avere giustificativi e/o flessibilità sufficiente a raggiungere il tempo 
	 * di orario giornaliero.
	 * 
	 * Per fare questo conteggio devo calcolarmi la situazione residua al giorno 
	 * precedente che non è persistita. Per tornare ad essere efficienti si deve 
	 * injettare nella populate person day la situazione calcolata al giorno precedente.
	 *
	 * Secondo me nascono delle problematiche significative nel caso in cui volessi 
	 * inserire dei riposi compensativi nel passato. Questo comporterebbe il ricalcolo
	 * dei giorni e potrebbe risultare cambiata in negativo una decisione passata 
	 * di attribuzione del buono pasto. Per ovviare a questa evenienza si dovrebbe
	 * effettuare un check di ogni giorno successivo al riposo compensativo da inserire
	 * e verificare che con tale assenza inserita non si va mai in negativo.
	 * Ma questo fatto potrebbe essere intercettato dal dipendente che si vede attribuire
	 * il riposo compensativo e cancellare il buono pasto e quindi potrebbe contattare
	 * la segreteria.
	 * 
	 * Una soluzione a ogni problema potrebbe essere quella di persistere nel db
	 * in ogni personDay la situazione di flessibilità raggiunta
	 * - residui anno passato, anno corrente e buoni pasto.
	 * In questo modo l'utente anche visivamente avrebbe la percezione di quello 
	 * che sta succedendo nel giorno.
	 * 
	 * @param workingTimeDecurted
	 * @param justifiedTimeAtWork
	 * @param wttd
	 * @return
	 */
	private boolean isGianvitoConditionSatisfied(int workingTimeDecurted, 
			int justifiedTimeAtWork, LocalDate date, Contract contract, WorkingTimeTypeDay wttd) {
		
		// - Ho il tempo di lavoro (eventualmente decurtato) che raggiunge il tempo di lavoro giornaliero.
		// 		ok buono pasto
		if( workingTimeDecurted >= wttd.workingTime) {
			return true;
		}
		
		// - Ho il tempo di lavoro (eventualmente decurtato) che non raggiunge il tempo di lavoro giornaliero.
		//	 - Aggiungo PEPE 
		//     - livello raggiunto ok buono pasto
		//TODO: capire se in justifiedTimeAtWork posso considerare tutte le assenze
		// orarie o solo PEPE e gravi motivi personali.
		if( workingTimeDecurted + justifiedTimeAtWork >= wttd.workingTime) {
			return true;
		}
		
		log.info(contract.person.toString() + date);
		//Ultimo tentativo con flessibilità
		int flexibility = contractMonthRecapManager
				.getMinutesForCompensatoryRest(contract, date.minusDays(1), new ArrayList<Absence>());
		if( workingTimeDecurted + justifiedTimeAtWork + flexibility >= wttd.workingTime )  {
			return true;
		}

		return false;
	}


	/**
	 * 
	 * FIXME: metodo usato in days per protime. Rimuoverlo e trovare una 
	 * soluzione furba perchè non è più mantenuto. 
	 * 
	 * Calcola i minuti lavorati nel person day. Assegna il campo
	 * isTicketAvailable.
	 *
	 * @return il numero di minuti trascorsi a lavoro
	 */
	@Deprecated
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
						&& pd.getValue().isTicketAvailable) {
					workTime = workTime - breakTimeDiff;
				}

				// caso in cui non sia stata effettuata una pausa pranzo
				if (breakTimeDiff == breakTicketTime) {

					pd.getValue().stampModificationType = 
							stampTypeManager.getStampMofificationType(
									StampModificationTypeCode.FOR_DAILY_LUNCH_TIME);
				}

				// Caso in cui la pausa pranzo fatta è inferiore a quella minima
				else if (breakTimeDiff > 0 && breakTimeDiff != breakTicketTime) {

					pd.getValue().stampModificationType =  
							stampTypeManager.getStampMofificationType(
									StampModificationTypeCode.FOR_MIN_LUNCH_TIME);
				}
			}

			setIsTickeAvailable(pd, false);

		}

		return workTime;
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
	 * assumendo la timbratura al di fuori della fascia uguale al limite di tale fascia. 
	 * (Le timbrature vengono tuttavia mantenute originali
	 * per garantire l'usabilità anche ai controller che gestiscono reperibilità e turni)
	 * @param validPairs le coppie di timbrature ritenute valide all'interno del giorno
	 * @return
	 */
	public List<PairStamping> getGapLunchPairs(PersonDay pd, List<PairStamping> validPairs) {
		
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
	 * Assegna il numero di minuti in cui una persona è stata a lavoro in quella data
	 * 
	 * @param pd
	 */
	public void updateTimeAtWork(IWrapperPersonDay pd) {
		
		pd.getValue().timeAtWork = getCalculatedTimeAtWork(pd);
	}

	/**
	 * Popola il campo difference del PersonDay.
	 * 
	 * @param pd
	 */
	public void updateDifference(IWrapperPersonDay pd) {

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
	public void updateProgressive(IWrapperPersonDay pd) {

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
	public void updateTicketAvailable(IWrapperPersonDay pd) {

		//caso forced by admin
		if(pd.getValue().isTicketForcedByAdmin) {
			return;
		}

		//caso persone fixed
		if(pd.isFixedTimeAtWork()) {
			if(pd.getValue().isHoliday) {
				pd.getValue().isTicketAvailable = false;
			} else if(!pd.getValue().isHoliday && !isAllDayAbsences(pd.getValue())) {
				pd.getValue().isTicketAvailable = true;
			} else if(!pd.getValue().isHoliday && isAllDayAbsences(pd.getValue())) {
				pd.getValue().isTicketAvailable = false;
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
	public void setIsTickeAvailable(IWrapperPersonDay pd, boolean isTicketAvailable) {

		if( ! pd.getValue().isTicketForcedByAdmin) {

			pd.getValue().isTicketAvailable = isTicketAvailable;
		}
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
		updateTicketAvailable(pd);
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
			if (!pdt.fixed) {
				pdt.fixed = true;
				pdt.save();
				//pd.getValue().save();
			}
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
	 * Calcola il numero massimo di coppie di colonne ingresso/uscita.
	 * 
	 * @param personDays
	 * @return
	 */
	public int getMaximumCoupleOfStampings(List<PersonDay> personDays) {
		int max = 0;
		for(PersonDay pd : personDays) {
			int coupleOfStampings = numberOfInOutInPersonDay(pd);
			if (max < coupleOfStampings) {
				max = coupleOfStampings;
			}
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
	public List<PersonDay> getTotalPersonDayInMonth(List<PersonDay> personDays, 
			Person person, int year, int month) {
		
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> totalDays = new ArrayList<PersonDay>();

		int currentWorkingDays = 0;
		LocalDate currentDate = beginMonth;
		while(!currentDate.isAfter(endMonth))
		{
			if(currentWorkingDays<personDays.size() && personDays.get(currentWorkingDays).date.isEqual(currentDate))
			{
				totalDays.add(personDays.get(currentWorkingDays));
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
	 * Il numero di buoni pasto usabili all'interno della lista di 
	 * person day passata come parametro
	 * @param personDays
	 * @return
	 */
	public int numberOfMealTicketToUse(List<PersonDay> personDays){
		int number = 0;	
		for(PersonDay pd : personDays) {
			if(pd.isTicketAvailable && !pd.isHoliday ) {
				number++;
			}
		}
		return number;
	}


	/**
	 * Il numero di buoni pasto da restituire all'interno della lista di 
	 * person day passata come parametro 
	 * @param personDays
	 * @return
	 */
	public int numberOfMealTicketToRender(List<PersonDay> personDays){
		int ticketTorender = 0;
		for(PersonDay pd : personDays) {
			
			if(!pd.isTicketAvailable) {
				//i giorni festivi e oggi
				if(pd.isHoliday || pd.isToday() ) {
					continue;
				}
				//i giorni futuri in cui non ho assenze
				if(pd.date.isAfter(LocalDate.now()) && pd.absences.isEmpty()) {
					continue;
				}
				ticketTorender++;
			}
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
