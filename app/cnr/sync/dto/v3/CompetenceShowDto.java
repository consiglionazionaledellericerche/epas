package cnr.sync.dto.v3;

import javax.inject.Inject;
import org.modelmapper.ModelMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;

@StaticInject
@ToString
@Data
public class CompetenceShowDto {

  private int year;
  private int month;
  private String personNumber;
  private String code;
  private int valueApproved; 
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un StampingShowTerseDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static CompetenceShowDto build(Competence competence) {
    val competenceShowDto = modelMapper.map(competence, CompetenceShowDto.class);
    competenceShowDto.setPersonNumber(competence.getPerson().getNumber());
    return competenceShowDto;
  }
}
