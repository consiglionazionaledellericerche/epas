package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import edu.emory.mathcs.backport.java.util.Collections;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.recaps.recomputation.RecomputeRecap;

import models.base.IPeriodModel;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;

import java.util.List;

import javax.inject.Inject;

/**
 * Manager per la gestione dei periodi.
 *
 * @author alessandro
 *
 */
public class PeriodManager {

  @Inject
  public PeriodManager() {

  }

  /**
   * Inserisce il nuovo period all'interno dei periodi in target.
   *
   * @param propertyInPeriod il periodo da inserire (ex.ContractWorkingTimeType)
   * @param persist true persiste la nuova lista di periodi.
   * @return la nuova lista dei periodi
   */
  public final List<IPropertyInPeriod> updatePeriods(
      IPropertyInPeriod propertyInPeriod, boolean persist) {
    boolean recomputeBeginSet = false;

    //copia dei periodi ordinata
    // TODO: periods() deve prendere il tipo quando ci sarà, e andranno modificate tutte 
    // le implementazioni
    List<IPropertyInPeriod> originals = Lists.newArrayList();
    for (IPropertyInPeriod originalPeriod :
      propertyInPeriod.getOwner().periods(propertyInPeriod.getClass()) ) {
      originals.add(originalPeriod);
    }
    Collections.sort(originals);

    DateInterval periodInterval =
        new DateInterval(propertyInPeriod.getBeginDate(), propertyInPeriod.getEndDate());
    List<IPropertyInPeriod> periodList = Lists.newArrayList();
    IPropertyInPeriod previous = null;

    List<IPropertyInPeriod> toRemove = Lists.newArrayList();

    for (IPropertyInPeriod oldPeriod : originals) {
      DateInterval oldInterval = new DateInterval(oldPeriod.getBeginDate(), oldPeriod.getEndDate());

      //non cambia il valore del periodo nessuna modifica su quel oldPeriod
      if (propertyInPeriod.periodValueEquals(oldPeriod)) {
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous == null || !previous.equals(oldPeriod)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }
      DateInterval intersection = DateUtility.intervalIntersection(periodInterval, oldInterval);
      //non si intersecano nessuna modifica su quel oldPeriiod
      if (intersection == null) {
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous == null || !previous.equals(oldPeriod)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }

      //si sovrappongono e sono diversi
      toRemove.add(oldPeriod);

      IPropertyInPeriod periodIntersect = propertyInPeriod.newInstance();
      periodIntersect.setBeginDate(intersection.getBegin());
      periodIntersect.setEndDate(intersection.getEnd());

      //Parte iniziale old
      if (oldPeriod.getBeginDate().isBefore(periodIntersect.getBeginDate())) {
        IPropertyInPeriod periodOldBeginRemain = oldPeriod.newInstance();
        periodOldBeginRemain.setBeginDate(oldPeriod.getBeginDate());
        periodOldBeginRemain.setEndDate(periodIntersect.getBeginDate().minusDays(1));
        previous = insertIntoList(previous, periodOldBeginRemain, periodList);
      }

      if (!recomputeBeginSet) {
        periodIntersect.setRecomputeFrom(periodIntersect.getBeginDate());
        recomputeBeginSet = true;
      }

      previous = insertIntoList(previous, periodIntersect, periodList);

      //Parte finale old
      if (periodIntersect.getEndDate() != null) {
        IPropertyInPeriod periodOldEndRemain = oldPeriod.newInstance();
        periodOldEndRemain.setBeginDate((periodIntersect.getEndDate()).plusDays(1));
        if (oldPeriod.getEndDate() != null) {
          if (periodIntersect.getEndDate()
              .isBefore(oldPeriod.getEndDate())) {
            periodOldEndRemain.setEndDate(oldPeriod.getEndDate());
            previous = insertIntoList(previous, periodOldEndRemain, periodList);
          }
        } else {
          periodOldEndRemain.setEndDate(oldPeriod.getEndDate());
          previous = insertIntoList(previous, periodOldEndRemain, periodList);
        }
      }

    }

    if (persist) {
      for (IPropertyInPeriod periodRemoved : toRemove) {
        periodRemoved._delete();
      }
      for (IPropertyInPeriod periodInsert : periodList) {
        periodInsert._save();
      }
      propertyInPeriod.getOwner()._save();
      JPA.em().flush();
    }


    return periodList;

  }

  /**
   * Inserisce un periodo nella nuova lista ordinata. Se il periodo precedente ha lo stesso valore
   * effettua la merge.
   * @param previous previous
   * @param present present
   * @param periodList periodList
   * @return l'ultimo periodo della lista
   */
  private IPropertyInPeriod insertIntoList(IPropertyInPeriod previous,
      IPropertyInPeriod present, List<IPropertyInPeriod> periodList) {

    if (previous != null && previous.periodValueEquals(present))  {
      previous.setEndDate(present.getEndDate());
      if (present.getRecomputeFrom() != null) {
        previous.setRecomputeFrom(present.getRecomputeFrom());
      }
      return previous;
    } else {
      periodList.add(present);
      return present;
    }
  }

  /**
   * Costruisce il riepilogo delle modifiche da effettuare.
   *
   * @param begin begin del target
   * @param end end del target
   * @param periods i nuovi periods del target
   * @return il riepilogo dei ricalcoli da effettuare.
   */
  public RecomputeRecap buildRecap(LocalDate begin, Optional<LocalDate> end,
      List<IPropertyInPeriod> periods) {

    RecomputeRecap recomputeRecap = new RecomputeRecap();

    recomputeRecap.needRecomputation = true;

    recomputeRecap.periods = periods;
    recomputeRecap.recomputeFrom = LocalDate.now().plusDays(1);
    recomputeRecap.recomputeTo = end;
    for (IPropertyInPeriod item : periods) {
      if (item.getRecomputeFrom() != null && item.getRecomputeFrom().isBefore(LocalDate.now())) {
        recomputeRecap.recomputeFrom = item.getRecomputeFrom();
      }
    }

    if (recomputeRecap.recomputeTo.isPresent()) {
      if (recomputeRecap.recomputeTo.get().isAfter(LocalDate.now())) {
        recomputeRecap.recomputeTo = Optional.fromNullable(LocalDate.now());
      }
      if (recomputeRecap.recomputeFrom.isBefore(begin)) {
        recomputeRecap.recomputeFrom = begin;
      }
    }

    setDays(recomputeRecap);

    setNeedRecap(recomputeRecap);

    return recomputeRecap;

  }

  /**
   * Quando modifico le date del target. 
   * 
   * @param previousInterval l'intervallo del target precedente
   * @param newInterval l'intervallo del target nuovo
   * @param initMissing se col nuovo intervallo manca l'inizializzazione del target
   * @return
   */
  public RecomputeRecap buildTargetRecap(DateInterval previousInterval, 
      DateInterval newInterval, boolean initMissing) {

    RecomputeRecap recomputeRecap = new RecomputeRecap();

    if (!newInterval.getBegin().isEqual(previousInterval.getBegin())) {
      if (newInterval.getBegin().isBefore(LocalDate.now())) {
        recomputeRecap.recomputeFrom = newInterval.getBegin();
      }
    }
    if (recomputeRecap.recomputeFrom == null) {
      if (!newInterval.getEnd().isEqual(previousInterval.getEnd())) {
        // scorcio allora solo riepiloghi
        if (newInterval.getEnd().isBefore(previousInterval.getEnd())) {
          recomputeRecap.recomputeFrom = newInterval.getEnd();
        }
        // allungo ma se inglobo passato allora ricalcolo
        if (newInterval.getEnd().isAfter(previousInterval.getEnd())
            && previousInterval.getEnd().isBefore(LocalDate.now())) {
          recomputeRecap.recomputeFrom = previousInterval.getEnd();
        }
      }
    }
    if (recomputeRecap.recomputeFrom != null) {
      recomputeRecap.recomputeTo = Optional.fromNullable(newInterval.getEnd());
      if (!recomputeRecap.recomputeTo.get().isBefore(LocalDate.now())) {
        recomputeRecap.recomputeTo = Optional.fromNullable(LocalDate.now());
      }
    }
    if (initMissing) {
      recomputeRecap.initMissing = true;
      recomputeRecap.recomputeFrom = null;
    }

    setDays(recomputeRecap);

    setNeedRecap(recomputeRecap);

    return recomputeRecap;
  }

  /**
   * 
   * @param recomputeRecap
   * @return
   */
  private void setDays(RecomputeRecap recomputeRecap) {
    if (recomputeRecap.recomputeFrom != null && 
        !recomputeRecap.recomputeFrom.isAfter(LocalDate.now())) {
      recomputeRecap.days = DateUtility.daysInInterval(
          new DateInterval(recomputeRecap.recomputeFrom, recomputeRecap.recomputeTo));
    } else {
      recomputeRecap.days = 0;
    }
  }

  private void setNeedRecap(RecomputeRecap recomputeRecap) {
    if (recomputeRecap.recomputeFrom == null || 
        recomputeRecap.recomputeFrom.isAfter(LocalDate.now())) {
      recomputeRecap.needRecomputation = false;
    } else {
      recomputeRecap.needRecomputation = true;
    }

  }

}
