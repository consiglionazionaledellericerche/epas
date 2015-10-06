package dao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.query.QWorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class WorkingTimeTypeDao extends DaoBase{

	private final ContractDao contractDao;
	@Inject
	WorkingTimeTypeDao(JPQLQueryFactory queryFactory,
			Provider<EntityManager> emp,ContractDao contractDao) {
		super(queryFactory, emp);
		this.contractDao = contractDao;
	}

	/**
	 * 
	 * @param description
	 * @return 
	 */
	@Deprecated
	public WorkingTimeType getWorkingTimeTypeByDescription(String description){
		final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = getQueryFactory().from(wtt)
				.where(wtt.description.eq(description));
		return query.singleResult(wtt);
	}

	/**
	 * Se office è present il tipo orario di con quella descrizione se esiste.
	 * Se office non è present il tipo orario di default con quella descrizione.
	 * @param description
	 * @param office
	 * @return
	 */
	public WorkingTimeType workingTypeTypeByDescription(String description,
			Optional<Office> office) {
		
		final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		
		if(office.isPresent()) {
			return getQueryFactory().from(wtt)
					.where(wtt.description.eq(description).and(wtt.office.eq(office.get())))
					.singleResult(wtt);
		} else {
			return getQueryFactory().from(wtt)
					.where(wtt.description.eq(description).and(wtt.office.isNull()))
					.singleResult(wtt);
		}
		
	}
	 


	/**
	 * 
	 * @return 
	 */
	public List<WorkingTimeType> getAllWorkingTimeType(){
		final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = getQueryFactory().from(wtt);
		return query.list(wtt);
	}


	/**
	 * 
	 * @param id
	 * @return 
	 */
	public WorkingTimeType getWorkingTimeTypeById(Long id){
		final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = getQueryFactory().from(wtt)
				.where(wtt.id.eq(id));
		return query.singleResult(wtt);
	}


	/**
	 * 
	 * @return la lista degli orari di lavoro presenti di default sul database
	 */
	public List<WorkingTimeType> getDefaultWorkingTimeType(){
		final QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = getQueryFactory().from(wtt)
				.where(wtt.office.isNull()).orderBy(wtt.description.asc());
		return query.list(wtt);
	}

	/**
	 * 
	 * @param date
	 * @return il tipo di orario di lavoro utilizzato in date
	 */
	public Optional<WorkingTimeType> getWorkingTimeType(LocalDate date, Person person) {

		Contract contract = contractDao.getContract(date, person);

		if(contract!=null){
			for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType){

				if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate))){
					return Optional.of(cwtt.workingTimeType);
				}
			}
		}
		return Optional.absent();
	}

}
