package models.dto;

import dao.history.HistoryValue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import models.Stamping;
import models.User;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;

@Data
@Builder
public class PrintTagsInfo {

  public Person person;
  
  public PersonStampingRecap psDto;
  
  @Builder.Default
  public List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
  
  @Builder.Default
  public List<OffSiteWorkingTemp> offSiteWorkingTempList = Lists.newArrayList();
  
  public boolean includeStampingDetails;
  
  @Builder.Default
  public Map<User, Set<LocalDate>> stampingOwnersInDays = Maps.newHashMap();
  
}
