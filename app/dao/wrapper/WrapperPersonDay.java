package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.PersonManager;
import models.Contract;
import models.ContractStampProfile;
import models.MealTicket;
import models.Person;
import models.PersonDay;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;

/**
 * @author alessandro
 *
 */
public class WrapperPersonDay implements IWrapperPersonDay {

	private final PersonDay value;
	
	private Optional<PersonDay> previousPersonDayInMonth = null;
	private Contract personDayContract = null;
	private Boolean isHoliday = null;
	private Boolean isFixedTimeAtWorkk = null;
	private MealTicket mealTicketAssigned = null;

	private final ContractDao contractDao;
	private final PersonManager personManager;
	
	@Inject
	WrapperPersonDay(@Assisted PersonDay pd, ContractDao contractDao,
			PersonManager personManager) {
		this.value = pd;
		this.contractDao = contractDao;
		this.personManager = personManager;
	}

	@Override
	public PersonDay getValue() {
		return value;
	}
	
	/**
	 * 
	 */
	public Contract getPersonDayContract() {

		if(this.personDayContract != null)
			return this.personDayContract;
		
		this.personDayContract = 
				contractDao.getContract(this.value.date, this.value.person);
		
		return this.personDayContract;
	}
	
	/**
	 * Controlla che il personDay cada in un giorno festivo
	 * 
	 * @param data
	 * @return
	 */
	public boolean isHoliday(){
		if(isHoliday != null)
			return isHoliday;
		
		isHoliday = personManager.isHoliday(this.value.person, this.value.date);
		return isHoliday;
	}
	
	/** 
	 * True se il personDay cade in uno stampProfile con fixedTimeAtWork = true;
	 * 
	 * @return
	 */
	public boolean isFixedTimeAtWork()
	{
		if( this.isFixedTimeAtWorkk!=null ) 
			return this.isFixedTimeAtWorkk;
		
		this.isFixedTimeAtWorkk = false;
		
		Contract contract = getPersonDayContract();
		
		if(contract == null)
			return false;
		
		for(ContractStampProfile csp : contract.contractStampProfile){
			
			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
			
			if( DateUtility.isDateIntoInterval(this.value.date, cspInterval) ){
				this.isFixedTimeAtWorkk = csp.fixedworkingtime;
			}
		}

		return this.isFixedTimeAtWorkk;
	}
	
	/**
	 * 
	 * @param previous
	 */
	public void setPreviousPersonDayInMonth(PersonDay previous) {
		this.previousPersonDayInMonth = Optional.fromNullable(previous);
	}
	
	/**
	 * 
	 * @return
	 */
	public Optional<PersonDay> getPreviousPersonDayInMonth() {
		if( this.previousPersonDayInMonth == null ) {
			//FIXME calcolarlo con una funzione ad hoc
			throw new IllegalStateException("PreviousPersonDayInMonth va calcolato");
		}
		
		return this.previousPersonDayInMonth;
	}


}
