package cnr.sync.dto.v2;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Informazioni esportate in Json per l'ufficio.
 * 
 * @author cristian
 *
 */
@Data
public class OfficeDto {

  private int id;
  private String name;
  private String code;
  private String codeId;
  private LocalDateTime updatedAt;
}