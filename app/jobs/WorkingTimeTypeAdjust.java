package jobs;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.WorkingTimeType;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async = true)
@Slf4j
public class WorkingTimeTypeAdjust extends Job<Void>{

  private final String MATERNITA = "Maternita";
  private final String MATERNITA_ACCENTO = "Maternità";
  private final String MATERNITA_LOWER_CASE = "maternita";
  private final String MATERNITA_LOWER_CASE_ACCENTO = "maternità";
  private final String ALLATTAMENTO = "Allattamento";
  private final String ALLATTAMENTO_LOWER_CASE = "allattamento";
  
  @Override
  public void doJob() {
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {
      if ((wtt.description.contains(MATERNITA) || wtt.description.contains(MATERNITA_ACCENTO) 
          || wtt.description.contains(MATERNITA_LOWER_CASE) 
          || wtt.description.contains(MATERNITA_LOWER_CASE_ACCENTO)
          || wtt.description.contains(ALLATTAMENTO)
          || wtt.description.contains(ALLATTAMENTO_LOWER_CASE)) &&
          wtt.enableAdjustmentForQuantity == true) {
        wtt.enableAdjustmentForQuantity = false;
        log.info("Messo a false il campo enableAdjustmentForQuantity per l'orario {} della sede {}", 
            wtt.description, wtt.office);
      }
      wtt.save();
    }
  }
}