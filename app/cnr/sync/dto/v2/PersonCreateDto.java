package cnr.sync.dto.v2;

import lombok.Data;
import lombok.val;
import models.Office;
import models.Person;
import models.Qualification;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione di una persona via REST.
 * 
 * @author cristian
 *
 */
@Data
public class PersonCreateDto {
  
  @Required
  private String name;
  @Required
  private String surname;
  private String othersSurnames;
  private String fiscalCode;
  @Required
  private String email;
  private String number; //Matricola
  private String eppn;
  private String telephone;
  private String fax;
  private String mobile;
  @Required
  private Integer qualification;
  @Required
  private Long officeId;
  
  /**
   * Nuova istanza di un oggetto person a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Person build(PersonCreateDto personDto) {
    ModelMapper modelMapper = new ModelMapper();
    val person = modelMapper.map(personDto, Person.class);
    if (personDto.getQualification() != null) {
      person.qualification = 
          ((Qualification) Qualification.findAll().stream()
              .filter(q -> 
                ((Qualification) q).qualification == personDto.getQualification().intValue())
              .findFirst().get());        
    }
    if (personDto.getOfficeId() != null) {
      person.office = Office.findById(personDto.getOfficeId());  
    }
    person.beginDate = LocalDate.now();
    return person;
  }
}