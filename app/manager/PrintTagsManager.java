package manager;

import com.google.common.collect.Lists;

import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;

import java.util.List;

import javax.inject.Inject;

import manager.charts.ChartsManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;

import models.Stamping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PrintTagsManager {

  private static final Logger log = LoggerFactory.getLogger(PrintTagsManager.class);
  private final StampingHistoryDao stampingHistoryDao;
  
  @Inject
  public PrintTagsManager(StampingHistoryDao stampingHistoryDao) {
    this.stampingHistoryDao = stampingHistoryDao;
  }
  
  public List<List<HistoryValue<Stamping>>> getHistoricalList(PersonStampingRecap psDto, 
      boolean includeStampingDetails) {
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
    if (includeStampingDetails) {
      for (PersonStampingDayRecap day : psDto.daysRecap) {
        if (!day.ignoreDay) {
          for (Stamping stamping : day.personDay.stampings) {
            if (stamping.markedByAdmin) {
              historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
            }
          }
        }
      }
    }
    return historyStampingsList;
  }
}
