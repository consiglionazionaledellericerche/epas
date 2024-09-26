package cnr.sync.dto.v3;

import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import lombok.Data;
import lombok.val;
import models.Configuration;
import models.PersonConfiguration;

@Data
public class PersonConfigurationShowDto {

  private String epasParam;
  private String fieldValue;
  private LocalDate beginDate;
  private LocalDate endDate;
  
  public static PersonConfigurationShowDto build(PersonConfiguration personConfiguration) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val configurationDto = modelMapper.map(personConfiguration, PersonConfigurationShowDto.class);
    if (personConfiguration.getEpasParam() != null) {
      configurationDto.setEpasParam(personConfiguration.getEpasParam().name);
    }
    return configurationDto;
  }
}
