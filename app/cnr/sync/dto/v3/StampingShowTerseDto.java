package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.ToString;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.StampTypes;
import org.modelmapper.ModelMapper;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di una timbratura.
 * @version 3
 * 
 * @author cristian
 *
 */
@StaticInject
@ToString
@Data
public class StampingShowTerseDto {

  private Long id;
  private LocalDateTime date;
  private WayType way;
  private StampTypes stampType;
  private String place;
  private String reason;
  private boolean markedByAdmin;
  private boolean markedByEmployee;
  private String note;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un StampingShowTerseDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static StampingShowTerseDto build(Stamping stamping) {
    return modelMapper.map(stamping, StampingShowTerseDto.class);    
  }
}