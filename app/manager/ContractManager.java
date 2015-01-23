package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.ContractYearRecap;
import models.PersonDay;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import play.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.PersonDayDao;
import dao.VacationCodeDao;
import dao.VacationPeriodDao;

/**
 * 
 * Manager per Contract
 * 
 * @author alessandro
 *
 */
public class ContractManager {
	
	/**
	 * Costruisce in modo controllato tutte le strutture dati associate al contratto
	 * appena creato passato come argomento.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * @param contract
	 */
	public static void properContractCreate(Contract contract, WorkingTimeType wtt) {
		
		ContractManager.buildVacationPeriods(contract);
		
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		
		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contract.beginContract;
		csp.endTo = contract.expireContract;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();

	}
	
	/**
	 * Aggiorna in modo appropriato tutte le strutture dati associate al contratto modificato.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * @param contract
	 */
	public static void properContractUpdate(Contract contract) {
		
		ContractManager.buildVacationPeriods(contract);
		ContractManager.updateContractWorkingTimeType(contract);
		ContractManager.updateContractStampProfile(contract);
	}

	/**
	 * Ritorna l'intervallo valido ePAS per il contratto. 
	 * (scarto la parte precedente a source contract se definita)
	 * @return
	 */
	public static DateInterval getContractDatabaseDateInterval(Contract contract) {
		
		if(contract.sourceDate != null && contract.sourceDate.isAfter(contract.beginContract)) {
			
			DateInterval contractInterval;
			if(contract.endContract!=null)
				contractInterval = new DateInterval(contract.sourceDate, contract.endContract);
			else
				contractInterval = new DateInterval(contract.sourceDate, contract.expireContract);
			return contractInterval;
		}
		
		return contract.getContractDateInterval();
		
	}
	
	/**
	 * Ricalcola completamente tutti i dati del contratto da dateFrom a dateTo.
	 *  
	 * 1) CheckHistoryError 
	 * 2) Ricalcolo tempi lavoro
	 * 3) Ricalcolo riepiloghi annuali 
	 * 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. 
	 *   Se null ricalcola dall'inizio del contratto.
	 *   
	 * @param dateTo ultimo giorno coinvolto nel ricalcolo. 
	 *   Se null ricalcola fino alla fine del contratto (utile nel caso in cui si 
	 *   modifica la data fine che potrebbe non essere persistita)
	 */
	public static void recomputeContract(Contract contract, LocalDate dateFrom, LocalDate dateTo) {

		// (0) Definisco l'intervallo su cui operare
		// Decido la data inizio
		String dateInitUse = ConfGeneralManager.getFieldValue("init_use_program", contract.person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		LocalDate date = contract.beginContract;
		if(date.isBefore(initUse))
			date = initUse;
		DateInterval contractInterval = ContractManager.getContractDatabaseDateInterval(contract);
		if( dateFrom != null && contractInterval.getBegin().isBefore(dateFrom)) {
			contractInterval = new DateInterval(dateFrom, contractInterval.getEnd());
		}
		// Decido la data di fine
		if(dateTo != null && dateTo.isBefore(contractInterval.getEnd())) {
			contractInterval = new DateInterval(contractInterval.getBegin(), dateTo);
		}

		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		LocalDate today = new LocalDate();
		Logger.info("CheckPersonDay (creazione ed history error) DA %s A %s", date, today);
		while(true) {
			Logger.debug("RecomputePopulate %s", date);

			if(date.isEqual(today))
				break;

			if(!DateUtility.isDateIntoInterval(date, contractInterval)) {
				date = date.plusDays(1);
				continue;
			}

			PersonUtility.checkPersonDay(contract.person.id, date);
			date = date.plusDays(1);


		}

		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		LocalDate actualMonth = contractInterval.getBegin().withDayOfMonth(1).minusMonths(1);
		LocalDate endMonth = new LocalDate().withDayOfMonth(1);

		Logger.debug("PopulatePersonDay (ricalcoli ed history error) DA %s A %s", actualMonth, endMonth);

		while( !actualMonth.isAfter(endMonth) )
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(contract.person, actualMonth, Optional.fromNullable(actualMonth.dayOfMonth().withMaximumValue()), true);
			//			List<PersonDay> pdList = 
			//					PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
			//							contract.person, actualMonth, actualMonth.dayOfMonth().withMaximumValue()).fetch();

			for(PersonDay pd : pdList){

				PersonDay pd1 = PersonDayDao.getPersonDayById(pd.id);
				//PersonDay pd1 = PersonDay.findById(pd.id);
				Logger.debug("RecomputePopulate %s", pd1.date);				
				PersonDayManager.populatePersonDay(pd1);
			}

			actualMonth = actualMonth.plusMonths(1);
		}

		Logger.info("BuildContractYearRecap");

		//(3) Ricalcolo dei riepiloghi annuali
		ContractYearRecapManager.buildContractYearRecap(contract);


	}

	/**
	 * Costruisce la struttura dei periodi ferie associati al contratto 
	 * applicando la normativa vigente.
	 * 
	 * @param contract
	 */
	private static void buildVacationPeriods(Contract contract){

		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(contract.expireContract == null)
		{

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCodeDao.getVacationCodeByDescription("26+4");
			//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = contract;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = VacationCodeDao.getVacationCodeByDescription("28+4");
			//second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract = contract;
			second.save();
			contract.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(contract.expireContract.isAfter(contract.beginContract.plusYears(3).minusDays(1))){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCodeDao.getVacationCodeByDescription("26+4");
			//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = contract;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = contract.expireContract;
			second.vacationCode = VacationCodeDao.getVacationCodeByDescription("28+4");
			//second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract = contract;
			second.save();
			contract.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = contract.beginContract;
		first.endTo = contract.expireContract;
		first.contract = contract;
		first.vacationCode = VacationCodeDao.getVacationCodeByDescription("26+4");
		//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
		first.save();
		contract.save();
	}
	
	/**
	 * Quando vengono modificate le date di inizio o fine del contratto 
	 * occorre rivedere la struttura dei periodi di tipo orario.
	 * (1)Eliminare i periodi non più appartenenti al contratto
	 * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 */
	private static void updateContractWorkingTimeType(Contract contract)
	{
		//Aggiornare i periodi workingTimeType
		//1) Cancello quelli che non appartengono più a contract
		List<ContractWorkingTimeType> toDelete = new ArrayList<ContractWorkingTimeType>();
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
			if(DateUtility.intervalIntersection(contract.getContractDateInterval(), cwttInterval) == null)
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
		first.beginDate = contract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractWorkingTimeType last = 
				cwttList.get(contract.contractWorkingTimeType.size()-1);
		last.endDate = contract.getContractDateInterval().getEnd();
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
	private static void updateContractStampProfile(Contract contract)
	{
		//Aggiornare i periodi stampProfile
		//1) Cancello quelli che non appartengono più a contract
		List<ContractStampProfile> toDelete = new ArrayList<ContractStampProfile>();
		for(ContractStampProfile csp : contract.contractStampProfile)
		{
			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.intervalIntersection(contract.getContractDateInterval(), cspInterval) == null)
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
		first.startFrom = contract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractStampProfile last = 
				cspList.get(contract.contractStampProfile.size()-1);
		last.endTo = contract.getContractDateInterval().getEnd();
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
	public static ContractWorkingTimeType getContractWorkingTimeTypeFromDate(Contract contract, LocalDate date) {
		
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
	public static List<ContractStampProfile> getContractStampProfileAsList(Contract contract) {
		
		return Lists.newArrayList(contract.contractStampProfile);
	}
	
	/**
	 * Ritorna il ContractStampProfile attivo alla data.
	 * @param contract
	 * @param date
	 * @return
	 */
	public static ContractStampProfile getContractStampProfileFromDate(Contract contract, LocalDate date) {
		
		for(ContractStampProfile csp : contract.contractStampProfile) {
			DateInterval interval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.isDateIntoInterval(date, interval))
				return csp;
			
		}
		return null;
	}
	
	/**
	 * La lista dei VacationPeriod associati al contratto in ordine crescente per data di inizio periodo.
	 * @param contract
	 * @return null in caso di piani ferie inesistenti.
	 */
	public static List<VacationPeriod> getContractVacationPeriods(Contract contract)
	{
	
		List<VacationPeriod> vpList = VacationPeriodDao.getVacationPeriodByContract(contract);

		//se il piano ferie associato al contratto non esiste 
		if(vpList.isEmpty())
		{
			return null;
		}

		return vpList;
	}

	/**
	 * Ritorna il riepilogo annule del contatto.
	 * @param year
	 * @return
	 */
	public static ContractYearRecap getContractYearRecap(Contract contract, int year)	{
		for(ContractYearRecap cyr : contract.recapPeriods) {
			
			if(cyr.year==year)
				return cyr;
		}
		return null;
	}
	
	/**
	 * La lista con tutti i contratti attivi nel periodo selezionato.
	 * @return
	 */
	public static List<Contract> getActiveContractInPeriod(LocalDate begin, LocalDate end) {
		
		if(end == null)
			end = new LocalDate(9999,1,1);

		List<Contract> activeContract = ContractDao.getActiveContractsInPeriod(begin, end);
		
		return activeContract;

	}
	
	/**
	 * True se il contratto non si interseca con nessun altro contratto per la persona. False altrimenti
	 * @return
	 */
	public static boolean isProperContract(Contract contract) {

		DateInterval contractInterval = contract.getContractDateInterval();
		for(Contract c : contract.person.contracts) {
			
			if(contract.id != null && c.id.equals(contract.id)) {
				continue;
			}
			
			if(DateUtility.intervalIntersection(contractInterval, c.getContractDateInterval()) != null) {
				return false;
			}
		}
		return true;
	}

}
