package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.absences.Absence;
import org.modelmapper.ModelMapper;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di un'assenza.
 * @since versione 3 dell'API REST
 * 
 * @author cristian
 *
 */
@Data
public class AbsenceShowTerseDto {

  private Long id;
  private LocalDate date;
  private String code;
  private Integer justifiedTime;
  private String justifiedType;
  private String note;
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un AbsenceShowTerseDto contenente i valori 
   * dell'oggetto absence passato.
   */
  public static AbsenceShowTerseDto build(Absence absence) {
    val absenceDto = modelMapper.map(absence, AbsenceShowTerseDto.class);
    absenceDto.setJustifiedTime(absence.justifiedTime());
    absenceDto.setJustifiedType(absence.justifiedType.name.name());
    return absenceDto;
  }
}