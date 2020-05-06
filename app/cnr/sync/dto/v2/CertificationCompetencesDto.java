package cnr.sync.dto.v2;

import org.joda.time.LocalDate;
import cnr.sync.dto.v2.CertificationAbsenceDto.CertificationAbsenceDtoBuilder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificationCompetencesDto {

  public String code;
  public Integer quantity;
}
