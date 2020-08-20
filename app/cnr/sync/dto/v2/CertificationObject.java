package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.joda.time.LocalDate;



@Builder
@Data
public class CertificationObject {

  public List<CertificationAbsenceDto> absences = Lists.newArrayList();
  public List<CertificationCompetencesDto> competences = Lists.newArrayList();
  public List<CertificationMealTicketDto> tickets = Lists.newArrayList();
  public List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();
}
