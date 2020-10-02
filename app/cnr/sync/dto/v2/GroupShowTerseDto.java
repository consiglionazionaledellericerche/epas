package cnr.sync.dto.v2;

import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.flows.Group;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per un gruppo di persone.
 * 
 * @author cristian
 *
 */ 
@Data
public class GroupShowTerseDto {
  
  private Long id;
  private String name;
  private String description;
  private LocalDate endDate;
  private PersonShowTerseDto manager;
  
  public static GroupShowTerseDto build(Group group) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowTerseDto.class);
    groupDto.setManager(PersonShowTerseDto.build(group.manager));
    return groupDto;
  }
}