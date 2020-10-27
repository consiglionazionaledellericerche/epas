package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.Contract;
import org.modelmapper.ModelMapper;

@StaticInject
@Data
public class ContractShowTerseDto {
 
  private Long id;
  private PersonShowTerseDto person;
  private LocalDate beginDate;
  private LocalDate endDate;
  private LocalDate endContract;
  
  private Boolean onCertificate;
  private ContractShowTerseDto previousContract;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un ContractShowTerseDto contenente i valori 
   * dell'oggetto contract passato.
   */
  public static ContractShowTerseDto build(Contract contract) {
    val contractDto = modelMapper.map(contract, ContractShowTerseDto.class);
    contractDto.setPerson(PersonShowTerseDto.build(contract.person));
    if (contract.getPreviousContract() != null) {
      contractDto.setPreviousContract(ContractShowTerseDto.build(contract.getPreviousContract()));
    }
    return contractDto;
  }
}
