package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.ContractManager;
import manager.PersonManager;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;
import dao.PersonDayDao;

/**
 * @author alessandro
 *
 */
public class WrapperContractMonthRecap implements IWrapperContractMonthRecap {

	private final ContractMonthRecap value;
	private final IWrapperContract contract;
	private Optional<ContractMonthRecap> previousRecap = null;
	private final IWrapperFactory wrapperFactory;  

	@Inject
	WrapperContractMonthRecap(@Assisted ContractMonthRecap cmr, 
			 IWrapperFactory wrapperFactory
			) {
		this.wrapperFactory = wrapperFactory;
		this.contract = wrapperFactory.create(cmr.contract);
		this.value = cmr;
	
	}

	@Override
	public ContractMonthRecap getValue() {
		return value;
	}
	
	@Override
	public IWrapperContract getContract() {
		return contract;
	}
	
	
	/**
	 * Il recap precedente se presente. Istanzia una variabile lazy.
	 * 
	 * @return
	 */
	@Override
	public Optional<ContractMonthRecap> getPreviousRecap() {
		
		if( this.previousRecap == null ) {
			
			this.previousRecap = wrapperFactory.create(value.contract)
					.getContractMonthRecap(new YearMonth(value.year, value.month)
					.minusMonths(1));
		}
		return this.previousRecap;
	}
	
	/**
	 * Se visualizzare il prospetto sul monte ore anno precedente.
	 * 
	 * @return
	 */
	@Override
	public boolean hasResidualLastYear() {
		
		return value.possibileUtilizzareResiduoAnnoPrecedente;
	}
	
	/**
	 * Il valore iniziale del monte ore anno precedente.
	 * 
	 * @return
	 */
	@Override
	public int getResidualLastYearInit() {
		
		if( ! hasResidualLastYear() ) {
			return 0;
		}
		//Preconditions.checkState(hasResidualLastYear());
		
		if( getPreviousRecap().isPresent() ) {
			
			if( value.month == 1) {
				return value.initMonteOreAnnoPassato;
			} else {
				return getPreviousRecap().get().remainingMinutesLastYear;
			}
			
		} else {
			return this.value.initMonteOreAnnoPassato;
		}
	}

}
