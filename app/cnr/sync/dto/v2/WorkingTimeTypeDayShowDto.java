package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import models.WorkingTimeTypeDay;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per le impostazioni giornaliere di un 
 * orario di lavoro.
 * 
 * @author cristian
 *
 */
@Data
public class WorkingTimeTypeDayShowDto {

  private Long id;
  private int dayOfWeek;
  private Integer workingTime;
  private boolean holiday;
  private Integer breakTicketTime;
  private Integer ticketAfternoonThreshold;
  private Integer ticketAfternoonWorkingTime;
  private Integer timeMealFrom;
  private Integer timeMealTo;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  public WorkingTimeTypeDayShowDto build(WorkingTimeTypeDay wttd) {
    return modelMapper.map(wttd, WorkingTimeTypeDayShowDto.class);
  }
}
