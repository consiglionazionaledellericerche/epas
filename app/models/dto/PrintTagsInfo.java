package models.dto;

import dao.history.HistoryValue;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import manager.recaps.personstamping.PersonStampingRecap;

import models.Person;
import models.Stamping;
import models.dto.ShiftEvent.ShiftEventBuilder;
import models.enumerate.EventColor;
import models.enumerate.ShiftSlot;

import org.assertj.core.util.Lists;
import org.joda.time.LocalDateTime;

@Data
@Builder
public class PrintTagsInfo {

  public Person person;
  
  public PersonStampingRecap psDto;
  
  public List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
  
  public boolean includeStampingDetails;
  
}
