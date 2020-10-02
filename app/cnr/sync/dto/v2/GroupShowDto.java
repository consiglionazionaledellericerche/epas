package cnr.sync.dto.v2;

import com.beust.jcommander.internal.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = true)
public class GroupShowDto extends GroupShowTerseDto {
  
  private Set<PersonShowTerseDto> people = Sets.newHashSet();
  private OfficeDto office;
  
  public static GroupShowDto build(Group group) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowDto.class);
    groupDto.setManager(PersonShowTerseDto.build(group.manager));
    groupDto.setPeople(
        group.getPeople().stream().map(p -> PersonShowTerseDto.build(p))
          .collect(Collectors.toSet()));
    return groupDto;
  }
}