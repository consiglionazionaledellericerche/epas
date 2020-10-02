package cnr.sync.dto.v2;

import lombok.Data;
import models.Person;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per la Persona.
 * 
 * @author cristian
 *
 */
@Data
public class PersonShowTerseDto {
  
  private Long id;
  private String fullname;
  private String fiscalCode;
  private String email;
  private String number; //Matricola
  private String eppn;
  
  public static PersonShowTerseDto build(Person person) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    return modelMapper.map(person, PersonShowTerseDto.class);
  }
}
