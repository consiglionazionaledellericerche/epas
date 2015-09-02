package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Absence;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPAPlugin;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.PersonDao;
import dao.VacationCodeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

/**
 * 
 * Manager per Contract
 * 
 * @author alessandro
 *
 */
public class ContractManager {

	@Inject
	public ContractManager(ConfGeneralManager confGeneralManager,
			ConsistencyManager consistencyManager, 
			VacationCodeDao vacationCodeDao,
			VacationManager vacationManager,
			IWrapperFactory wrapperFactory, 
			ContractDao contractDao, 
			PersonDao personDao) {

		this.confGeneralManager = confGeneralManager;
		this.consistencyManager = consistencyManager;
		this.vacationCodeDao = vacationCodeDao;
		this.vacationManager = vacationManager;
		this.wrapperFactory = wrapperFactory;
		this.contractDao = contractDao;
		this.personDao = personDao;
	}

	private final ConfGeneralManager confGeneralManager;
	private final ConsistencyManager consistencyManager;
	private final IWrapperFactory wrapperFactory;
	private final ContractDao contractDao;
	private final PersonDao personDao;
	private final VacationCodeDao vacationCodeDao;
	private final VacationManager vacationManager;
	
	private final static Logger log = LoggerFactory.getLogger(ContractManager.class);
	
	/**
	 * True se il contratto non si interseca con nessun 
	 * altro contratto per la persona. False altrimenti
	 * @return
	 */
	public boolean isProperContract(Contract contract) {

		DateInterval contractInterval = wrapperFactory
				.create(contract).getContractDateInterval();
		for(Contract c : contract.person.contracts) {

			if(contract.id != null && c.id.equals(contract.id)) {
				continue;
			}

			if(DateUtility.intervalIntersection(contractInterval, 
					wrapperFactory.create(c).getContractDateInterval()) != null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Validatore per il contratto. Controlla la consistenza delle date 
	 * all'interno del contratto e la coerenza con gli altri contratti della persona.
	 * @param contract
	 * @return
	 */
	public boolean contractCrossFieldValidation(Contract contract) {

		if(contract.expireContract != null 
				&& contract.expireContract.isBefore(contract.beginContract))
			return false;

		if(contract.endContract != null 
				&& contract.endContract.isBefore(contract.beginContract))
			return false;

		if(contract.expireContract != null && contract.endContract != null 
				&& contract.expireContract.isBefore(contract.endContract))
			return false;

		if(!isProperContract(contract) ) 
			return false;

		return true;
	}

	/**
	 * Costruisce in modo controllato tutte le strutture dati associate al contratto
	 * appena creato passato come argomento.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * Effettua la recompute.
	 * 
	 * @param contract
	 */
	public boolean properContractCreate(Contract contract, WorkingTimeType wtt) {

		if ( !contractCrossFieldValidation(contract)) {
			return false;
		}
		
		if ( !isProperContract(contract) ) { 
			return false;
		}
		
		contract.save();
		
		buildVacationPeriods(contract);

		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		contract.contractWorkingTimeType.add(cwtt);

		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contract.beginContract;
		csp.endTo = contract.expireContract;
		csp.fixedworkingtime = false;
		csp.save();
		contract.contractStampProfile.add(csp);
		
		//Se il contratto inizia prima di today inserisco un source fittizio alla
		// data attuale che mi imposta tutti i residui a zero.
		//(Comportamento Disabilitato, da valutare.)
		contract.sourceDate = null;
//		IWrapperContract wcontract = wrapperFactory.create(contract);
//		contract.sourceDate = LocalDate.now().minusDays(1);
//		
//		contract.sourceRemainingMealTicket = 0;
//		contract.sourceRemainingMinutesCurrentYear = 0;
//		contract.sourceRemainingMinutesLastYear = 0;
//
//		List<Absence> postPartum = Lists.newArrayList();
//		
//		contract.sourcePermissionUsed = vacationManager.getPermissionAccruedYear(wcontract, 
//				contract.sourceDate.getYear() , Optional.<LocalDate>absent());
//		
//		contract.sourceVacationLastYearUsed = vacationManager.getVacationAccruedYear(wcontract,
//				contract.sourceDate.getYear() -1, Optional.<LocalDate>absent(), postPartum);
//				
//		contract.sourceVacationCurrentYearUsed = vacationManager.getVacationAccruedYear(wcontract,
//				contract.sourceDate.getYear(), Optional.<LocalDate>absent(), postPartum);
//		
//		contract.sourceRecoveryDayUsed = 0;		
//		
		contract.sourceByAdmin = false;
		
		contract.save();
		
		// FIXME: comando JPA per aggiornale la person
		contract.person.contracts.add(contract);
		
		//Aggiornamento stato contratto
		//Nella creazione se effettuo i ricalcoli devo farli necessariamente
		//per tutto il contratto (al più dall'installazione del software).
		//TODO: Si potrebbe prevedere la possibilità di specificare se effettuare 
		//'inizializzazione fittizia che esaurisce tutte le ferie e i permessi 
		// e inibisce tutti i ricalcoli nel caso di creazione di contratto inserito 
		// dopo il suo inizio.  
		//(adesso l'ho disabilitata perchè come soluzione non mi piace).
		recomputeContract(contract, Optional.<LocalDate>absent(), true);
		
		return true;

	}

	/**
	 * Aggiorna in modo appropriato tutte le strutture dati associate al contratto modificato.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * Effettua la recomputeContract.
	 * 
	 * @param contract
	 */
	public void properContractUpdate(Contract contract) {

		buildVacationPeriods(contract);
		updateContractWorkingTimeType(contract);
		updateContractStampProfile(contract);
		
		//Ricalcoli dall'inizio del contratto.
		// TODO: l'update dovrebbe ricevere un parametro dateFrom nel quale impostare
		//la data dalla quale effettuare i ricalcoli. Se ne deve occupare il chiamante.
		recomputeContract(contract, Optional.<LocalDate>absent(), true);
	}

	/**
	 * Ricalcola completamente tutti i dati del contratto da dateFrom a dateTo.
	 * 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. 
	 *   Se null ricalcola dall'inizio del contratto.
	 *   newContract: indica se il ricalcolo è relativo ad un nuvo contratto o ad uno già esistente
	 */
	public void recomputeContract(Contract contract, Optional<LocalDate> dateFrom, 
			boolean newContract) {

		// (0) Definisco l'intervallo su cui operare
		// Decido la data inizio
		LocalDate initUse = new LocalDate(confGeneralManager
				.getFieldValue(Parameter.INIT_USE_PROGRAM, contract.person.office));
		
		LocalDate startDate = contract.beginContract;
		if(startDate.isBefore(initUse)) {
			startDate = initUse;
		}

		if(dateFrom.isPresent()) {
			if(startDate.isBefore(dateFrom.get())) {
				startDate = dateFrom.get();
			}
		}
	
		if(!newContract){
			//Distruggere i riepiloghi
			// TODO: anche quelli sulle ferie quando ci saranno
			destroyContractMonthRecap(contract);
			
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
		}
		
		consistencyManager.updatePersonSituation(contract.person.id, startDate);
	}
	
	private void destroyContractMonthRecap(Contract contract) {
		for(ContractMonthRecap cmr : contract.contractMonthRecaps) {
			cmr.delete();
		}
		//contract = contractDao.getContractById(contract.id);
		contract.save();
	}

	/**
	 * Costruisce la struttura dei periodi ferie associati al contratto 
	 * applicando la normativa vigente.
	 * 
	 * @param contract
	 */
	private void buildVacationPeriods(Contract contract){

		// FIXME: Distruggere o aggiornare quelli precedenti
		
		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(contract.expireContract == null) {

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
			first.contract = contract;
			first.save();
			contract.vacationPeriods.add(first);
			
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = vacationCodeDao.getVacationCodeByDescription("28+4");
			second.contract = contract;
			second.save();
			contract.vacationPeriods.add(second);
			contract.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(contract.expireContract.isAfter(contract.beginContract.plusYears(3).minusDays(1))){
			
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
			first.contract = contract;
			first.save();
			contract.vacationPeriods.add(first);
			
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = contract.expireContract;
			second.vacationCode = vacationCodeDao.getVacationCodeByDescription("28+4");
			second.contract = contract;
			second.save();
			contract.vacationPeriods.add(second);
			contract.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = contract.beginContract;
		first.endTo = contract.expireContract;
		first.contract = contract;
		first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
		first.save();
		contract.vacationPeriods.add(first);
		contract.save();
	}

	/**
	 * Quando vengono modificate le date di inizio o fine del contratto 
	 * occorre rivedere la struttura dei periodi di tipo orario.
	 * (1)Eliminare i periodi non più appartenenti al contratto
	 * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 */
	private void updateContractWorkingTimeType(Contract contract){
		//Aggiornare i periodi workingTimeType
		//1) Cancello quelli che non appartengono più a contract
		List<ContractWorkingTimeType> toDelete = new ArrayList<ContractWorkingTimeType>();
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
			if(DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(), cwttInterval) == null)
			{
				toDelete.add(cwtt);
			}
		}
		for(ContractWorkingTimeType cwtt : toDelete)
		{
			cwtt.delete();
			contract.contractWorkingTimeType.remove(cwtt);
			contract.save();
		}

		//Conversione a List per avere il metodo get()
		List<ContractWorkingTimeType> cwttList = Lists.newArrayList(contract.contractWorkingTimeType);

		//Sistemo il primo		
		ContractWorkingTimeType first = cwttList.get(0);
		first.beginDate = wrappedContract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractWorkingTimeType last = 
				cwttList.get(contract.contractWorkingTimeType.size()-1);
		last.endDate = wrappedContract.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endDate))
			last.endDate = null;
		last.save();
		contract.save();
	}

	/**
	 * Quando vengono modificate le date di inizio o fine del contratto 
	 * occorre rivedere la struttura dei periodi di stampProfile.
	 * (1)Eliminare i periodi non più appartenenti al contratto
	 * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 * 
	 */
	private void updateContractStampProfile(Contract contract){
		//Aggiornare i periodi stampProfile
		//1) Cancello quelli che non appartengono più a contract
		List<ContractStampProfile> toDelete = new ArrayList<ContractStampProfile>();
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		for(ContractStampProfile csp : contract.contractStampProfile)
		{
			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(), cspInterval) == null)
			{
				toDelete.add(csp);
			}
		}
		for(ContractStampProfile csp : toDelete)
		{
			csp.delete();
			contract.contractWorkingTimeType.remove(csp);
			contract.save();
		}

		//Conversione a List per avere il metodo get()
		List<ContractStampProfile> cspList = Lists.newArrayList(contract.contractStampProfile);

		//Sistemo il primo		
		ContractStampProfile first = cspList.get(0);
		first.startFrom = wrappedContract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractStampProfile last = 
				cspList.get(contract.contractStampProfile.size()-1);
		last.endTo = wrappedContract.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endTo))
			last.endTo = null;
		last.save();
		contract.save();
	}

	/**
	 * Il ContractWorkingTimeType a cui appartiene la data.
	 * @param date
	 * @return
	 */
	public ContractWorkingTimeType getContractWorkingTimeTypeFromDate(Contract contract, LocalDate date) {

		for(ContractWorkingTimeType cwtt: contract.contractWorkingTimeType) {

			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate) ))
				return cwtt;
		}
		return null;
	}


	/**
	 * Conversione della lista dei contractStampProfile da Set a List
	 * @param contract
	 * @return
	 */
	public List<ContractStampProfile> getContractStampProfileAsList(Contract contract) {

		return Lists.newArrayList(contract.contractStampProfile);
	}

	/**
	 * La lista con tutti i contratti attivi nel periodo selezionato.
	 * @return
	 */
	public List<Contract> getActiveContractInPeriod(LocalDate begin, LocalDate end) {

		if(end == null)
			end = new LocalDate(9999,1,1);

		List<Contract> activeContract = contractDao.getActiveContractsInPeriod(begin, end);

		return activeContract;

	}

	

	/**
	 * 
	 * @param contract
	 */
	public void saveSourceContract(Contract contract){
		if(contract.sourceVacationLastYearUsed == null){ 
			contract.sourceVacationLastYearUsed = 0;
		}
		if(contract.sourceVacationCurrentYearUsed == null) {
			contract.sourceVacationCurrentYearUsed = 0;
		}
		if(contract.sourcePermissionUsed == null) {
			contract.sourcePermissionUsed = 0;
		}
		if(contract.sourceRemainingMinutesCurrentYear == null){ 
			contract.sourceRemainingMinutesCurrentYear = 0;
		}
		if(contract.sourceRemainingMinutesLastYear == null){ 
			contract.sourceRemainingMinutesLastYear = 0;
		}
		if(contract.sourceRecoveryDayUsed == null){ 
			contract.sourceRecoveryDayUsed = 0;
		}
		if(contract.sourceRemainingMealTicket == null){ 
			contract.sourceRemainingMealTicket = 0;
		}

		contract.save();
	}

}
