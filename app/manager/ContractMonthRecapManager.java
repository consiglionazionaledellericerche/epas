package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Absence;
import models.AbsenceType;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
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

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

/**
 * TODO: una volta mergiato il branch fare un unico manager che gestisca sia 
 * year che month recap.
 * 
 * @author alessandro
 *
 */
public class ContractMonthRecapManager {

	@Inject
	private VacationsRecapFactory vacationsFactory;
	@Inject 
	private ConfGeneralManager confGeneralManager;
	@Inject
	private ConfYearManager confYearManager;
	@Inject 
	private MealTicketDao mealTicketDao;
	@Inject
	private PersonDayDao personDayDao;
	@Inject
	private CompetenceDao competenceDao;
	@Inject
	private AbsenceDao absenceDao;
	@Inject
	private IWrapperFactory wrapperFactory;
	@Inject
	private AbsenceTypeDao absenceTypeDao;
	@Inject
	private CompetenceCodeDao competenceCodeDao;
		
	
	private final static Logger log = LoggerFactory.getLogger(ContractMonthRecapManager.class);
	
	
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
			Contract contract, LocalDate date) {
		
		ContractMonthRecap cmr = new ContractMonthRecap();
		cmr.year = date.getYear();
		cmr.month = date.getMonthOfYear();
		cmr.contract = contract;
		
		Optional<ContractMonthRecap> recap = 
				populateResidualModule(cmr, new YearMonth(date), date);
		
		if( recap.isPresent() ) {
			return recap.get().remainingMinutesCurrentYear 
					+ recap.get().remainingMinutesLastYear;
		}
		return 0;
	}
	
	
	
	/**
	 * Costruisce i riepiloghi mensili per il contratto fornito.
	 * Se yearMonthFrom è present costruisce i riepiloghi a partire da quel mese.
	 * Se non vi sono i riepiloghi mensili necessari precedenti a yearMonthFrom
	 * vengono costruiti.
	 * 
	 * @param contract
	 * @param yearMonthFrom
	 */
	public void populateContractMonthRecap(Contract contract, 
			Optional<YearMonth> yearMonthFrom) {

		YearMonth yearMonthToCompute = wrapperFactory.create(contract).getFirstMonthToRecap();
		
		if(yearMonthFrom.isPresent() && yearMonthFrom.get().isAfter(yearMonthToCompute)) {
			yearMonthToCompute = yearMonthFrom.get();
		}
		
		//Tentativo da sourceDate
		yearMonthToCompute = populateContractMonthFromSource(contract, yearMonthToCompute);
		
		YearMonth lastMonthToCompute = wrapperFactory.create(contract).getLastMonthToRecap();
		
		ContractMonthRecap cmr = null;
		
		while ( !yearMonthToCompute.isAfter(lastMonthToCompute) ) {
			
			cmr = buildContractMonthRecap(contract, yearMonthToCompute);

			// (1) FERIE E PERMESSI 
			
			// TODO: per il calcolo delle ferie e permessi ho bisogno solo del
			// riepilogo di dicembre. Una ottimizzazione è calcolare questi campi
			// solo nel caso di dicembre. Però i dati dei mesi intermedi potrebbero 
			// essere usati per report. Decidere. 
			
			LocalDate lastDayInYearMonth = new LocalDate(yearMonthToCompute.getYear(), 
					yearMonthToCompute.getMonthOfYear(), 1).dayOfMonth().withMaximumValue();

			Optional<VacationsRecap> vacationRecap = vacationsFactory
					.create(yearMonthToCompute.getYear(), contract, lastDayInYearMonth, true);
			
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
			
			cmr.vacationLastYearUsed = vacationRecap.get().vacationDaysLastYearUsed.size();
			cmr.vacationCurrentYearUsed = vacationRecap.get().vacationDaysCurrentYearUsed.size();
			cmr.permissionUsed = vacationRecap.get().permissionUsed.size();
			
			// (2) RESIDUI
			Optional<ContractMonthRecap> recap = 
					populateResidualModule(cmr, yearMonthToCompute, lastDayInYearMonth);

			if( !recap.isPresent() ) {
				return;
			}
			
			recap.get().save();
			contract.contractMonthRecaps.add(recap.get());
			contract.save();
			
			yearMonthToCompute = yearMonthToCompute.plusMonths(1);
		}
	}

	/**
	 * Costruisce i riepiloghi mensili inerenti la persona a partire da yeraMonthFrom.
	 * 
	 * Utilizzo: specificare il mese dal quale ricostruire i riepiloghi. 

 	 * @param person
	 * @param yearMonthFrom
	 */
	public void populateContractMonthRecapByPerson( Person person, 
			YearMonth yearMonthFrom) {

		Person p = Person.findById(person.id);
		
		for( Contract contract : p.contracts ){
			
			DateInterval contractDateInterval = 
					wrapperFactory.create(contract).getContractDateInterval();
			YearMonth endContractYearMonth = new YearMonth(contractDateInterval.getEnd());
			
			//Se yearMonthFrom non è successivo alla fine del contratto...
			if ( !yearMonthFrom.isAfter(endContractYearMonth) ) {
				
				if( wrapperFactory.create(contract).getContractVacationPeriods().isEmpty()) {
					log.info("No vacation period {}", contract.toString());
					continue;
				}
				
				populateContractMonthRecap(contract, Optional.fromNullable(yearMonthFrom));
			} 
		}
	}
	
	/**
	 * Ritorna il riepilogo mensile del contatto.
	 * Se needed effettua il tentativo di ricalcolarlo.
	 * 
	 * @param contract
	 * @param yearMonth
	 * @param needed
	 * @return
	 */
	private Optional<ContractMonthRecap> getContractMonthRecap(Contract contract,
			YearMonth yearMonth, boolean needed) {
		
		for (ContractMonthRecap cmr : contract.contractMonthRecaps) {
			
			if ( cmr.year == yearMonth.getYear() && cmr.month == yearMonth.getMonthOfYear() )
				return Optional.fromNullable(cmr);
		}
		
		if (needed) {
			populateContractMonthRecap(contract,
					Optional.fromNullable(yearMonth));
			
			Optional<ContractMonthRecap> recap = getContractMonthRecap(contract, 
					yearMonth, false );
			
			return recap;
		}
		
		return Optional.absent();
	}
	
	/**
	 * Costruzione di un ContractMonthRecap pulito. Il preesistente se presente
	 * viene distrutto. Alternativa pulirlo (ma sono tanti campi!) 
	 * 
	 * @param contract
	 * @param yearMonth
	 * @return
	 */
	private ContractMonthRecap buildContractMonthRecap(Contract contract, 
			YearMonth yearMonth ) {
		
		Optional<ContractMonthRecap> cmrOld = 
				getContractMonthRecap(contract, yearMonth, false);

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
	 *    allora non si deve creare alcun riepilogo.
	 * 3) Se sourceDate non è l'ultimo giorno del mese e si riferisce ad un mese passato
	 *    costruisce il riepilogo andando a combinare le informazioni presenti in sourceContract
	 *    e nel database (a partire dal giorno successivo a sourceDate).
	 *    
	 * @return YearMonth di cui si deve costruire il prossimo contractMonthRecap
	 * @param contract
	 * @param yearMonthToCompute il riepilogo che si vuole costruire
	 * @return
	 */
	private YearMonth populateContractMonthFromSource(Contract contract, 
			YearMonth yearMonthToCompute) {
		
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
		AbsenceType ab31 = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).orNull();
		AbsenceType ab32 = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()).orNull();
		AbsenceType ab37 = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()).orNull(); 
		AbsenceType ab94 = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).orNull(); 
				
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

	/**
	 * FIXME: questa versione nasce come copia adattate del vecchio algoritmo basato
	 * sui riepiloghi annuali. Necessita di una rifattorizzazione per renderla meno complessa
	 * e più leggibile.
	 * 
	 * Popola la parte residuale del riepilogo mensile fino alla data calcolaFinoA: 
	 *  - minuti rimanenti dell'anno passato
	 *  - minuti rimanenti dell'anno corrente
	 *  - buoni pasto rimanenti
	 *  
	 *  Se il riepilogo mensile precedente necessario come input dell'algoritmo
	 *  non esiste, esso viene costruito tramite una chiamata a populateContractMonthRecap
	 *  con yearMonthToCompute absent() in modo che calcoli i riepiloghi per tutto il contratto.
	 *  Se è impossibile costruire il riepilogo torna absent().
	 * 
	 * @param cmr
	 * @param yearMonth
	 * @param calcolaFinoA
	 * @return il riepilogo costruito.
	 */
	private Optional<ContractMonthRecap> populateResidualModule(ContractMonthRecap cmr, 
			YearMonth yearMonth, LocalDate calcolaFinoA) {

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
		DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
		DateInterval mealTicketInterval = new DateInterval(dateStartMealTicket.orNull(), calcolaFinoA);

		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;

		////////////////////////////////////////////////////////////////////////
		//	Recupero situazione iniziale del mese richiesto
		////////////////////////////////////////////////////////////////////////
		
		Optional<ContractMonthRecap> recapPreviousMonth;
		
		YearMonth firstContractMonthRecap = wrapperFactory
				.create(contract).getFirstMonthToRecap();
		if ( yearMonth.isAfter(firstContractMonthRecap) ) {
			//Riepilogo essenziale
			recapPreviousMonth = getContractMonthRecap(contract, yearMonth.minusMonths(1), true);
			if( !recapPreviousMonth.isPresent() ) {
				return Optional.absent();
			}
		} else {
			recapPreviousMonth = getContractMonthRecap(contract, yearMonth.minusMonths(1), false);
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
			requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);

			// TODO: initMealTickets da source contract
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
					cmr.person.office.office, cmr.year);
			if(monthExpiryRecoveryDay != 0 && cmr.month > monthExpiryRecoveryDay) {
				cmr.possibileUtilizzareResiduoAnnoPrecedente = false;
				cmr.remainingMinutesLastYear = 0;
			}
		}
		
		//Inizializzazione buoni pasto
		if (recapPreviousMonth.isPresent()) {
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
		cmr.remainingMinutesCurrentYear = cmr.remainingMinutesCurrentYear + cmr.progressivoFinalePositivoMeseAux;	
		
		return Optional.fromNullable( cmr );
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
		CompetenceCode s1 = competenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode s2 = competenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode s3 = competenceCodeDao.getCompetenceCodeByCode("S3");
		
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
						wrapperFactory.create(abs.personDay).getWorkingTimeTypeDay().get().workingTime;	
				// FIXME: potrebbe essere absent() ??
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



	

