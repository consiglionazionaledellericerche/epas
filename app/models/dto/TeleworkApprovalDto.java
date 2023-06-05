package models.dto;

import lombok.Builder;
import lombok.Data;
import models.TeleworkValidation;
import models.informationrequests.TeleworkRequest;

/**
 * Dto per l'approvazione delle richieste di telelavoro.
 *
 */
@Data
@Builder()
public class TeleworkApprovalDto {
  
  TeleworkRequest teleworkRequest;
  
  TeleworkValidation teleworkValidation;

}