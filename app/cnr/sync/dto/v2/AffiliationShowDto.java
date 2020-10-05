package cnr.sync.dto.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.flows.Affiliation;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per l'affiliazione di una persona ad un gruppo.
 * 
 * @author cristian
 *
 */ 
@Data
public class AffiliationShowDto {
  
  private Long id;
  private Long groupId;
  private Long personId;
  private BigDecimal percentage;
  private LocalDate beginDate;
  private LocalDate endDate;

  /**
   * Nuova instanza di un GroupShowTerseDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static AffiliationShowDto build(Affiliation affiliation) {
    val modelMapper = new ModelMapper();
    return modelMapper.map(affiliation, AffiliationShowDto.class);
  }
}