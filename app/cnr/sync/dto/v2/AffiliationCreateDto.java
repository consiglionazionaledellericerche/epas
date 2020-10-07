package cnr.sync.dto.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.Person;
import models.flows.Affiliation;
import models.flows.Group;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di una affiliazione di una persona ad
 * un gruppo.
 * 
 * @author cristian
 *
 */
@Data
public class AffiliationCreateDto {
  
  @Required
  private Long groupId;
  @Required
  private Long personId;  
  private BigDecimal percentage;
  @Required
  private LocalDate beginDate;
  private LocalDate endDate;
  
  /**
   * Nuova istanza di un oggetto affiliation a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Affiliation build(AffiliationCreateDto affiliationDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val affiliation = modelMapper.map(affiliationDto, Affiliation.class);
    if (affiliationDto.getGroupId() != null) {
      affiliation.setGroup(Group.findById(affiliationDto.getGroupId()));  
    }
    if (affiliationDto.getPersonId() != null) {
      affiliation.setPerson(Person.findById(affiliationDto.getPersonId()));  
    }
    return affiliation;
  }
}