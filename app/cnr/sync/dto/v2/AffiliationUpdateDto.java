package cnr.sync.dto.v2;

import lombok.Builder;
import models.Person;
import models.flows.Affiliation;
import models.flows.Group;

/**
 * Dati per l'aggiornamento vai REST di una affiliazione di 
 * una persona ad un gruppo.
 *  
 * @author cristian
 *
 */
@Builder
public class AffiliationUpdateDto extends AffiliationCreateDto {

  /**
   * Aggiorna i dati dell'oggetto affiliation passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Affiliation affiliation) {
    if (getGroupId() != null) {
      affiliation.setGroup(Group.findById(getGroupId()));
    }
    if (getPersonId() != null) {
      affiliation.setPerson(Person.findById(getPersonId()));  
    }
    affiliation.setPercentage(getPercentage());
    affiliation.setBeginDate(getEndDate());
    affiliation.setEndDate(getEndDate());
  }
}