package dao;

import helpers.ModelQuery;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.WorkingTimeType;
import models.query.QWorkingTimeType;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class WorkingTimeTypeDao {

	/**
	 * 
	 * @param description
	 * @return il workingTimeType relativo alla descrizione passata come parametro
	 */
	public static WorkingTimeType getWorkingTimeTypeByDescription(String description){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt)
				.where(wtt.description.eq(description));
		return query.singleResult(wtt);
	}
	
	
	/**
	 * 
	 * @return la lista di tutti gli workingTimeType presenti nel database
	 */
	public static List<WorkingTimeType> getAllWorkingTimeType(){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt);
		return query.list(wtt);
	}
	
	
	/**
	 * 
	 * @param id
	 * @return il workingTimeType relativo all'id passato come parametro
	 */
	public static WorkingTimeType getWorkingTimeTypeById(Long id){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt)
				.where(wtt.id.eq(id));
		return query.singleResult(wtt);
	}
	
	
	/**
	 * 
	 * @return la lista degli orari di lavoro presenti di default sul database perchè validi a livello nazionale per il CNR
	 */
	public static List<WorkingTimeType> getDefaultWorkingTimeType(){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt)
				.where(wtt.office.isNull()).orderBy(wtt.description.asc());
		return query.list(wtt);
	}
	
	
	/**
	 * 
	 * @param date
	 * @return il tipo di orario di lavoro utilizzato in date
	 */
	public WorkingTimeType getWorkingTimeType(LocalDate date, Person person) {
		//Contract contract = this.getContract(date);
		Contract contract = ContractDao.getContract(date, person);
		if(contract==null)
			return null;
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType;
			}
		}
		return null;
	}
	
	/**
	 *	//FIXME eliminare questo metodo affrontando il passaggio ad Injection
	 *	//del PersonDayManager. 
	 * @param date
	 * @return il tipo di orario di lavoro utilizzato in date
	 */
	@Deprecated
	public static WorkingTimeType getWorkingTimeTypeStatic(LocalDate date, Person person) {
		//Contract contract = this.getContract(date);
		Contract contract = ContractDao.getContract(date, person);
		if(contract==null)
			return null;
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				return cwtt.workingTimeType;
			}
		}
		return null;
	}
}
