package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import manager.cache.CompetenceCodeManager;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.WorkingTimeTypeDay;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

/**
 * 
 * @author alessandro
 *
 */
public class ContractMonthRecapManager {

	@Inject 
	private MealTicketDao mealTicketDao;
	@Inject
	private PersonDayDao personDayDao;
	@Inject
	private CompetenceDao competenceDao;
	@Inject
	private CompetenceCodeManager competenceCodeManager;
	@Inject
	private AbsenceDao absenceDao;
	@Inject
	private IWrapperFactory wrapperFactory;
	@Inject
	private ConfGeneralManager confGeneralManager;
	@Inject
	private ConfYearManager confYearManager;
	
	
	//private final static Logger log = LoggerFactory.getLogger(ContractMonthRecapManager.class);
	
	/**
	 * Metodo da utilizzare per calcolare i minuti di residuo disponibili per 
	 * riposo compensativo. Per adesso è in questa classe per proteggere l'istanza
	 * effettiva di ContractMonthRecap in modo che non si corra il rischio di 
	 * sovrascriverla con quella provvisoria. Capire ...
	 * 
	 * @param contract
	 * @param date
	 * @return
	 */
	public int getMinutesForCompensatoryRest (
			Contract contract, LocalDate date, List<Absence> otherAbsences) {
		
		ContractMonthRecap cmr = new ContractMonthRecap();
		cmr.year = date.getYear();
		cmr.month = date.getMonthOfYear();
		cmr.contract = contract;
		
		Optional<ContractMonthRecap> recap = 
				computeResidualModule(cmr, new YearMonth(date), date, otherAbsences);
		
		if( recap.isPresent() ) {
			return recap.get().remainingMinutesCurrentYear 
					+ recap.get().remainingMinutesLastYear;
		}
		return 0;
	}
	
	/**
	 * Aggiorna i campi inerenti la situazione residuale del riepilogo mensile.
	 *  
	 * NB: Non effettua salvataggi ma solo assegnamenti.
	 *
	 * Popola la parte residuale del riepilogo mensile fino alla data calcolaFinoA: 
	 *  - minuti rimanenti dell'anno passato
	 *  - minuti rimanenti dell'anno corrente
	 *  - buoni pasto rimanenti
	 * 
	 * @param cmr
	 * @param yearMonth
	 * @param calcolaFinoA
	 * @return il riepilogo costruito.
	 */
	public Optional<ContractMonthRecap> computeResidualModule(ContractMonthRecap cmr, 
			YearMonth yearMonth, LocalDate calcolaFinoA, List<Absence> otherAbsences) {

		IWrapperContract wcontract = wrapperFactory.create(cmr.contract);
		Contract contract = cmr.contract;

		//Se ho necessità del riepilogo del mese precedente lo cerco nel DataBase.
		// Se non lo trovo non posso costruire il riepilogo quindi esco.
		Optional<ContractMonthRecap> recapPreviousMonth = 
				Optional.<ContractMonthRecap>absent();
		Optional<YearMonth> firstContractMonthRecap = wrapperFactory
				.create(contract).getFirstMonthToRecap();
		if (!firstContractMonthRecap.isPresent()) {
			return Optional.<ContractMonthRecap>absent();
		}
		if ( yearMonth.isAfter(firstContractMonthRecap.get()) ) {
			//Riepilogo essenziale
			recapPreviousMonth = wcontract.getContractMonthRecap(yearMonth.minusMonths(1));
			if( !recapPreviousMonth.isPresent() ) {
				//Errore nella costruzione manca il riepilogo essenziale
				return Optional.absent();
			}
		} 

		//Valori dei residui all'inizio del mese richiesto
		//Se è il mese di beginContrat i residui sono 0
		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;
		
		if( recapPreviousMonth.isPresent() ) {
			//Se ho il riepilogo del mese precedente lo utilizzo.
			if ( recapPreviousMonth.get().month == 12 ) {
				initMonteOreAnnoPassato = 
						recapPreviousMonth.get().remainingMinutesCurrentYear 
						+ recapPreviousMonth.get().remainingMinutesLastYear;
			
			} else {
				initMonteOreAnnoCorrente = recapPreviousMonth.get().remainingMinutesCurrentYear; 
				initMonteOreAnnoPassato = recapPreviousMonth.get().remainingMinutesLastYear;
			}
		}
		else if ( contract.sourceDate != null 
				&& contract.sourceDate.getYear() == yearMonth.getYear() 
				&& contract.sourceDate.getMonthOfYear() == yearMonth.getMonthOfYear() )	{
			//Se è il primo riepilogo dovuto ad inzializzazione utilizzo in dati 
			//in source
			initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
			initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;
		}
		
		//TODO: contract.sourceMealTicketDate
		if ( contract.sourceDate != null 
				&& contract.sourceDate.getYear() == yearMonth.getYear() 
				&& contract.sourceDate.getMonthOfYear() == yearMonth.getMonthOfYear() )	{
			
			cmr.buoniPastoDaInizializzazione = contract.sourceRemainingMealTicket;
		} else {
			cmr.buoniPastoDaInizializzazione = 0;
		}
		
		//Inizializzazione buoni pasto
		if (cmr.buoniPastoDaInizializzazione == 0 && recapPreviousMonth.isPresent()) {
			cmr.buoniPastoDalMesePrecedente = recapPreviousMonth.get().remainingMealTickets;
		}
		
		
		
		DateInterval validDataForPersonDay = buildIntervalForProgressive(yearMonth, 
				calcolaFinoA, contract);
		
		DateInterval validDataForCompensatoryRest = 
				buildIntervalForCompensatoryRest(yearMonth, contract);
		
		DateInterval validDataForMealTickets = buildIntervalForMealTicket(yearMonth, 
				calcolaFinoA, contract);
		

		cmr.wcontract = wrapperFactory.create(contract);
		cmr.person = contract.person;
		cmr.qualifica = cmr.person.qualification.qualification;
		
		cmr.initMonteOreAnnoCorrente = initMonteOreAnnoCorrente;
		cmr.initMonteOreAnnoPassato = initMonteOreAnnoPassato;
		
		
		//Per stampare a video il residuo da inizializzazione se riferito al mese
		if(contract.sourceDate != null && 
				contract.sourceDate.getMonthOfYear() == cmr.month && 
				contract.sourceDate.getYear() == cmr.year) {
			cmr.initResiduoAnnoCorrenteNelMese = contract.sourceRemainingMinutesCurrentYear;
		}
		
		//Inizializzazione residui
		//Gennaio
		if(cmr.month==1) {
			cmr.mesePrecedente = null;
			cmr.remainingMinutesLastYear = initMonteOreAnnoPassato;
			cmr.remainingMinutesCurrentYear = initMonteOreAnnoCorrente;
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(cmr.remainingMinutesLastYear < 0) {
				cmr.progressivoFinalePositivoMeseAux = cmr.progressivoFinalePositivoMeseAux 
						+ cmr.remainingMinutesLastYear;
				cmr.remainingMinutesLastYear = 0;
			}
		} else {
			cmr.mesePrecedente = recapPreviousMonth;
			cmr.remainingMinutesLastYear = initMonteOreAnnoPassato;
			cmr.remainingMinutesCurrentYear = initMonteOreAnnoCorrente;
			
			Parameter param = cmr.qualifica > 3 ? 
					Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49: 
						Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13;
			Integer monthExpiryRecoveryDay = confYearManager.getIntegerFieldValue(param,
					cmr.person.office, cmr.year);
			if(monthExpiryRecoveryDay != 0 && cmr.month > monthExpiryRecoveryDay) {
				cmr.possibileUtilizzareResiduoAnnoPrecedente = false;
				cmr.remainingMinutesLastYear = 0;
			}
		}
		

		
		setMealTicketsInformation(cmr, validDataForMealTickets);
		setPersonDayInformation(cmr, validDataForPersonDay);
		setPersonMonthInformation(cmr, wcontract, validDataForCompensatoryRest, otherAbsences);
		

		assegnaProgressivoFinaleNegativo(cmr);
		assegnaStraordinari(cmr);
		assegnaRiposiCompensativi(cmr);
		
		//All'anno corrente imputo sia ciò che ho imputato al residuo del mese precedente dell'anno corrente sia ciò che ho imputato al progressivo finale positivo del mese
		//perchè non ho interesse a visualizzarli separati nel template. 
		cmr.progressivoFinaleNegativoMeseImputatoAnnoCorrente = cmr.progressivoFinaleNegativoMeseImputatoAnnoCorrente + cmr.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		cmr.riposiCompensativiMinutiImputatoAnnoCorrente = cmr.riposiCompensativiMinutiImputatoAnnoCorrente + cmr.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		
		//Al monte ore dell'anno corrente aggiungo ciò che non ho utilizzato del progressivo finale positivo del mese
		cmr.remainingMinutesCurrentYear = cmr.remainingMinutesCurrentYear + cmr.progressivoFinalePositivoMeseAux;	
		
		return Optional.fromNullable( cmr );
	}

	/**
	 * Costruisce l'intervallo dei giorni da considerare per il calcolo dei progressivi.<br>
	 * 1) Parto dall'intero intervallo del mese.<br>
	 * 2) Nel caso di calcolo riepilogo mese attuale considero i giorni fino
	 *    a ieri. (se oggi è il primo giorno del mese ritorna l'intervallo vuoto)<br>
	 * 3) Riduco ulteriormente in base al parametro calcolaFinoA
	 *    e sull'intervallo del contratto nel database. 
	 * @param yearMonth
	 * @param calcolaFinoA
	 * @param contract
	 * @return
	 */
	private DateInterval buildIntervalForProgressive(YearMonth yearMonth, 
			LocalDate calcolaFinoA, Contract contract) {
		
		LocalDate firstDayOfRequestedMonth = 
				new LocalDate(yearMonth.getYear(),yearMonth.getMonthOfYear(),1);
		DateInterval requestInterval = 
				new DateInterval(firstDayOfRequestedMonth, calcolaFinoA);
		
		DateInterval contractDatabaseInterval = 
				wrapperFactory.create(contract).getContractDatabaseInterval();
		
		LocalDate today = LocalDate.now();
		
		//Parto da tutti i giorni del mese
		LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
		
		//Filtro se mese attuale
		if( DateUtility.isDateIntoInterval(today, monthInterval) ) {

			if ( today.getDayOfMonth() != 1 ) {
				
				//Se oggi non è il primo giorno del mese allora 
				//tutti i giorni del mese fino a ieri.
				
				monthEnd = today.minusDays(1);
				monthInterval = new DateInterval(monthBegin, monthEnd);
			} else {
				
				//Se oggi è il primo giorno del mese allora nessun giorno.
				monthInterval = null;
			}
		}

		//Filtro per dati nel database e estremi del contratto
		DateInterval validDataForPersonDay = null;
		if(monthInterval != null) {
			validDataForPersonDay = DateUtility
					.intervalIntersection(monthInterval, requestInterval);
			validDataForPersonDay = DateUtility
					.intervalIntersection(validDataForPersonDay, contractDatabaseInterval);
		}
		
		return validDataForPersonDay;
	}

	/**
	 * Costruisce l'intervallo dei giorni da considerare per il conteggio 
	 * dei riposi compensativi utilizzati.<br>
	 * 1) Parto dall'intero intervallo del mese.<br>
	 * 2) Nel caso di calcolo riepilogo mese attuale considero tutti i giorni 
	 *    del mese attuale e di quello successivo<br>
	 * 3) Riduco ulteriormente in base agli estremi del contratto nel database. 
	 * @param yearMonth
	 * @param contract
	 * @return
	 */
	private DateInterval buildIntervalForCompensatoryRest(YearMonth yearMonth, 
			Contract contract) {

		DateInterval contractDatabaseInterval = 
				wrapperFactory.create(contract).getContractDatabaseInterval();
		
		LocalDate today = LocalDate.now();
		
		//Parto da tutti i giorni del mese
		LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);

		//Nel caso del mese attuale considero anche il mese successivo
		if( DateUtility.isDateIntoInterval(today, monthInterval) ) {
			monthEnd = monthEnd.plusMonths(1).dayOfMonth().withMaximumValue();
			monthInterval = new DateInterval(monthBegin, monthEnd);
		}

		//Filtro per dati nel database e estremi del contratto
		DateInterval validDataForCompensatoryRest = null;
		validDataForCompensatoryRest = DateUtility
				.intervalIntersection(monthInterval, contractDatabaseInterval);
		return validDataForCompensatoryRest;
		
	}
	
	/**
	 * Costruisce l'intervallo dei giorni da considerare per il calcolo 
	 * buoni pasto utilizzati.<br>
	 * Se l'office della persona non ha una data di inizio utilizzo buoni pasto
	 * ritorna un intervallo vuoto (null)<br>
	 * 1) Parto dall'intero intervallo del mese.<br>
	 * 2) Nel caso di calcolo riepilogo mese attuale considero i giorni fino
	 *    a oggi. <br>
	 * 3) Riduco ulteriormente in base al parametro calcolaFinoA,
	 *    in base all'inizializzazione del contratto per buoni pasto 
	 *    ed alla data di inizio utilizzo buoni pasto dell'office. 
	 * @param yearMonth
	 * @param calcolaFinoA
	 * @param contract
	 * @return
	 */
	private DateInterval buildIntervalForMealTicket(YearMonth yearMonth, 
			LocalDate calcolaFinoA,	Contract contract) {

		LocalDate firstDayOfRequestedMonth = 
				new LocalDate(yearMonth.getYear(),yearMonth.getMonthOfYear(),1);
		DateInterval requestInterval = new DateInterval(firstDayOfRequestedMonth, calcolaFinoA);
		
		Optional<LocalDate> dateStartMealTicketInOffice = 
				confGeneralManager.getLocalDateFieldValue(
						Parameter.DATE_START_MEAL_TICKET, contract.person.office); 
		
		if (!dateStartMealTicketInOffice.isPresent()) {
			return null;
		}
		
		LocalDate today = LocalDate.now();
		
		//Parto da tutti i giorni del mese
		LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
				
		//Nel caso del calcolo del mese attuale considero dall'inizio
		//del mese fino a oggi.
		if( DateUtility.isDateIntoInterval(today, monthInterval) ) { 
			monthEnd = today;
			monthInterval = new DateInterval(monthBegin, monthEnd);
		}

		//Filtro per dati nel database, estremi del contratto, inizio utilizzo buoni pasto
		DateInterval contractIntervalForMealTicket = 
				wrapperFactory.create(contract).getContractDatabaseIntervalForMealTicket();
		DateInterval mealTicketIntervalInOffice = 
				new DateInterval(dateStartMealTicketInOffice.orNull(), null);
		
		DateInterval validDataForMealTickets = null;
		if(monthInterval != null)	{
			validDataForMealTickets = DateUtility
					.intervalIntersection(monthInterval, requestInterval);
			validDataForMealTickets = DateUtility
					.intervalIntersection(validDataForMealTickets, contractIntervalForMealTicket);
			validDataForMealTickets = DateUtility
					.intervalIntersection(validDataForMealTickets, mealTicketIntervalInOffice);
		}

		return validDataForMealTickets;

	}

	/**
	 * Assegna i seguenti campi del riepilogo mensile: <br>
	 * cmr.progressivoFinaleMese <br>
	 * cmr.progressivoFinalePositivoMeseAux <br>
	 * cmr.progressivoFinaleNegativoMese <br>
	 *  
	 * @param cmr
	 * @param validDataForPersonDay
	 */
	private void setPersonDayInformation(ContractMonthRecap cmr, 
			DateInterval validDataForPersonDay) {
		
		if(validDataForPersonDay!=null) {
			
			// TODO: implementare un metodo che no fa fetch di stampings... in 
			// questo caso non servono.
			
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriodDesc(
					cmr.person, validDataForPersonDay.getBegin(), 
					Optional.fromNullable(validDataForPersonDay.getEnd()));

			//progressivo finale fine mese
			for (PersonDay pd : pdList) {
				if(pd != null) {
					cmr.progressivoFinaleMese = pd.progressive;
					break;
				}
				else{
					//
				}
			}

			//progressivo finale positivo e negativo mese
			for (PersonDay pd : pdList) {
				if (pd.difference >= 0) {
					cmr.progressivoFinalePositivoMeseAux += pd.difference;
				} else {
					cmr.progressivoFinaleNegativoMese += pd.difference;
				}
				cmr.oreLavorate += pd.timeAtWork;
			}
			cmr.progressivoFinaleNegativoMese = 
					cmr.progressivoFinaleNegativoMese * -1;

			cmr.progressivoFinalePositivoMese = 
					cmr.progressivoFinalePositivoMeseAux;
			
		}
	}

	/**
	 * Assegna i seguenti campi del riepilogo mensile: <br>
	 * cmr.buoniPastoUsatiNelMese <br>
	 * cmr.buoniPastoConsegnatiNelMese <br>
	 * cmr.remainingMealTickets <br>
	 * 
	 * @param cmr
	 * @param validDataForMealTickets l'intervallo all'interno del quale 
	 * ricercare i person day per buoni pasto utilizzati e buoni pasto consegnati.
	 */
	private void setMealTicketsInformation(ContractMonthRecap cmr, 
			DateInterval validDataForMealTickets) {
		
		if (validDataForMealTickets != null) {
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(cmr.person,
					validDataForMealTickets.getBegin(), 
					Optional.fromNullable(validDataForMealTickets.getEnd()));

			//buoni pasto utilizzati
			for (PersonDay pd : pdList) {
				if (pd != null && pd.isTicketAvailable) {
					cmr.buoniPastoUsatiNelMese++;
				}
			}
			
			//Numero ticket consegnati nel mese
			cmr.buoniPastoConsegnatiNelMese = 
					mealTicketDao.getMealTicketAssignedToPersonIntoInterval(
							cmr.contract, validDataForMealTickets).size();
		}
		
		//residuo
		cmr.remainingMealTickets = cmr.buoniPastoDalMesePrecedente 
				+ cmr.buoniPastoDaInizializzazione
				+ cmr.buoniPastoConsegnatiNelMese 
				- cmr.buoniPastoUsatiNelMese;
	}
	
	/**
	 * Assegna i seguenti campi del riepilogo mensile: <br>
	 * cmr.straordinariMinuti <br>
	 * cmr.straordinariMinutiS1Print <br>
	 * cmr.straordinariMinutiS2Print <br>
	 * cmr.straordinariMinutiS3Print <br>
	 * cmr.riposiCompensativiMinuti <br>
	 * cmr.recoveryDayUsed <br>
	 * 
	 * @param cmr
	 * @param wcontract
	 * @param validDataForCompensatoryRest l'intervallo all'interno del quale 
	 * ricercare i riposi compensativi
	 * @param otherAbsences le assenze inserire e non persistite (usato per le 
	 * simulazioni di inserimento assenze).
	 */
	private void setPersonMonthInformation(ContractMonthRecap cmr, 
			IWrapperContract wcontract,	DateInterval validDataForCompensatoryRest, 
			List<Absence> otherAbsences) {
		
		//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		if (wcontract.isLastInMonth(cmr.month, cmr.year)) {
			
			CompetenceCode s1 = competenceCodeManager.getCompetenceCode("S1");
			CompetenceCode s2 = competenceCodeManager.getCompetenceCode("S2");
			CompetenceCode s3 = competenceCodeManager.getCompetenceCode("S3");
			List<CompetenceCode> codes = Lists.newArrayList();
			codes.add(s1);
			codes.add(s2);
			codes.add(s3);
			
			cmr.straordinariMinuti = 0;
			cmr.straordinariMinutiS1Print = 0;
			cmr.straordinariMinutiS2Print = 0;
			cmr.straordinariMinutiS3Print = 0;
			
			List<Competence> competences = competenceDao
					.getCompetences(cmr.person, cmr.year, cmr.month, codes);
			
			for (Competence competence : competences) {
				
				if(competence.competenceCode.id.equals(s1.id)) {
					cmr.straordinariMinutiS1Print = (competence.valueApproved * 60);
				} else if(competence.competenceCode.id.equals(s2.id)) {
					cmr.straordinariMinutiS2Print = (competence.valueApproved * 60);
				} else if(competence.competenceCode.id.equals(s3.id)) {
					cmr.straordinariMinutiS3Print = (competence.valueApproved * 60);
				}
			}

			cmr.straordinariMinuti = cmr.straordinariMinutiS1Print 
					+ cmr.straordinariMinutiS2Print 
					+ cmr.straordinariMinutiS3Print;
		}
		
		if (validDataForCompensatoryRest != null) {
			
			LocalDate begin = validDataForCompensatoryRest.getBegin();
			LocalDate end = validDataForCompensatoryRest.getEnd();
			
			List<Absence> riposi = absenceDao.absenceInPeriod(cmr.person, begin, end, "91");
			
			cmr.riposiCompensativiMinuti = 0;
			cmr.recoveryDayUsed = 0;
			
			for(Absence riposo : otherAbsences) {
				if(DateUtility.isDateIntoInterval(riposo.date, validDataForCompensatoryRest)) {

					// TODO: rifattorizzare questa parte. Serve un metodo 
					// .getWorkingTimeTypeDay(date) in WrapperContract
					
					LocalDate date = riposo.date;
					for(ContractWorkingTimeType cwtt : 
						wcontract.getValue().contractWorkingTimeType ) {

						if(DateUtility.isDateIntoInterval(date, 
								wrapperFactory.create(cwtt).getDateInverval())) {

							WorkingTimeTypeDay wttd = cwtt.workingTimeType.workingTimeTypeDays
									.get(date.getDayOfWeek() - 1);

							Preconditions.checkState(wttd.dayOfWeek == date.getDayOfWeek());
							cmr.riposiCompensativiMinuti += wttd.workingTime;
							cmr.recoveryDayUsed++;
						}
					}
				} 
			}
			for (Absence abs : riposi){
				cmr.riposiCompensativiMinuti += wrapperFactory.create(abs.personDay)
						.getWorkingTimeTypeDay().get().workingTime;
				cmr.recoveryDayUsed++;
			}	
			
			cmr.riposiCompensativiMinutiPrint = cmr.riposiCompensativiMinuti;
		}
	}
	
	private void assegnaProgressivoFinaleNegativo(ContractMonthRecap monthRecap)
	{
		
		//quello che assegno al monte ore passato
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.remainingMinutesLastYear)
		{
			monthRecap.remainingMinutesLastYear = monthRecap.remainingMinutesLastYear - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.remainingMinutesLastYear;
			monthRecap.remainingMinutesLastYear = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.remainingMinutesCurrentYear)
		{
			monthRecap.remainingMinutesCurrentYear = monthRecap.remainingMinutesCurrentYear - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.remainingMinutesCurrentYear;
			monthRecap.remainingMinutesCurrentYear = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente;
		}
		
		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMeseAux = monthRecap.progressivoFinalePositivoMeseAux - monthRecap.progressivoFinaleNegativoMese;
		monthRecap.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = monthRecap.progressivoFinaleNegativoMese;
		return;
		
	}
	
	private void assegnaStraordinari(ContractMonthRecap monthRecap)
	{
		monthRecap.progressivoFinalePositivoMeseAux = monthRecap.progressivoFinalePositivoMeseAux - monthRecap.straordinariMinuti;
	}
	
	private void assegnaRiposiCompensativi(ContractMonthRecap monthRecap)
	{
		//quello che assegno al monte ore passato
		if(monthRecap.riposiCompensativiMinuti < monthRecap.remainingMinutesLastYear)
		{
			monthRecap.remainingMinutesLastYear = monthRecap.remainingMinutesLastYear - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.remainingMinutesLastYear;
			monthRecap.remainingMinutesLastYear = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(monthRecap.riposiCompensativiMinuti < monthRecap.remainingMinutesCurrentYear)
		{
			monthRecap.remainingMinutesCurrentYear = monthRecap.remainingMinutesCurrentYear - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.remainingMinutesCurrentYear;
			monthRecap.remainingMinutesCurrentYear = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente;
		}
		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMeseAux = monthRecap.progressivoFinalePositivoMeseAux - monthRecap.riposiCompensativiMinuti;
		monthRecap.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = monthRecap.riposiCompensativiMinuti;
	
	}	
}



	

