package models.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import models.TeleworkValidation;
import models.dto.TeleworkDto.TeleworkDtoBuilder;
import models.enumerate.TeleworkStampTypes;
import models.informationrequests.TeleworkRequest;

@Data
@Builder()
public class TeleworkApprovalDto {
  
  TeleworkRequest teleworkRequest;
  
  TeleworkValidation teleworkValidation;

}
