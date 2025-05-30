package cnr.sync.dto.v3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import org.modelmapper.ModelMapper;
import org.testng.collections.Lists;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cnr.sync.dto.v2.GroupShowTerseDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import common.injection.StaticInject;
import dao.AffiliationDao;
import lombok.Data;
import lombok.val;
import models.flows.Affiliation;
import models.flows.Group;

@StaticInject
@Data
public class GroupShowDto {

  private String name;
  private String description;
  private String endDate;
  private String manager;
  private List<PersonAffiliationShowDto> list;
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  @Inject
  static AffiliationDao affiliationDao;
  
  /**
   * Nuova instanza di un GroupShowDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static GroupShowDto build(Group group) {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowDto.class);
    groupDto.setManager((group.getManager().getNumber()));
    List<PersonAffiliationShowDto> list = Lists.newArrayList();
    for (Affiliation affiliation: affiliationDao.byGroup(group)) {
      list.add(PersonAffiliationShowDto.build(affiliation));
    }
    groupDto.setList(list);
    return groupDto;
  }
}
