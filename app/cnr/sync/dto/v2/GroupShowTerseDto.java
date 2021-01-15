package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.flows.Group;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in forma ridotta ed in Json per un gruppo di persone.
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
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un GroupShowTerseDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static GroupShowTerseDto build(Group group) {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowTerseDto.class);
    groupDto.setManager(PersonShowTerseDto.build(group.manager));
    return groupDto;
  }
}