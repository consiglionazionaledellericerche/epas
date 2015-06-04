package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.PersonManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;
import dao.PersonDayDao;

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

		if( ! getPersonDayContract().isPresent() ) {
			this.previousForProgressive = Optional.absent();
			return this.previousForProgressive;
		}

		List<PersonDay> personDayInMonthAsc;

		//Assegnare logicamente il previousForProgressive
		if( this.value.date.getDayOfMonth() == 1) {
			this.previousForProgressive = Optional.absent();
			return this.previousForProgressive;
		}

		//if( ! optPersonDayInMonthAsc.isPresent() ) {

		personDayInMonthAsc = personDayDao.getPersonDayInMonth(this.value.person, 
				new YearMonth(this.value.date));
		//}
		//else {
		//	personDayInMonthAsc = optPersonDayInMonthAsc.get();
		//}

		for(int i = 1; i < personDayInMonthAsc.size(); i++) {
			PersonDay current = personDayInMonthAsc.get(i);
			PersonDay previous = personDayInMonthAsc.get(i-1);
			if(current.id.equals(this.value.id)) {
				this.previousForProgressive = Optional.fromNullable(previous);
			}
		}
		if(this.previousForProgressive == null) {
			this.previousForProgressive = Optional.absent();
		}

		//Se il giorno precedente non appartiene allo stesso contratto non lo considero
		//valido come progressivo e lo fisso come absent.
		if( this.previousForProgressive.isPresent() ) {

			if( !DateUtility.isDateIntoInterval(this.previousForProgressive.get().date,
					factory.create(this.getPersonDayContract().get()).getContractDateInterval() )) {
				this.previousForProgressive = Optional.absent();
			}
		}

		return this.previousForProgressive;
	}

	/**
	 * Il personDay precedente solo se immediatamente consecutivo. Altrimenti
	 * absent().
	 * 
	 * @return
	 */
	public Optional<PersonDay> getPreviousForNightStamp() {

		if( this.previousForNightStamp != null ) {
			return this.previousForNightStamp;
		}

		LocalDate realPreviousDate = this.value.date.minusDays(1);

		//caso semplice da previousForProgressive
		if(this.getPreviousForProgressive().isPresent() ) {

			if(this.previousForProgressive.get().date.isEqual(realPreviousDate)) {			
				this.previousForNightStamp = this.previousForProgressive;
				return this.previousForProgressive;
			}
		}

		PersonDay firstPrevious = personDayDao
				.getPreviousPersonDay(this.value.person, this.value.date);

		//primo giorno del contratto
		if( firstPrevious == null ) { 
			this.previousForNightStamp = Optional.absent();
			return this.previousForNightStamp;
		}

		//giorni non consecutivi
		if( ! firstPrevious.date.isEqual(realPreviousDate) ) {
			this.previousForNightStamp = Optional.absent();
			return this.previousForNightStamp;
		}

		this.previousForNightStamp = Optional.fromNullable(firstPrevious);
		return this.previousForNightStamp;
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

				if(DateUtility.isDateIntoInterval(this.value.date, factory.create(cwtt).getDateInverval())) {

					return Optional.fromNullable(
							cwtt.workingTimeType.workingTimeTypeDays
							.get(this.value.date.getDayOfWeek() - 1));
				}

			}
		}

		return Optional.absent();
	}

}
