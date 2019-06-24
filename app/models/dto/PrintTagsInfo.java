package models.dto;

import dao.history.HistoryValue;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import models.Stamping;
import org.assertj.core.util.Lists;

@Data
@Builder
public class PrintTagsInfo {

  public Person person;
  
  public PersonStampingRecap psDto;
  
  public List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
  
  public List<OffSiteWorkingTemp> offSiteWorkingTempList = Lists.newArrayList();
  
  public boolean includeStampingDetails;
  
}
