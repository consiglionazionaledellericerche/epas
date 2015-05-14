package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Absence;
import models.AbsenceType;
import models.Competence;
import models.CompetenceCode;
import models.ConfYear;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractYearRecap;
import models.Person;
import models.PersonDay;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.sun.org.apache.bcel.internal.generic.NEW;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ConfYearDao;
import dao.ContractDao;
import dao.MealTicketDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.WorkingTimeTypeDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import exceptions.EpasExceptionNoSourceData;

/**
 * TODO: una volta mergiato il branch fare un unico manager che gestisca sia 
 * year che month recap.
 * 
 * @author alessandro
 *
 */
public class ContractMonthRecapManager {

	@Inject
	public VacationsRecapFactory vacationsFactory;
	@Inject 
	public MealTicketDao mealTicketDao;
	@Inject
	public PersonDayDao personDayDao;
	@Inject
	public CompetenceDao competenceDao;
	@Inject
	public AbsenceDao absenceDao;
	@Inject
	public IWrapperFactory wrapperFactory;
	@Inject
	public ContractManager contractManager;
	
	/**
	 * Costruisce i riepiloghi mensili per il contratto fornito.
	 * Se yearMonthFrom è present costruisce i riepiloghi a partire da quel mese.
	 * Se non vi sono i riepiloghi mensili necessari precedenti a yearMonthFrom
	 * vengono costruiti.
	 * 
	 * @param contract
	 * @param yearMonthFrom
	 * @throws EpasExceptionNoSourceData
	 */
	public void populateContractMonthRecap(Contract contract, 
			Optional<YearMonth> yearMonthFrom) throws EpasExceptionNoSourceData  {
		
		//Controllo se ho sufficienti dati
		
		String dateInitUse = ConfGeneralManager.getFieldValue(Parameter.INIT_USE_PROGRAM, contract.person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		if(contract.sourceDate!=null)
			initUse = contract.sourceDate;
		DateInterval personDatabaseInterval = new DateInterval(initUse, new LocalDate());
		DateInterval contractInterval = contract.getContractDateInterval();

		DateInterval personContractDatabaseInterval = DateUtility.intervalIntersection(contractInterval, personDatabaseInterval);
		
		//Se intersezione fra contratto e dati utili database vuota non costruisco alcun contractMonthRecap
		if( personContractDatabaseInterval == null)
			return;

		YearMonth yearMonthToCompute = new YearMonth( personContractDatabaseInterval.getBegin() );
		
		if(yearMonthFrom.isPresent() && yearMonthFrom.get().isAfter(yearMonthToCompute)) {
			yearMonthToCompute = yearMonthFrom.get();
		}

		//Tentativo da sourceDate
		yearMonthToCompute = populateContractMonthFromSource(contract, yearMonthToCompute);
		
		YearMonth lastYearMonthToCompute = new YearMonth(LocalDate.now());
		
		if ( lastYearMonthToCompute.isAfter(new YearMonth(contractInterval.getEnd())) )
			lastYearMonthToCompute = new YearMonth(contractInterval.getEnd().getYear());
		
		while ( ! yearMonthToCompute.isAfter(lastYearMonthToCompute) ) {
			
			ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);
			
			//FERIE E PERMESSI
			LocalDate lastDayInYearMonth = new LocalDate(yearMonthToCompute.getYear(), 
					yearMonthToCompute.getMonthOfYear(), 1).dayOfMonth().withMaximumValue();
			
			VacationsRecap vacationRecap = vacationsFactory
					.create(yearMonthToCompute.getYear(), contract, lastDayInYearMonth, true);
			
			cmr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cmr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cmr.permissionUsed = vacationRecap.permissionUsed.size();
			
			//RESIDUI
			populateResidualModule(cmr, yearMonthToCompute, lastDayInYearMonth);
						
			cmr.save();
			contract.contractMonthRecaps.add(cmr);
			contract.save();
			
			yearMonthToCompute = yearMonthToCompute.plusMonths(1);
		}
		
	}

	/**
	 * Costruisce i riepiloghi mensili inerenti la persona a partire da yeraMonthFrom.
	 * 
	 * @param person
	 * @param yearMonthFrom
	 * @throws EpasExceptionNoSourceData
	 */
	public void populateContractMonthRecapByPerson( Person person, 
			Optional<YearMonth> yearMonthFrom) throws EpasExceptionNoSourceData {

		for( Contract contract : person.contracts ){
			
			DateInterval contractDateInterval = contract.getContractDateInterval();
			
			//Se yearMonthFrom non è successivo alla fine del contratto...
			if ( yearMonthFrom.isPresent() && ! yearMonthFrom.get()
					.isAfter( new YearMonth(contractDateInterval.getEnd() )) ) {
				
				populateContractMonthRecap(contract, yearMonthFrom);
				
			} else {
				populateContractMonthRecap(contract, Optional.<YearMonth>absent());
			}
				
		}
	}
	
	/**
	 * Costruzione di un ContractMonthRecap pulito. Il preesistente se presente
	 * viene distrutto. Alternativa pulirlo (ma sono tanti campi!) 
	 * 
	 * @param contract
	 * @param yearMonth
	 * @return
	 */
	private ContractMonthRecap buildContractMonthRecap(Contract contract, YearMonth yearMonth ) {
		
		Optional<ContractMonthRecap> cmrOld = contractManager.getContractMonthRecap(contract, yearMonth);

		if ( cmrOld.isPresent() ) {
			cmrOld.get().delete();
			contract.contractMonthRecaps.remove(cmrOld.get());
			contract.save();
		}
		
		ContractMonthRecap cmr = new ContractMonthRecap();
		cmr.year = yearMonth.getYear();
		cmr.month = yearMonth.getMonthOfYear();
		cmr.contract = contract;
		
		return cmr;
	}
	
	/**
	 * Costruisce il contractMonthRecap da contract.SourceDate.
	 * 1) Se sourceDate è l'ultimo giorno del mese costruisce il riepilogo 
	 *    copiando le informazioni in esso contenute.
	 * 2) Se sourceDate non è l'ultimo giorno del mese e si riferisce al mese corrente
	 *    allora non si deve creare alcun riepilogo.  //FIXME andrà calcolato è ciò che serve di più!!!! 
	 * 3) Se sourceDate non è l'ultimo giorno del mese e si riferisce ad un mese passato
	 *    costruisce il riepilogo andando a combinare le informazioni presenti in sourceContract
	 *    e nel database (a partire dal giorno successivo a sourceDate).
	 *    
	 * @return YearMonth di cui si deve costruire il prossimo contractMonthRecap
	 * @throws EpasExceptionNoSourceData 
	 */
	private YearMonth populateContractMonthFromSource(Contract contract, YearMonth yearMonthToCompute) throws EpasExceptionNoSourceData
	{
		if(contract.sourceDate == null)
			return yearMonthToCompute;
		
		// Mese da costruire con sourceDate
		YearMonth yearMonthToComputeFromSource = new YearMonth(contract.sourceDate);

		//Mese da costruire di competenza si sourceDate?
		if( yearMonthToCompute.isAfter(yearMonthToComputeFromSource ) ) {
			return yearMonthToCompute;
		}

		//Caso semplice ultimo giorno del mese
		LocalDate lastDayInSourceMonth = contract.sourceDate.dayOfMonth().withMaximumValue();
		if(lastDayInSourceMonth.isEqual(contract.sourceDate))
		{
			ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);

			cmr.remainingMinutesCurrentYear = contract.sourceRemainingMinutesCurrentYear;
			cmr.remainingMinutesLastYear = contract.sourceRemainingMinutesLastYear;
			cmr.vacationLastYearUsed = contract.sourceVacationLastYearUsed;
			cmr.vacationCurrentYearUsed = contract.sourceVacationCurrentYearUsed;
			cmr.recoveryDayUsed = contract.sourceRecoveryDayUsed;
			cmr.permissionUsed = contract.sourcePermissionUsed;
			cmr.save();
			contract.contractMonthRecaps.add(cmr);
			contract.save();
			return yearMonthToCompute.plusMonths(1);
		}

		//Nel caso in cui non sia l'ultimo giorno del mese e source cade nel mese_anno attuale 
		
		ContractMonthRecap cmr = buildContractMonthRecap(contract, yearMonthToCompute);
		
		//Caso complesso, TODO vedere (dopo che ci sono i test) se creando il VacationRecap si ottengono le stesse informazioni
		AbsenceType ab31 = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).orNull();
		AbsenceType ab32 = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()).orNull();
		AbsenceType ab37 = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()).orNull(); 
		AbsenceType ab94 = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).orNull(); 
				
		DateInterval monthInterSource = new DateInterval(contract.sourceDate.plusDays(1), lastDayInSourceMonth);
		List<Absence> abs32 = absenceDao.getAbsenceDays(monthInterSource, contract, ab32);
		List<Absence> abs31 = absenceDao.getAbsenceDays(monthInterSource, contract, ab31);
		List<Absence> abs37 = absenceDao.getAbsenceDays(monthInterSource, contract, ab37);
		List<Absence> abs94 = absenceDao.getAbsenceDays(monthInterSource, contract, ab94);
				
		cmr.vacationLastYearUsed = contract.sourceVacationLastYearUsed + abs31.size() + abs37.size();
		cmr.vacationCurrentYearUsed = contract.sourceVacationCurrentYearUsed + abs32.size();
		cmr.permissionUsed = contract.sourcePermissionUsed + abs94.size();
		
		contract.contractMonthRecaps.add(cmr);
		cmr.save();
		
		// Informazioni relative ai residui		
				 
		populateResidualModule(cmr, yearMonthToCompute, new LocalDate().minusDays(1));
		
		cmr.save();
		
		contract.save();
		return yearMonthToCompute.plusMonths(1);
		
	}
	

	//private void buildPartialMonthRecap(Contract contract, YearMonth yearMonth) {
		// Il parametro yearMonth potrebbe essere tolto in quanto è il mese attuale
		// non ancora concluso.
		// Da implementare
	//}

	/**
	 * @throws EpasExceptionNoSourceData 
	 * 
	 */
	private ContractMonthRecap populateResidualModule(ContractMonthRecap cmr, YearMonth yearMonth, LocalDate calcolaFinoA) throws EpasExceptionNoSourceData {

		IWrapperContract wcontract = wrapperFactory.create(cmr.contract);
		Contract contract = cmr.contract;
		boolean mealTicketToCompute = true;
		
		Optional<LocalDate> dateStartMealTicket = mealTicketDao.getMealTicketStartDate(contract.person.office);
		if(!dateStartMealTicket.isPresent() || dateStartMealTicket.get().isAfter(calcolaFinoA)) {
			mealTicketToCompute = false;
		}
		
		LocalDate firstDayInDatabase = new LocalDate(yearMonth.getYear(),yearMonth.getMonthOfYear(),1);
		DateInterval contractInterval = contract.getContractDateInterval();
		DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
		DateInterval mealTicketInterval = new DateInterval(dateStartMealTicket.orNull(), calcolaFinoA);

		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;
		int initMealTicket = 0;

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//	Recupero situazione iniziale del mese richiesto
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		Optional<ContractMonthRecap> recapPreviousMonth = Optional.absent();
		YearMonth firstContractMonthRecap = new YearMonth(contract.beginContract);
		if(contract.sourceDate != null && new YearMonth(contract.sourceDate).isAfter(firstContractMonthRecap)) {
			firstContractMonthRecap = new YearMonth(contract.sourceDate);
			
		}
		if ( yearMonth.isAfter(firstContractMonthRecap) ) {
			
			recapPreviousMonth = contractManager.getContractMonthRecap(contract, yearMonth.minusMonths(1));
			
			if ( ! recapPreviousMonth.isPresent() ) {
				populateContractMonthRecap(contract, Optional.<YearMonth>absent());
			}
		}
		
		if( recapPreviousMonth.isPresent() )	
		{
			if ( recapPreviousMonth.get().month == 12 ) {
				initMonteOreAnnoPassato = recapPreviousMonth.get().remainingMinutesCurrentYear 
						+ recapPreviousMonth.get().remainingMinutesLastYear;
			
			} else {
				initMonteOreAnnoCorrente = recapPreviousMonth.get().remainingMinutesCurrentYear; 
				initMonteOreAnnoPassato = recapPreviousMonth.get().remainingMinutesLastYear;
			}
			initMealTicket = recapPreviousMonth.get().remainingMealTickets;
		}
		if ( contract.sourceDate != null 
				&& contract.sourceDate.getYear() == yearMonth.getYear() 
				&& contract.sourceDate.getMonthOfYear() == yearMonth.getMonthOfYear() )
		{
			initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
			initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;

			firstDayInDatabase = contract.sourceDate.plusDays(1);
			requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
			
			//TODO initMealTickets da source contract
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
			validDataForPersonDay = DateUtility.intervalIntersection(monthIntervalForPersonDay, requestInterval);
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

		// 2) Nel caso del calcolo del mese attuale

		if( DateUtility.isDateIntoInterval(today, monthIntervalForMealTickets) )
		{
			// 2.1) Se oggi non è il primo giorno del mese allora tutti i giorni del mese fino a ieri.
			if ( today.getDayOfMonth() != 1 )
			{
				monthEndForMealTickets = today;
				monthIntervalForMealTickets = new DateInterval(monthBeginForMealTickets, monthEndForMealTickets);
			}

			// 2.2) Se oggi è il primo giorno del mese allora null.
			else
			{
				monthIntervalForMealTickets = null;
			}
		}

		// 3) Filtro per dati nel database, estremi del contratto, inizio utilizzo buoni pasto
		DateInterval validDataForMealTickets = null;
		if(monthIntervalForMealTickets != null)
		{
			validDataForMealTickets = DateUtility.intervalIntersection(monthIntervalForMealTickets, requestInterval);
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
		
		setContractDescription(cmr);
		
		//Inizializzazione residui
		//Gennaio
		ConfYear confYear = null;
		Optional<ConfYear> conf = null;
		String description = cmr.qualifica > 3 ? 
				Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49.description : 
					Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13.description;
		conf = ConfYearDao.getByFieldName(description, cmr.year, cmr.person.office);
		
		if(conf.isPresent()){
			confYear = conf.get();
		}
		else{
			confYear = ConfYearDao.getByFieldName(
					description, cmr.year-1, cmr.person.office).get();
		}
		if(cmr.month==1)
		{
			cmr.mesePrecedente = null;
			cmr.remainingMinutesLastYear = initMonteOreAnnoPassato;
			cmr.remainingMinutesCurrentYear = initMonteOreAnnoCorrente;
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(cmr.remainingMinutesLastYear<0)
			{
				cmr.progressivoFinalePositivoMese = cmr.progressivoFinalePositivoMese + cmr.remainingMinutesLastYear;
				cmr.remainingMinutesLastYear = 0;
			}
		}
		else
		{
			cmr.mesePrecedente = recapPreviousMonth;
			cmr.remainingMinutesLastYear = initMonteOreAnnoPassato;
			cmr.remainingMinutesCurrentYear = initMonteOreAnnoCorrente;
			
			if(new Integer(confYear.fieldValue) != 0 && cmr.month > new Integer(confYear.fieldValue))
			{
				cmr.possibileUtilizzareResiduoAnnoPrecedente = false;
				cmr.remainingMinutesLastYear = 0;
			}
		}
		
		//Inizializzazione buoni pasto
		if (recapPreviousMonth.isPresent())
		{
			cmr.buoniPastoDalMesePrecedente = recapPreviousMonth.get().remainingMealTickets;
		}
		
		setMealTicketsInformation(cmr, validDataForMealTickets);
		
		setPersonDayInformation(cmr, validDataForPersonDay);
		setPersonMonthInformation(cmr, validDataForCompensatoryRest, wcontract);
		

		assegnaProgressivoFinaleNegativo(cmr);
		assegnaStraordinari(cmr);
		assegnaRiposiCompensativi(cmr);
		
		//All'anno corrente imputo sia ciò che ho imputato al residuo del mese precedente dell'anno corrente sia ciò che ho imputato al progressivo finale positivo del mese
		//perchè non ho interesse a visualizzarli separati nel template. 
		cmr.progressivoFinaleNegativoMeseImputatoAnnoCorrente = cmr.progressivoFinaleNegativoMeseImputatoAnnoCorrente + cmr.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		cmr.riposiCompensativiMinutiImputatoAnnoCorrente = cmr.riposiCompensativiMinutiImputatoAnnoCorrente + cmr.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		
		//Al monte ore dell'anno corrente aggiungo ciò che non ho utilizzato del progressivo finale positivo del mese
		cmr.remainingMinutesCurrentYear = cmr.remainingMinutesCurrentYear + cmr.progressivoFinalePositivoMese;	
		
		return null;
	}
	
	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private void setPersonDayInformation(ContractMonthRecap monthRecap, DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null)
		{
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriodDesc(monthRecap.person,
					validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd(), true);

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
					monthRecap.progressivoFinalePositivoMese += pd.difference;
				else
					monthRecap.progressivoFinaleNegativoMese += pd.difference;
				
				monthRecap.oreLavorate += pd.timeAtWork;
			}
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese*-1;

			monthRecap.progressivoFinalePositivoMesePrint = monthRecap.progressivoFinalePositivoMese;
			
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
					validDataForMealTickets.getBegin(), Optional.fromNullable(validDataForMealTickets.getEnd()), true);

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
			
			//residuo
			monthRecap.remainingMealTickets = monthRecap.buoniPastoDalMesePrecedente 
					+ monthRecap.buoniPastoConsegnatiNelMese - monthRecap.buoniPastoUsatiNelMese;
						
		}
	}
	
	/**
	 * 
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	private void setPersonMonthInformation(ContractMonthRecap monthRecap, DateInterval validDataForCompensatoryRest, IWrapperContract wcontract)
	{
		CompetenceCode s1 = CompetenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode s2 = CompetenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode s3 = CompetenceCodeDao.getCompetenceCodeByCode("S3");
		
		if(wcontract.isLastInMonth(monthRecap.month, monthRecap.year))	//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		{
			//straordinari s1
			Optional<Competence> competenceS1 = competenceDao.getCompetence(monthRecap.person, monthRecap.year, monthRecap.month, s1);

			if(competenceS1.isPresent())
				monthRecap.straordinariMinutiS1Print = monthRecap.straordinariMinutiS1Print + (competenceS1.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS1Print = 0;
			//straordinari s2
			Optional<Competence> competenceS2 = competenceDao.getCompetence(monthRecap.person, monthRecap.year, monthRecap.month, s2);
			

			if(competenceS2.isPresent())
				monthRecap.straordinariMinutiS2Print = monthRecap.straordinariMinutiS2Print + (competenceS2.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS2Print = 0;
			//straordinari s3
			Optional<Competence> competenceS3 = competenceDao.getCompetence(monthRecap.person, monthRecap.year, monthRecap.month, s3);
			if(competenceS3.isPresent())
				monthRecap.straordinariMinutiS3Print = monthRecap.straordinariMinutiS3Print + (competenceS3.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS3Print = 0;


			monthRecap.straordinariMinuti = monthRecap.straordinariMinutiS1Print + monthRecap.straordinariMinutiS2Print + monthRecap.straordinariMinutiS3Print;
		}
		
		if(validDataForCompensatoryRest!=null)
		{
			List<Absence> riposiCompensativi = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(monthRecap.person), Optional.fromNullable("91"), 
					validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd(), 
					Optional.<JustifiedTimeAtWork>absent(), false, false);
			monthRecap.riposiCompensativiMinuti = 0;
			monthRecap.recoveryDayUsed = 0;
			for(Absence abs : riposiCompensativi){
				monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti + 
						WorkingTimeTypeDayDao.getWorkingTimeTypeDay(wcontract.getValue().person, abs.personDay.date).workingTime;	//FIXME potrebbe essere null
				monthRecap.recoveryDayUsed++;
			}
			monthRecap.riposiCompensativiMinutiPrint = monthRecap.riposiCompensativiMinuti;
			
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
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.progressivoFinaleNegativoMese;
		monthRecap.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = monthRecap.progressivoFinaleNegativoMese;
		return;
		
	}
	
	private void assegnaStraordinari(ContractMonthRecap monthRecap)
	{
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.straordinariMinuti;
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
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.riposiCompensativiMinuti;
		monthRecap.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = monthRecap.riposiCompensativiMinuti;
	
	}	
	
	/**
	 * Costruisce una stringa di descrizione per il contratto utilizzata in stampings.html e personStampings.html
	 */
	private void setContractDescription(ContractMonthRecap monthRecap)
	{
		LocalDate beginMonth = new LocalDate(monthRecap.year, monthRecap.month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(beginMonth, endMonth);	
		LocalDate endContract = monthRecap.contract.expireContract;
		if(monthRecap.contract.endContract!=null)
			endContract = monthRecap.contract.endContract;
		
		if(DateUtility.isDateIntoInterval(endContract, monthInterval))
			monthRecap.contractDescription = "(contratto scaduto in data " + endContract+")";
		else
			monthRecap.contractDescription = "";
	}
	
	
}



	

