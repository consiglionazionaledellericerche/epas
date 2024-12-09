package cnr.sync.dto.v3;

import java.time.LocalDateTime;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import lombok.Data;
import lombok.ToString;
import lombok.val;
import models.Contract;

@Data
@ToString
public class ContractTerseDto {

  private String number;
  private String name;
  private String surname;
  private String email;
  private String beginDate;
  private String endDate;
  private int qualification;
  
  public static ContractTerseDto build (Contract contract) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val contractDto = modelMapper.map(contract, ContractTerseDto.class);
    if (contract.getPerson() != null) {
      contractDto.setNumber(contract.getPerson().getNumber());
      contractDto.setEmail(contract.getPerson().getEmail());
      contractDto.setName(contract.getPerson().getName());
      contractDto.setSurname(contract.getPerson().getSurname());
      contractDto.setQualification(contract.getPerson().getQualification().getQualification());
    }
    return contractDto;
  }
}
