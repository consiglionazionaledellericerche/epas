package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dao.ContractDao;
import dao.PersonDayDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.PersonManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeTypeDay;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

/**
 * @author alessandro
 *
 */
public class WrapperPersonDay implements IWrapperPersonDay {

	private final PersonDay value;

	private Optional<PersonDay> previousForProgressive = null;
	private Optional<PersonDay> previousForNightStamp = null;
	private Optional<Contract> personDayContract = null;
	private Boolean isFixedTimeAtWorkk = null;
	private Optional<WorkingTimeTypeDay> workingTimeTypeDay = null;

	private final ContractDao contractDao;
	private final PersonDayDao personDayDao;
	private final IWrapperFactory factory;

	@Inject
	WrapperPersonDay(@Assisted PersonDay pd,ContractDao contractDao,
			PersonManager personManager,PersonDayDao personDayDao,
			IWrapperFactory factory) {
		this.value = pd;
		this.contractDao = contractDao;
		this.personDayDao = personDayDao;
		this.factory = factory;
	}

	@Override
	public PersonDay getValue() {
		return value;
	}

	/**
	 * Il personDay precedente per il calcolo del progressivo.
	 * 
	 * @return
	 */
	public Optional<PersonDay> getPreviousForProgressive() {

		if(this.previousForProgressive != null ) {
			return this.previousForProgressive;
		}
		
		setPreviousForProgressive(Optional.<PersonDay>absent());
		return this.previousForProgressive;
	}
	
	
	/**
	 * Il personDay precedente solo se immediatamente consecutivo. Altrimenti
	 * absent().
	 * 
	 * @return
	 */
	public Optional<PersonDay> getPreviousForNightStamp() {
	
		
		if(this.previousForNightStamp != null ) {
			return this.previousForNightStamp;
		}
		
		setPreviousForNightStamp(Optional.<PersonDay>absent());
		return this.previousForNightStamp;

	}

	public void setPreviousForProgressive(Optional<PersonDay> potentialOnlyPrevious) {
		
		this.previousForProgressive = Optional.<PersonDay>absent();
		
		if( ! getPersonDayContract().isPresent() ) {
			return;
		}
		
		//Assegnare logicamente il previousForProgressive
		if( this.value.date.getDayOfMonth() == 1) {
			//Primo giorno del mese
			return;
		}
		
		
		if( this.getPersonDayContract().get().sourceDateResidual != null &&
				this.getPersonDayContract().get().sourceDateResidual
				.isEqual(this.value.date.minusDays(1)) ) {
			//Giorno successivo all'inizializzazione
			return;
		}
		
		PersonDay candidate = null;
		
		if( potentialOnlyPrevious.isPresent() ) {
			candidate = potentialOnlyPrevious.get();
			
		} else {
			
			List<PersonDay> personDayInMonthAsc = personDayDao
					.getPersonDayInMonth(this.value.person, 
					new YearMonth(this.value.date));
			for(int i = 1; i < personDayInMonthAsc.size(); i++) {
				PersonDay current = personDayInMonthAsc.get(i);
				PersonDay previous = personDayInMonthAsc.get(i-1);
				if(current.id.equals(this.value.id)) {
					candidate = previous;
				}
			}
		}
		if( candidate == null ) {
			return;
		}
		
		//Non stesso contratto 
		// TODO: (equivalente a caso this.value.equals(begincontract)
		if( !DateUtility.isDateIntoInterval(candidate.date,
				factory.create(this.getPersonDayContract().get()).getContractDateInterval() )) {
			return;
		}
		this.previousForProgressive = Optional.fromNullable(candidate);

	}
	
	public void setPreviousForNightStamp(Optional<PersonDay> potentialOnlyPrevious) {
		
		this.previousForNightStamp = Optional.absent();
		
		if( ! getPersonDayContract().isPresent() ) {
			return;
		}
		
		LocalDate realPreviousDate = this.value.date.minusDays(1);
		
		PersonDay candidate = null;
		
		if(potentialOnlyPrevious.isPresent()) {
			candidate = potentialOnlyPrevious.get();
		} else {
			
			candidate = personDayDao
					.getPreviousPersonDay(this.value.person, this.value.date);
		}
		
		//primo giorno del contratto
		if( candidate == null ) { 
			return;
		}
	
		//giorni non consecutivi
		if( ! candidate.date.isEqual(realPreviousDate) ) {
			return;
		}
	
		this.previousForNightStamp = Optional.fromNullable(candidate);
	}

	/**
	 * 
	 */
	public Optional<Contract> getPersonDayContract() {

		if(this.personDayContract != null) {
			return this.personDayContract;
		}

		Contract contract = contractDao.getContract(this.value.date, this.value.person);

		if(contract == null) {
			this.personDayContract = Optional.absent();
			return this.personDayContract;
		}

		this.personDayContract = Optional.fromNullable(contract);

		return this.personDayContract;
	}

	/** 
	 * True se il personDay cade in uno stampProfile con fixedTimeAtWork = true;
	 * 
	 * @return
	 */
	public boolean isFixedTimeAtWork()
	{
		if( this.isFixedTimeAtWorkk != null ) {
			return this.isFixedTimeAtWorkk;
		}

		this.isFixedTimeAtWorkk = false;

		Optional<Contract> contract = getPersonDayContract();

		if( ! contract.isPresent() ) {
			return this.isFixedTimeAtWorkk;
		}

		for(ContractStampProfile csp : contract.get().contractStampProfile) {

			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);

			if(DateUtility.isDateIntoInterval(this.value.date, cspInterval) ){
				this.isFixedTimeAtWorkk = csp.fixedworkingtime;
				return this.isFixedTimeAtWorkk;
			}
		}

		return this.isFixedTimeAtWorkk;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay() {

		if(this.workingTimeTypeDay != null) {
			return this.workingTimeTypeDay;
		}

		if( getPersonDayContract().isPresent() ) {
		    
			for(ContractWorkingTimeType cwtt : 
				this.getPersonDayContract().get().contractWorkingTimeType ) {

				if(DateUtility.isDateIntoInterval(this.value.date, 
						factory.create(cwtt).getDateInverval())) {
					
					WorkingTimeTypeDay wttd = cwtt.workingTimeType.workingTimeTypeDays
							.get(this.value.date.getDayOfWeek() - 1);
					
					Preconditions.checkState(wttd.dayOfWeek == value.date.getDayOfWeek());
					return Optional.fromNullable(wttd);
				}

			}
		}
		return Optional.absent();
	}
	
	/**
	 * L'ultima timbratura in ordine di tempo nel giorno.
	 * @return
	 */
	public Stamping getLastStamping() { 
		Stamping last = null;
		for(Stamping s : value.stampings) {
			if(last == null) {
				last = s;
			} else if(last.date.isBefore(s.date)) {
				last = s;
			}
		}
		return last;
	}

}
