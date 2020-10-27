package cnr.sync.dto.v2;

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Contract;
import org.modelmapper.ModelMapper;

@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractShowDto extends ContractShowTerseDto {

  private Set<ContractWorkingTimeTypeShowTerseDto> workingTimeTypes = Sets.newHashSet();

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un ContractShowDto contenente i valori 
   * dell'oggetto contract passato.
   */
  public static ContractShowDto build(Contract contract) {
    val contractDto = modelMapper.map(contract, ContractShowDto.class);
    contractDto.setPerson(PersonShowTerseDto.build(contract.person));
    if (contract.getPreviousContract() != null) {
      contractDto.setPreviousContract(ContractShowTerseDto.build(contract.getPreviousContract()));
    }
    contractDto.setWorkingTimeTypes(
        contract.contractWorkingTimeType.stream()
          .map(ContractWorkingTimeTypeShowTerseDto::build)
          .collect(Collectors.toSet()));
    return contractDto;
  }
}
