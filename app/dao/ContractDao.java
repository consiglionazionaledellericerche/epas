package dao;

import helpers.ModelQuery;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QInitializationAbsence;
import models.query.QInitializationTime;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class ContractDao {

	/**
	 * 
	 * @param id
	 * @return il contratto corrispondente all'id passato come parametro
	 */
	public static Contract getContractById(Long id){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.id.eq(id));
		return query.singleResult(contract);
	}
	
	/**
	 * 
	 * @param begin
	 * @param end
	 * @return la lista di contratti che sono attivi nel periodo compreso tra begin e end
	 */
	public static List<Contract> getActiveContractsInPeriod(LocalDate begin, LocalDate end){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.endContract.isNull().andAnyOf(
						contract.expireContract.isNull().and(contract.beginContract.loe(end)), 
						contract.expireContract.isNotNull().and(contract.beginContract.loe(end).and(contract.expireContract.goe(begin))))
						.or(contract.endContract.isNotNull().and(contract.beginContract.loe(end).and(contract.endContract.goe(begin)))));
		return query.list(contract);
	}
	
	/**
	 * 
	 * @param person
	 * @return la lista di contratti associati alla persona person passata come parametro ordinati per data inizio contratto
	 */
	public static List<Contract> getPersonContractList(Person person){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.person.eq(person)).orderBy(contract.beginContract.asc());
		return query.list(contract);
	}
	
	
	/**
	 * 
	 * @param wtt
	 * @return la lista di contratti associata al workingTimeType passato come parametro
	 */
	public static List<Contract> getContractListByWorkingTimeType(WorkingTimeType wtt){
		QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract).
				leftJoin(contract.contractWorkingTimeType,cwtt).where(cwtt.workingTimeType.eq(wtt));
						
		return query.list(contract);
	}
	

	//PER la delete quindi per adesso permettiamo l'eliminazione solo di contratti particolari di office
	//bisogna controllare che this non sia default ma abbia l'associazione con office
	
	public static List<Contract> getAssociatedContract(WorkingTimeType wtt) {

		List<Contract> contractList = ContractDao.getContractListByWorkingTimeType(wtt);
//		List<Contract> contractList = Contract.find(
//				"Select distinct c from Contract c "
//						+ "left outer join fetch c.contractWorkingTimeType as cwtt "
//						+ "where cwtt.workingTimeType = ?", this).fetch();

		return contractList;
	}

	/**
	 * 
	 * @return il contratto attivo per quella persona alla date date
	 */
	public static Contract getContract(LocalDate date, Person person){
		
		for(Contract c : person.contracts)
		{
			if(DateUtility.isDateIntoInterval(date, c.getContractDateInterval()))
				return c;
		}
		
		//FIXME sommani aprile 2014, lui ha due contratti ma nello heap ce ne sono due identici e manca quello nuovo.
		List<Contract> contractList = ContractDao.getPersonContractList(person);
		//List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", this).fetch();
		//this.contracts = contractList;
		for(Contract c : contractList)
		{
			if(DateUtility.isDateIntoInterval(date, c.getContractDateInterval()))
				return c;
		}
		//-----------------------
		return null;
	}
	
	/**
	 * Il Contratto corrente per la persona. (Attivo alla data di oggi).
	 * @param person
	 * @return null se non esiste contratto attivo alla data di oggi.
	 */
	public static Contract getCurrentContract(Person person) {
		
		return getContract(LocalDate.now(), person);
	}
	
	/**
	 * Il Tipo orario attivo per la persona (Attivo alla data di oggi).
	 * @param person
	 * @return null se la persona non ha contratto attivo oggi.
	 */
	public static WorkingTimeType getCurrentWorkingTimeType(Person person) {
		
		Contract currentContract = ContractDao.getCurrentContract(person);
		
		if(currentContract == null)
			return null;
		
		//ricerca
		for(ContractWorkingTimeType cwtt : currentContract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType; 
			}
		}
		return null;
	}
	
	/**
	 * Il piano ferie attivo per la person (Attivo alla data di oggi).
	 * @param person
	 * @return
	 */
	public static VacationPeriod getCurrentVacationPeriod(Person person)
	{
		Contract contract = ContractDao.getCurrentContract(person);
		
		if(contract == null)
			return null;

		for(VacationPeriod vp : contract.vacationPeriods) {

			LocalDate now = new LocalDate();

			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
		}
		return null;
	}
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative ai ContractStampProfile per evitare di creare una classe specifica che contenga     */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @return la lista dei contractStampProfile relativi alla persona person o al contratto contract passati come parametro 
	 * e ordinati per data inizio del contractStampProfile
	 * La funzione permette di scegliere quale dei due parametri indicare per effettuare la ricerca. Sono mutuamente esclusivi
	 */
	public static List<ContractStampProfile> getPersonContractStampProfile(Optional<Person> person, Optional<Contract> contract){
		QContractStampProfile csp = QContractStampProfile.contractStampProfile;
		final BooleanBuilder condition = new BooleanBuilder();
		if(person.isPresent())
			condition.and(csp.contract.person.eq(person.get()));
		if(contract.isPresent())
			condition.and(csp.contract.eq(contract.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(csp)
				.where(condition).orderBy(csp.startFrom.asc());
		return query.list(csp);
		
	}
	
	/**
	 * 
	 * @param id
	 * @return il contractStampProfile relativo all'id passato come parametro
	 */
	public static ContractStampProfile getContractStampProfileById(Long id){
		QContractStampProfile csp = QContractStampProfile.contractStampProfile;
		final JPQLQuery query = ModelQuery.queryFactory().from(csp)
				.where(csp.id.eq(id));
		return query.singleResult(csp);
	}
	
	
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative agli InitializationTime per evitare di creare una classe specifica che contenga     */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @return l'initializationTime relativo alla persona passata come parametro
	 */
	public static InitializationTime getInitializationTime(Person person){
		QInitializationTime init = QInitializationTime.initializationTime;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.person.eq(person));
		return query.singleResult(init);
		
	}
	
	/**
	 * 
	 * @param id
	 * @return l'initializationTime relativo all'id passato come parametro
	 */
	public static InitializationTime getInitializationTimeById(Long id){
		QInitializationTime init = QInitializationTime.initializationTime;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.id.eq(id));
		return query.singleResult(init);
	}
	
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative agli InitializationAbsence per evitare di creare una classe specifica che contenga  */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/

	/**
	 * 
	 * @param id
	 * @return l'initializationTime relativo all'id passato come parametro
	 */
	public static InitializationAbsence getInitializationAbsenceById(Long id){
		QInitializationAbsence init = QInitializationAbsence.initializationAbsence;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.id.eq(id));
		return query.singleResult(init);
	}
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative ai ContractWorkingTimeType per evitare di creare una classe specifica che contenga  */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	
	/**
	 * 
	 * @param contract
	 * @return la lista di contractWorkingTimeType associati al contratto passato come parametro
	 */
	public static List<ContractWorkingTimeType> getContractWorkingTimeTypeList(Contract contract){
		QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(cwtt)
				.where(cwtt.contract.eq(contract));
		return query.list(cwtt);
		
	}
}
