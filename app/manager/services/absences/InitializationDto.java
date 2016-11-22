package manager.services.absences;

import org.testng.collections.Lists;

import play.data.validation.Max;
import play.data.validation.Min;

import java.util.List;

public class InitializationDto {
  
  @Min(0)
  public Integer takenHours = 0;
  @Min(0) @Max(59)
  public Integer takenMinutes = 0;
  @Min(0) @Max(99)
  public Integer takenUnits = 0;
  
  /**
   * I minuti inseribili...
   * @return list
   */
  public List<Integer> selectableMinutes() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 59; i++) {
      hours.add(i);
    }
    return hours;
  }
}