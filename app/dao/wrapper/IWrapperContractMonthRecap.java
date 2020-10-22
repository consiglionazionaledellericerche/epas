package dao.wrapper;

import com.google.common.base.Optional;
import models.ContractMonthRecap;

/**
 * Contratto con alcune funzionalità aggiuntive.
 *
 * @author alessandro
 */
public interface IWrapperContractMonthRecap extends IWrapperModel<ContractMonthRecap> {

  public IWrapperContract getContract();

  public Optional<ContractMonthRecap> getPreviousRecap();
  
  public Optional<ContractMonthRecap> getPreviousRecapInYear();

  /**
   * Se visualizzare il prospetto sul monte ore anno precedente.
   */
  public boolean hasResidualLastYear();

  /**
   * Il valore iniziale del monte ore anno precedente.
   */
  public int getResidualLastYearInit();


}
