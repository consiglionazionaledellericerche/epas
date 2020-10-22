package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;
import models.absences.Absence;
import org.joda.time.LocalDate;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di un'assenza.
 * @since versione 2 dell'API REST
 * 
 * @author cristian
 *
 */
@Builder
@Data
public class AbsenceDto {

  private LocalDate date;
  private String code;
  private Integer justifiedTime;
  private String justifiedType;
  private String note;
  
  public static AbsenceDto build(Absence absence) {
    return AbsenceDto.builder()
        .date(absence.date)
        .code(absence.getCode())
        .justifiedTime(absence.justifiedTime())
        .justifiedType(absence.justifiedType.name.name())
        .note(absence.note)
        .build();
  }
}