package cnr.sync.dto.v2;

import com.google.common.base.Verify;
import helpers.JodaConverters;
import lombok.Builder;
import models.Contract;
import models.Person;

/**
 * Dati per l'aggiornamento vai REST di un contratto di 
 * una persona.
 *  
 * @author cristian
 *
 */
@Builder
public class ContractUpdateDto extends ContractCreateDto {

  /**
   * Aggiorna i dati dell'oggetto contract passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Contract contract) {
    Verify.verifyNotNull(contract);
    Verify.verifyNotNull(contract.person);
    
    contract.person = Person.findById(getPersonId());
    contract.beginDate = JodaConverters.javaToJodaLocalDate(getBeginDate());
    contract.endDate = JodaConverters.javaToJodaLocalDate(getEndDate());
    contract.endContract = JodaConverters.javaToJodaLocalDate(getEndContract());
    contract.perseoId = getPerseoId();
    contract.onCertificate = getOnCertificate();
    
    if (getPreviousContractId() != null) {
      contract.setPreviousContract(Contract.findById(getPreviousContractId()));
    }
  }
}