package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import org.joda.time.LocalDate;
import cnr.sync.dto.v2.CertificationDto.CertificationDtoBuilder;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CertificationObject {

  public List<CertificationAbsenceDto> absences = Lists.newArrayList();
  public List<CertificationCompetencesDto> competences = Lists.newArrayList();
  public List<CertificationMealTicketDto> tickets = Lists.newArrayList();
  public List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();
}
