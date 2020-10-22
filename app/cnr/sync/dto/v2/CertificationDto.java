package cnr.sync.dto.v2;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CertificationDto {
  private String fullName;
  private String number;
  private int year;
  private int month;
  private List<CertificationAbsenceDto> absences;
  private List<CertificationCompetencesDto> competences;
  private List<CertificationMealTicketDto> mealTickets;
  private List<CertificationTrainingHoursDto> trainingHours;
}
