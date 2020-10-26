package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.val;
import org.testng.collections.Lists;

@AllArgsConstructor
@Builder
@Data
public class PersonMonthRecapDto {

  private Integer year;
  private Integer month;
  private PersonShowTerseDto person;

  @Builder.Default
  private List<PersonDayShowTerseDto> personDays = Lists.newArrayList();

  @Override
  public boolean equals(Object other) {
    if (! (other instanceof PersonMonthRecapDto)) {
      return false;
    }
    val otherRecap = (PersonMonthRecapDto) other;
    return (year != null && year.equals(otherRecap.getYear()) 
        && (month != null && month.equals(otherRecap.getMonth()))
        && (person != null && person.getId() != null && otherRecap.getPerson() != null 
        && person.getId().equals(otherRecap.getPerson().getId())));
  }

  @Override
  public int hashCode() {
    return person.hashCode() + year.hashCode() + month.hashCode();
  }
}
