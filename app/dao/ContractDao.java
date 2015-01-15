package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.joda.time.LocalDate;

import helpers.ModelQuery;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.sun.org.apache.xml.internal.utils.SuballocatedByteVector;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.WorkingTimeType;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QInitializationAbsence;
import models.query.QInitializationTime;
import models.query.QWorkingTimeType;

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
	 * @param officeId
	 * @param wtt
	 * @return I contratti attivi che attualmente hanno impostato il WorkingTimeType
	 */
	public static List<Contract> getAssociatedActiveContract(Long officeId, WorkingTimeType wtt) {
		
		List<Contract> contractList = new ArrayList<Contract>();
		
		LocalDate today = new LocalDate();
		
		List<Contract> activeContract = Contract.getActiveContractInPeriod(today, today);
		
		for(Contract contract : activeContract) {
			
			if( !contract.person.office.id.equals(officeId))
				continue;
			
			ContractWorkingTimeType current = contract.getContractWorkingTimeType(today);
			if(current.workingTimeType.id.equals(wtt.id))
				contractList.add(contract);
		}
		
		return contractList;
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
