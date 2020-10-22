package cnr.sync.dto.v2;

import lombok.Builder;
import models.Office;
import models.Person;
import models.Qualification;

/**
 * Dati per la creazione di una persona via REST.
 * 
 * @author cristian
 *
 */
@Builder
public class PersonUpdateDto extends PersonCreateDto {

  /**
   * Aggiorna i dati dell'oggetto Person passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Person person) {
    person.name = getName();
    person.surname = getSurname();
    person.othersSurnames = getOthersSurnames();
    person.fiscalCode = getFiscalCode();
    person.email = getEmail();
    person.number = getNumber();
    person.eppn = getEppn();
    person.telephone = getTelephone();
    person.fax = getFax();
    person.mobile = getMobile();
    if (getQualification() != null) {
      person.qualification = 
          ((Qualification) Qualification.findAll().stream()
              .filter(q -> ((Qualification) q).qualification == getQualification().intValue())
              .findFirst().get());        
    }
    if (getOfficeId() != null) {
      person.office = Office.findById(getOfficeId());  
    }

  }
}