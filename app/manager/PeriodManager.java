package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import edu.emory.mathcs.backport.java.util.Collections;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.cache.StampTypeManager;
import manager.recaps.vacation.VacationsRecapFactory;

import models.base.BaseModel;
import models.base.IPeriodModel;
import models.base.IPeriodTarget;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.db.jpa.JPAPlugin;

import java.util.List;

import javax.inject.Inject;

public class PeriodManager {
  
  @Inject
  public PeriodManager() {
    
  }
  
  /**
   * Inserisce il nuovo period all'interno dei periodi in target.
   * 
   * @param target il modello contenente la lista di periodi
   * @param period il periodo da inserire
   * @param persist true persiste la nuova lista di periodi.
   * @return
   */
  public final List<PeriodModel> updatePeriods(IPeriodTarget target, 
      IPeriodModel period, boolean persist) {
    boolean recomputeBeginSet = false;

    //copia dei periodi ordinata
    List<PeriodModel> originals = Lists.newArrayList();
    for (Object originalPeriod : period.periods() ) {
      originals.add((PeriodModel)originalPeriod);
    }
    Collections.sort(originals);

    DateInterval periodInterval = new DateInterval(period.getBegin(), period.getEnd());
    List<PeriodModel> periodList = Lists.newArrayList();
    PeriodModel previous = null;
    
    List<PeriodModel> toRemove = Lists.newArrayList();
    
    for (PeriodModel oldPeriod : originals) {
      DateInterval oldInterval = new DateInterval(oldPeriod.getBegin(), oldPeriod.getEnd());

      //non cambia il tipo orario nessuna modifica su quel oldCwtt
      if (period.periodValueEquals(oldPeriod)) {        
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous.id == null || !previous.id.equals(oldPeriod.id)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }
      DateInterval intersection = DateUtility.intervalIntersection(periodInterval, oldInterval);
      //non si intersecano nessuna modifica su quel oldCwtt
      if (intersection == null) {
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous.id == null || !previous.id.equals(oldPeriod.id)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }

      //si sovrappongono e sono diversi
      toRemove.add(oldPeriod);
      
      PeriodModel periodIntersect = period.newInstance();
      periodIntersect.setTarget(target);
      periodIntersect.setBegin(intersection.getBegin());
      periodIntersect.setEnd(Optional.fromNullable(intersection.getEnd()));
      periodIntersect.setValue(period.getValue());
       
      //Parte iniziale old
      if (oldPeriod.getBegin().isBefore(periodIntersect.getBegin())) {
        PeriodModel periodOldBeginRemain = period.newInstance();
        periodOldBeginRemain.setTarget(target);
        periodOldBeginRemain.setBegin(oldPeriod.getBegin());
        periodOldBeginRemain.setEnd(Optional.fromNullable(periodIntersect.getBegin().minusDays(1)));
        periodOldBeginRemain.setValue(oldPeriod.getValue());
        previous = insertIntoList(previous, periodOldBeginRemain, periodList); 
      }

      if (!recomputeBeginSet) {
        periodIntersect.recomputeFrom = periodIntersect.getBegin();
      }

      previous = insertIntoList(previous, periodIntersect, periodList);
      
      //Parte finale old
      if (periodIntersect.getEnd().isPresent()) {
        PeriodModel periodOldEndRemain = period.newInstance();
        periodOldEndRemain.setTarget(target);
        periodOldEndRemain.setBegin(((LocalDate)periodIntersect.getEnd().get()).plusDays(1));
        periodOldEndRemain.setValue(oldPeriod.getValue());
        if (oldPeriod.getEnd().isPresent()) {
          if (((LocalDate)periodIntersect.getEnd().get())
              .isBefore((LocalDate)oldPeriod.getEnd().get())) {
            periodOldEndRemain.setEnd(oldPeriod.getEnd());
            previous = insertIntoList(previous, periodOldEndRemain, periodList); 
          }
        } else {
          periodOldEndRemain.setEnd(oldPeriod.getEnd());
          previous = insertIntoList(previous, periodOldEndRemain, periodList); 
        }
      }

    }

    if (persist) {
      target.getValue().refresh();
      for (IPeriodModel periodRemoved : toRemove) {
        periodRemoved.getPeriod().delete();
        periodRemoved.periods().remove(periodRemoved);
      }
      target.getValue().save();
      for (IPeriodModel periodInsert : periodList) {
        //if (cwttInsert.isPersistent()) {
        periodInsert.getPeriod().save();
        periodInsert.periods().add(periodInsert);
        target.getValue().save();
        //}
      }
      target.getValue().save();
      JPAPlugin.closeTx(false);
      JPAPlugin.startTx(false);
    }

    
    return periodList;

  }

  private PeriodModel insertIntoList(PeriodModel previous, 
      PeriodModel present, List<PeriodModel> periodList) {
    
    if (previous != null && previous.periodValueEquals(present))  {
      previous.setEnd(present.getEnd()); 
      if (present.recomputeFrom != null) {
        previous.recomputeFrom = present.recomputeFrom;
      }
      return previous;
    } else {
      periodList.add(present);
      return present;
    }
  }
  
}
