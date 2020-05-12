package models.dto;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import models.PersonDay;
import models.TeleworkStamping;

@Builder
@Data
public class TeleworkPersonDayDto {

  public PersonDay personDay;
  public List<TeleworkStamping> beginEnd = Lists.newArrayList();
  public List<TeleworkStamping> meal = Lists.newArrayList();
  public List<TeleworkStamping> interruptions = Lists.newArrayList();
  
  public boolean isBeginEndComplete() {
    return !beginEnd.isEmpty() && beginEnd.size() % 2 == 0;
  }
  
  public boolean isMealComplete() {
    return !meal.isEmpty() && meal.size() % 2 == 0;
  }
  
}
