package cnr.sync.dto.v2;

import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.Office;
import models.Person;
import models.flows.Group;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione di una persona via REST.
 * 
 * @author cristian
 *
 */
@Data
public class GroupCreateDto {
  
  @Required
  private String name;
  private String description;
  private Boolean sendFlowsEmail;
  @Required
  private Long officeId;
  @Required
  private Long managerId;  
  private String externalId;
  private LocalDate endDate;
  
  /**
   * Nuova istanza di un oggetto person a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Group build(GroupCreateDto groupDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val group = modelMapper.map(groupDto, Group.class);
    if (groupDto.getOfficeId() != null) {
      group.office = Office.findById(groupDto.getOfficeId());  
    }
    if (groupDto.getManagerId() != null) {
      group.manager = Person.findById(groupDto.getManagerId());  
    }
    return group;
  }
}