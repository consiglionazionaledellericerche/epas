package models.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import models.enumerate.TeleworkStampTypes;



@Data
@Builder
public class TeleworkDto {

  private String personDayId;
  
  private String stampType;
  
  private String date;
  
  private String note;
  
}
