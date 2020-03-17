package models.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * dto per il passaggio di informazioni dal manager al controller del calendario di reperibilità.
 * @author dario
 *
 */
@Builder
@Data
public class ReperibilityDto {

  private List<WorkDaysReperibilityDto> listWorkdaysRep;
  private List<HolidaysReperibilityDto> listHolidaysRep;
	
}
