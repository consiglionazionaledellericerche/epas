package cnr.sync.dto.v2;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.StampTypes;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * principali di una timbratura.
 * @version 2
 * 
 * @author cristian
 *
 */
@ToString
@Builder
@Data
public class StampingDto {

  private String date;
  private WayType way;
  private StampTypes stampType;
  private String place;
  private String reason;
  private boolean markedByAdmin;
  private boolean markedByEmployee;
  private String note;

  /**
   * Nuova instanza di un StampingDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static StampingDto build(Stamping stamping) {
    return StampingDto.builder()
        .date(stamping.date.toString())
        .way(stamping.way)
        .place(stamping.place)
        .reason(stamping.reason)
        .markedByAdmin(stamping.markedByAdmin)
        .markedByEmployee(stamping.markedByAdmin)
        .note(stamping.note)        
        .build();
  }
}