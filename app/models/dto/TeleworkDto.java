package models.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import models.enumerate.TeleworkStampTypes;



@Data
@Builder
public class TeleworkDto {
  
  private Long id;

  private long personDayId;
  
  private String stampType;
  
  private String date;
  
  private String note;
  
  @Override
  public String toString() {
    return "Id timbratura: "+ id + ", PersonDayId: " + personDayId + ", Causale: " 
        + stampType + ", Data: " + date + ", Note: " + note;
  }
}
