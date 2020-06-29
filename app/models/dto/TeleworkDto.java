package models.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeleworkDto {

  private Long personDayId;
  
  private String stampType;
  
  private LocalDateTime date;
  
  private String note;
  
}
