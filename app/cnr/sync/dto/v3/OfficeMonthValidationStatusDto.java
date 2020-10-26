package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import java.util.List;
import lombok.Data;
import org.testng.collections.Lists;

@Data
public class OfficeMonthValidationStatusDto {

  private List<PersonShowTerseDto> validatedPersons = Lists.newArrayList();
  private List<PersonShowTerseDto> notValidatedPersons = Lists.newArrayList();

  boolean allCertificationsValidated;
  
  public boolean isAllCertificationsValidated() {
    return !validatedPersons.isEmpty() && notValidatedPersons.isEmpty();
  }
}
