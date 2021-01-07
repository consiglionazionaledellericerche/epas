package cnr.sync.dto.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  private GroupShowTerseDto group;
  private PersonShowTerseDto person;
  private BigDecimal percentage;
  private LocalDate beginDate;
  private LocalDate endDate;
  private String externalId;
  private LocalDateTime updatedAt;

  /**
   * Nuova instanza di un GroupShowTerseDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static AffiliationShowDto build(Affiliation affiliation) {
    val modelMapper = new ModelMapper();
    val affilationDto = modelMapper.map(affiliation, AffiliationShowDto.class);
    affilationDto.setGroup(GroupShowTerseDto.build(affiliation.getGroup()));
    affilationDto.setPerson(PersonShowTerseDto.build(affiliation.getPerson()));
    return affilationDto;
  }
}