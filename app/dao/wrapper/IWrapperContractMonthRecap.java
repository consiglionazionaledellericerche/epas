package dao.wrapper;

import models.ContractMonthRecap;

/**
 * @author alessandro
 *
 */
public interface IWrapperContractMonthRecap extends IWrapperModel<ContractMonthRecap> {
	
	public IWrapperContract getContract();
	
	/**
	 * Riepilogo precedente. Inizializza una variabile lazy.
	 * @return
	 */
	public ContractMonthRecap getPreviousRecap();
	
}
