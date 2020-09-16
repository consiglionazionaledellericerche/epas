package cnr.sync.dto.v2;

import lombok.Data;
import models.Office;
import models.Person;
import models.Qualification;
import org.modelmapper.ModelMapper;

/**
 * Dati per la creazione di una persona via REST.
 * 
 * @author cristian
 *
 */
@Data
public class PersonCreateDto {
  
  private String name;
  private String surname;
  private String othersSurnames;
  private String fiscalCode;
  private String email;
  private String number; //Matricola
  private String eppn;
  private String telephone;
  private String fax;
  private String mobile;
  private Integer qualification;
  private Long officeId;
  
  public static Person build(PersonCreateDto person) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    modelMapper.typeMap(PersonCreateDto.class, Person.class).addMappings(mapper -> {
      mapper.map(src -> src.getQualification(),
          (dest, v) -> dest.qualification = Qualification.findById(v));
      mapper.map(src -> src.getOfficeId(), 
          (dest, officeId) -> dest.office = Office.findById(officeId));
    });
    return  modelMapper.map(person, Person.class);
  }
}