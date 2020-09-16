package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificationCompetencesDto {

  public String code;
  public Integer quantity;
}
