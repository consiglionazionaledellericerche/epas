package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import models.WorkingTimeType;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni di un 
 * orario di lavoro.
 * 
 * @author cristian
 *
 */
@StaticInject
@Data
public class WorkingTimeTypeShowTerseDto {

  private Long id;
  private String description;
  private Boolean horizontal;
  private OfficeDto office;
  private boolean disabled;
  private String externalId;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  public static WorkingTimeTypeShowTerseDto build(WorkingTimeType wtt) {
    return modelMapper.map(wtt, WorkingTimeTypeShowTerseDto.class);
  }
}