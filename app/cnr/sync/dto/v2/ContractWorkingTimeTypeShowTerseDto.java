package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.Data;
import models.ContractWorkingTimeType;
import org.modelmapper.ModelMapper;

@StaticInject
@Data
public class ContractWorkingTimeTypeShowTerseDto {

  private LocalDate beginDate;
  private LocalDate endDate;
  private WorkingTimeTypeShowTerseDto workingTimeType;
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  public static ContractWorkingTimeTypeShowTerseDto build(ContractWorkingTimeType cwtt) {
    return modelMapper.map(cwtt, ContractWorkingTimeTypeShowTerseDto.class);
  }
}
