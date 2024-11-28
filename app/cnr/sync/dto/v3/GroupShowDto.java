package cnr.sync.dto.v3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import org.modelmapper.ModelMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cnr.sync.dto.v2.GroupShowTerseDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import common.injection.StaticInject;
import lombok.Data;
import lombok.val;
import models.flows.Group;

@StaticInject
@Data
public class GroupShowDto {

  private String name;
  private String description;
  private LocalDate endDate;
  private String manager;
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un GroupShowDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static GroupShowDto build(Group group) {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowDto.class);
    groupDto.setManager((group.getManager().getNumber()));
    return groupDto;
  }
}
