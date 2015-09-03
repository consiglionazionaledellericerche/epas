package dao.wrapper;

import models.ContractMonthRecap;

import com.google.common.base.Optional;

/**
 * @author alessandro
 *
 */
public interface IWrapperContractMonthRecap extends IWrapperModel<ContractMonthRecap> {
	
	public IWrapperContract getContract();
	
	public Optional<ContractMonthRecap> getPreviousRecap();
	
	/**
	 * Se visualizzare il prospetto sul monte ore anno precedente.
	 * 
	 * @return
	 */
	public boolean hasResidualLastYear();
	
	/**
	 * Il valore iniziale del monte ore anno precedente.
	 * 
	 * @return
	 */
	public int getResidualLastYearInit();
		

}
