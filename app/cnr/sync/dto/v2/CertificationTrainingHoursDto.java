package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;
import org.joda.time.LocalDate;

@Data
@Builder
public class CertificationTrainingHoursDto {

  public Integer quantity;
  public LocalDate from;
  public LocalDate to;
}
