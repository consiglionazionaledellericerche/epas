package cnr.sync.dto.v2;

import com.beust.jcommander.internal.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Person;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per la Persona.
 * 
 * @author cristian
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonShowDto extends PersonShowTerseDto {
  
  private String name;
  private String surname;
  private String othersSurnames;
  private String telephone;
  private String fax;
  private String mobile;
  private Integer qualification;
  private Set<String> badges = Sets.newHashSet();
  private OfficeDto office;

  /**
   * Nuova instanza di un PersonShowDto contenente i valori 
   * dell'oggetto person passato.
   */
  public static PersonShowDto build(Person person) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    modelMapper.typeMap(Person.class, PersonShowDto.class).addMappings(mapper -> {
      mapper.map(src -> src.getQualification().getQualification(),
          PersonShowDto::setQualification);
    });
    val personDto = modelMapper.map(person, PersonShowDto.class);
    personDto.setBadges(person.getBadges().stream().map(b -> b.code).collect(Collectors.toSet()));
    return personDto;
    
  }
}
