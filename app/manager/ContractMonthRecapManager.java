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
		boolean mealTicketToCompute = true;
		
		Optional<LocalDate> dateStartMealTicket = 
				confGeneralManager.getLocalDateFieldValue(
						Parameter.DATE_START_MEAL_TICKET, contract.person.office); 
				
		
		if(!dateStartMealTicket.isPresent() 
				|| dateStartMealTicket.get().isAfter(calcolaFinoA)) {
			mealTicketToCompute = false;
		}
		
		LocalDate firstDayInDatabase = new LocalDate(yearMonth.getYear(),yearMonth.getMonthOfYear(),1);
		DateInterval contractInterval = wrapperFactory.create(contract).getContractDateInterval();
		DateInterval requestIntervalForProgressive = new DateInterval(firstDayInDatabase, calcolaFinoA);
		DateInterval requestIntervalForMealTicket = new DateInterval(firstDayInDatabase, calcolaFinoA);
		DateInterval mealTicketInterval = new DateInterval(dateStartMealTicket.orNull(), calcolaFinoA);

		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;

		////////////////////////////////////////////////////////////////////////
		//	Recupero situazione iniziale del mese richiesto
		////////////////////////////////////////////////////////////////////////
		
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

		////////////////////////////////////////////////////////////////////////
		//	Utilizzo situazione iniziale del mese richiesto
		////////////////////////////////////////////////////////////////////////
		
		if( recapPreviousMonth.isPresent() ) {
			if ( recapPreviousMonth.get().month == 12 ) {
				initMonteOreAnnoPassato = 
						recapPreviousMonth.get().remainingMinutesCurrentYear 
						+ recapPreviousMonth.get().remainingMinutesLastYear;
			
			} else {
				initMonteOreAnnoCorrente = recapPreviousMonth.get().remainingMinutesCurrentYear; 
				initMonteOreAnnoPassato = recapPreviousMonth.get().remainingMinutesLastYear;
			}
		}
		if ( contract.sourceDate != null 
				&& contract.sourceDate.getYear() == yearMonth.getYear() 
				&& contract.sourceDate.getMonthOfYear() == yearMonth.getMonthOfYear() )	{
			initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
			initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;

			firstDayInDatabase = contract.sourceDate.plusDays(1);
			requestIntervalForProgressive = new DateInterval(firstDayInDatabase, calcolaFinoA);

			
		}
		//TODO: contract.sourceMealTicketDate
		if ( contract.sourceDate != null 
				&& contract.sourceDate.getYear() == yearMonth.getYear() 
				&& contract.sourceDate.getMonthOfYear() == yearMonth.getMonthOfYear() )	{
			
			firstDayInDatabase = contract.sourceDate.plusDays(1);
			requestIntervalForMealTicket= new DateInterval(firstDayInDatabase, calcolaFinoA);
			cmr.buoniPastoDaInizializzazione = contract.sourceRemainingMealTicket;
		}
		
		LocalDate today = LocalDate.now();

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//	Intervallo per progressivi
		//////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 1) Tutti i giorni del mese

		LocalDate monthBeginForPersonDay = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEndForPersonDay = monthBeginForPersonDay.dayOfMonth().withMaximumValue();
		DateInterval monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);

		// 2) Nel caso del calcolo del mese attuale

		if( DateUtility.isDateIntoInterval(today, monthIntervalForPersonDay) )
		{
			// 2.1) Se oggi non è il primo gPersonResidualYearRecap csap = new PersonResidualYearRecap();iorno del mese allora tutti i giorni del mese fino a ieri.

			if ( today.getDayOfMonth() != 1 )
			{
				monthEndForPersonDay = today.minusDays(1);
				monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);
			}

			// 2.2) Se oggi è il primo giorno del mese allora null.

			else
			{
				monthIntervalForPersonDay = null;
			}
		}

		// 3) Filtro per dati nel database e estremi del contratto

		DateInterval validDataForPersonDay = null;
		if(monthIntervalForPersonDay != null)
		{
			validDataForPersonDay = DateUtility.intervalIntersection(monthIntervalForPersonDay, requestIntervalForProgressive);
			validDataForPersonDay = DateUtility.intervalIntersection(validDataForPersonDay, contractInterval);
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//	Intervallo per riposi compensativi
		////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 1) Tutti i giorni del mese

		LocalDate monthBeginForCompensatoryRest = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEndForCompensatoryRest = monthBeginForCompensatoryRest.dayOfMonth().withMaximumValue();
		DateInterval monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);

		// 2) Nel caso del mese attuale considero anche il mese successivo

		if( DateUtility.isDateIntoInterval(today, monthIntervalForCompensatoryRest) ) 
		{
			monthEndForCompensatoryRest = monthEndForCompensatoryRest.plusMonths(1).dayOfMonth().withMaximumValue();
			monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);
		}

		// 3) Filtro per dati nel database e estremi del contratto

		DateInterval validDataForCompensatoryRest = null;

		validDataForCompensatoryRest = DateUtility.intervalIntersection(monthIntervalForCompensatoryRest, contractInterval);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//	Intervallo per mealTickets
		//////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 1) Tutti i giorni del mese

		LocalDate monthBeginForMealTickets = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEndForMealTickets = monthBeginForMealTickets.dayOfMonth().withMaximumValue();
		DateInterval monthIntervalForMealTickets = new DateInterval(monthBeginForMealTickets, monthEndForMealTickets);

		// 2) Nel caso del calcolo del mese attuale considero dall'inizio
		// del mese fino a oggi.
		if( DateUtility.isDateIntoInterval(today, monthIntervalForMealTickets) ) 
		{ 
			monthEndForMealTickets = today;
			monthIntervalForMealTickets = new DateInterval(monthBeginForMealTickets, monthEndForMealTickets);
		}

		// 3) Filtro per dati nel database, estremi del contratto, inizio utilizzo buoni pasto
		DateInterval validDataForMealTickets = null;
		if(monthIntervalForMealTickets != null)
		{
			validDataForMealTickets = DateUtility.intervalIntersection(monthIntervalForMealTickets, requestIntervalForMealTicket);
			validDataForMealTickets = DateUtility.intervalIntersection(validDataForMealTickets, contractInterval);
			validDataForMealTickets = DateUtility.intervalIntersection(validDataForMealTickets, mealTicketInterval);
		}
		
		if( !mealTicketToCompute ) {
			validDataForMealTickets = null;
		}
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//		Intervallo per mealTickets
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		
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
		
		//Inizializzazione buoni pasto
		if (cmr.buoniPastoDaInizializzazione == 0 && recapPreviousMonth.isPresent()) {
			cmr.buoniPastoDalMesePrecedente = recapPreviousMonth.get().remainingMealTickets;
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
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private void setPersonDayInformation(ContractMonthRecap monthRecap, DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null) {
			
			// TODO: implementare un metodo che no fa fetch di stampings... in 
			// questo caso non servono.
			
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriodDesc(
					monthRecap.person, validDataForPersonDay.getBegin(), 
					Optional.fromNullable(validDataForPersonDay.getEnd()));

			//progressivo finale fine mese
			for(PersonDay pd : pdList){
				if(pd != null){
					monthRecap.progressivoFinaleMese = pd.progressive;
					break;
				}
				else{
					//
				}
			}

			//progressivo finale positivo e negativo mese
			for(PersonDay pd : pdList)
			{
				if(pd.difference>=0)
					monthRecap.progressivoFinalePositivoMeseAux += pd.difference;
				else
					monthRecap.progressivoFinaleNegativoMese += pd.difference;
				
				monthRecap.oreLavorate += pd.timeAtWork;
			}
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese*-1;

			monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMeseAux;
			
		}
	}
	
	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private void setMealTicketsInformation(ContractMonthRecap monthRecap, DateInterval validDataForMealTickets)
	{
		
		if(validDataForMealTickets!=null)
		{
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(monthRecap.person,
					validDataForMealTickets.getBegin(), 
					Optional.fromNullable(validDataForMealTickets.getEnd()));

			//buoni pasto utilizzati
			for(PersonDay pd : pdList){
				if(pd != null && pd.isTicketAvailable){
					monthRecap.buoniPastoUsatiNelMese++;
				}
			}
			
			//Numero ticket consegnati nel mese
			monthRecap.buoniPastoConsegnatiNelMese = 
					mealTicketDao.getMealTicketAssignedToPersonIntoInterval(
							monthRecap.contract, validDataForMealTickets).size();
		}
		
		//residuo
		monthRecap.remainingMealTickets = monthRecap.buoniPastoDalMesePrecedente 
				+ monthRecap.buoniPastoDaInizializzazione
				+ monthRecap.buoniPastoConsegnatiNelMese 
				- monthRecap.buoniPastoUsatiNelMese;
	}
	
	/**
	 * 
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	private void setPersonMonthInformation(ContractMonthRecap cmr, 
			IWrapperContract wcontract,	DateInterval validDataForCompensatoryRest, List<Absence> otherAbsences) {
		
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



	

