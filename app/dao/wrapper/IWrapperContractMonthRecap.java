package dao.wrapper;

import models.ContractMonthRecap;

/**
 * @author alessandro
 *
 */
public interface IWrapperContractMonthRecap extends IWrapperModel<ContractMonthRecap> {
	
	public IWrapperContract getContract();
	
}
