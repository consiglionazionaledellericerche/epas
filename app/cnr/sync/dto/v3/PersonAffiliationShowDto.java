package cnr.sync.dto.v3;

import org.joda.time.LocalDate;
import lombok.Data;
import lombok.val;
import manager.recaps.personstamping.PersonStampingRecap;
import models.flows.Affiliation;

@Data
public class PersonAffiliationShowDto {

  private String number;
  private String beginDate;
  private String endDate;
  
public static PersonAffiliationShowDto build(Affiliation affiliation) {
    
    val personAffiliationShowDto = new PersonAffiliationShowDto();
    if (affiliation != null) {
      personAffiliationShowDto.setNumber(affiliation.getPerson().getNumber());
      personAffiliationShowDto.setBeginDate(affiliation.getBeginDate().toString());
      personAffiliationShowDto.setEndDate(affiliation.getEndDate() != null ? 
          affiliation.getEndDate().toString() : "");
    }
    return personAffiliationShowDto;
  }
}
