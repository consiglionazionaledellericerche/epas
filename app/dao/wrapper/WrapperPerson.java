package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.ContractManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;

/**
 * @author marco
 *
 */
public class WrapperPerson implements IWrapperPerson {

	private final Person value;
	private final ContractManager contractManager;
	private final ContractDao contractDao;

	private Optional<Contract> currentContract;
	private WorkingTimeType currentWorkingTimeType = null;
	private VacationCode currentVacationCode = null;

	@Inject
	WrapperPerson(@Assisted Person person,	ContractManager contractManager,
			ContractDao contractDao) {
		this.value = person;
		this.contractManager = contractManager;
		this.contractDao = contractDao;
	}

	@Override
	public Person getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return il contratto attualmente attivo per quella persona
	 */
	public Optional<Contract> getCurrentContract() {
		
		if( this.currentContract != null )
			return this.currentContract;
		
		if( this.currentContract == null )
			this.currentContract = Optional.fromNullable(contractDao.getContract(LocalDate.now(), value));

		return this.currentContract;
	}

	public ContractStampProfile getCurrentContractStampProfile() {
		
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		
		if( !this.currentContract.isPresent() ) {
			return null;
		}
		
		return contractManager.getContractStampProfileFromDate( this.currentContract.get(),
				LocalDate.now());
	}
	
	public  WorkingTimeType getCurrentWorkingTimeType(){
		
		if( this.currentWorkingTimeType != null ) {
			return this.currentWorkingTimeType;
		}
		
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		
		if( !this.currentContract.isPresent())
			return null;
		
		//ricerca
		for(ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				this.currentWorkingTimeType = cwtt.workingTimeType; 
				return currentWorkingTimeType;
			}
		}
		return null;

	}

	public VacationCode getCurrentVacationCode() {

		if( this.currentVacationCode != null )
			return this.currentVacationCode;
				
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		if( ! this.currentContract.isPresent() )
			return null;
		
		//ricerca
		for(VacationPeriod vp : this.currentContract.get().vacationPeriods)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(vp.beginFrom, vp.endTo)))
			{
				this.currentVacationCode = vp.vacationCode;
				return this.currentVacationCode;
			}
		}
		return null;
	}
}
