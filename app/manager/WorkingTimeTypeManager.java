package manager;

import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

public class WorkingTimeTypeManager {

  public void saveWorkingTimeType(
      WorkingTimeTypeDay wttd, WorkingTimeType wtt, int dayOfWeek) {

    wttd.dayOfWeek = dayOfWeek;
    wttd.workingTimeType = wtt;
    wttd.save();
  }


}
