package dao.wrapper;

import java.util.List;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.TimeSlot;

public interface IWrapperTimeSlot extends IWrapperModel<TimeSlot> {

  /**
   * I contratti attivi che attualmente hanno impostato il TimeSlot.
   */
  List<Contract> getAssociatedActiveContract(Office office);

  /**
   * Ritorna i periodi con questo tipo orario appartenti a contratti attualmente attivi.
   */
  List<ContractMandatoryTimeSlot> getAssociatedPeriodInActiveContract(Office office);

  /**
   * Tutti i contratti associati alla fascia di orario lavorativo. 
   */
  List<Contract> getAssociatedContract();

}