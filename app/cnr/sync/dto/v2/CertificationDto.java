package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import cnr.sync.dto.v2.PersonDayDto.PersonDayDtoBuilder;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CertificationDto {
  
  private List<CertificationAbsenceDto> absences;
  private List<CertificationCompetencesDto> competences;
  private List<CertificationMealTicketDto> tickets;
  private List<CertificationTrainingHoursDto> trainingHours;
}
