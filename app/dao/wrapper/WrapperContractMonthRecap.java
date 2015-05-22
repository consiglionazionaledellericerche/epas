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

	@Inject
	WrapperContractMonthRecap(@Assisted ContractMonthRecap cmr, ContractManager contractManager,
			 IWrapperFactory wrapperFactory
			) {
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
}
