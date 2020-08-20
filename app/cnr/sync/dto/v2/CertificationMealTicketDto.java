package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;
import org.joda.time.LocalDate;

@Data
@Builder
public class CertificationMealTicketDto {

  public Integer quantity;
}
