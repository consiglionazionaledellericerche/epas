package cnr.sync.dto.v2;

import org.joda.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificationAbsenceDto {

  public String code;
  public String from;
  public String to;
}
