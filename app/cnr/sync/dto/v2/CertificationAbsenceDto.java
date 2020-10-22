package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;
import org.joda.time.LocalDate;


@Data
@Builder
public class CertificationAbsenceDto {

  public String code;
  private Integer justifiedTime;
  private String justifiedType;
  public LocalDate from;
  public LocalDate to;
}
