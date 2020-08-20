package jobs;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.WorkingTimeType;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async = true)
@Slf4j
public class WorkingTimeTypeAdjust extends Job<Void> {

  private final String maternita = "Maternita";
  private final String maternitaAccento = "Maternità";
  private final String maternitaLowerCase = "maternita";
  private final String maternitaLowerCaseAccento = "maternità";
  private final String allattamento = "Allattamento";
  private final String allattamentoLowerCase = "allattamento";
  
  @Override
  public void doJob() {
    List<WorkingTimeType> wttList = WorkingTimeType.findAll();
    for (WorkingTimeType wtt : wttList) {
      if ((wtt.description.contains(maternita) || wtt.description.contains(maternitaAccento) 
          || wtt.description.contains(maternitaLowerCase) 
          || wtt.description.contains(maternitaLowerCaseAccento)
          || wtt.description.contains(allattamento)
          || wtt.description.contains(allattamentoLowerCase)) 
          && wtt.enableAdjustmentForQuantity == true) {
        wtt.enableAdjustmentForQuantity = false;
        log.info("Messo a false il campo enableAdjustmentForQuantity per l'orario {} "
            + "della sede {}", wtt.description, wtt.office);
      }
      wtt.save();
    }
  }
}