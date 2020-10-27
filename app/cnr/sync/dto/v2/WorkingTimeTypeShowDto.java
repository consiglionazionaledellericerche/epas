package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.WorkingTimeType;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni di un 
 * orario di lavoro compreso gli orario giornalieri.
 * 
 * @author cristian
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkingTimeTypeShowDto extends WorkingTimeTypeShowTerseDto {

  private List<WorkingTimeTypeDayShowDto> workingTimeTypeDays;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  public static WorkingTimeTypeShowDto build(WorkingTimeType wtt) {
    return modelMapper.map(wtt, WorkingTimeTypeShowDto.class);
  }
}
