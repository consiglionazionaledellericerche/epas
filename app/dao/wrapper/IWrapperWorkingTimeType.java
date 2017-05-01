package dao.wrapper;

import java.util.List;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.WorkingTimeType;

public interface IWrapperWorkingTimeType extends IWrapperModel<WorkingTimeType> {

  /**
   * I contratti attivi che attualmente hanno impostato il WorkingTimeType.
   */
  List<Contract> getAssociatedActiveContract(Office office);

  /**
   * Ritorna i periodi con questo tipo orario appartenti a contratti attualmente attivi.
   */
  List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(Office office);

  /**
   * Tutti i contratti associati al tipo orario. 
   */
  List<Contract> getAssociatedContract();


}
