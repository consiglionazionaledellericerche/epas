package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CertificationObject {

  @Builder.Default
  public List<CertificationAbsenceDto> absences = Lists.newArrayList();
  @Builder.Default
  public List<CertificationCompetencesDto> competences = Lists.newArrayList();
  @Builder.Default
  public List<CertificationMealTicketDto> tickets = Lists.newArrayList();
  @Builder.Default
  public List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();
}
