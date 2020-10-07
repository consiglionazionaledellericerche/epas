package cnr.sync.dto.v2;

import lombok.Builder;
import models.Office;
import models.Person;
import models.flows.Group;

/**
 * Dati per l'aggiornamento di un gruppo via REST.
 * 
 * @author cristian
 *
 */
@Builder
public class GroupUpdateDto extends GroupCreateDto {

  /**
   * Aggiorna i dati dell'oggetto Group passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Group group) {
    group.name = getName();
    group.description = getDescription();
    group.sendFlowsEmail = getSendFlowsEmail();
    if (getOfficeId() != null) {
      group.office = Office.findById(getOfficeId());
    }
    if (getManagerId() != null) {
      group.manager = Person.findById(getManagerId());  
    }

  }
}