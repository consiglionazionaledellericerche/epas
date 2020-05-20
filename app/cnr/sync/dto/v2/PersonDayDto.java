package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import org.joda.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PersonDayDto {

  private LocalDate data;
  private String number;
  private int tempoLavoro;
  private int differenza;
  private int progressivo;
  private boolean buonoPasto;
  private boolean giornoLavorativo;
  @Builder.Default
  private List<StampingDto> timbrature = Lists.newArrayList();
  @Builder.Default
  private List<AbsenceDto> codiciAssenza = Lists.newArrayList();
}
