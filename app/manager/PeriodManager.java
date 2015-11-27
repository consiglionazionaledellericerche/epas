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
    List<IPropertyInPeriod> originals = Lists.newArrayList();
    for (IPropertyInPeriod originalPeriod :
        propertyInPeriod.getOwner().periods(propertyInPeriod.getType()) ) {
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
      //periodIntersect.setTarget(target);
      periodIntersect.setBeginDate(intersection.getBegin());
      periodIntersect.setEndDate(intersection.getEnd());
      periodIntersect.setValue(propertyInPeriod.getValue());

      //Parte iniziale old
      if (oldPeriod.getBeginDate().isBefore(periodIntersect.getBeginDate())) {
        IPropertyInPeriod periodOldBeginRemain = oldPeriod.newInstance();
        //periodOldBeginRemain.setTarget(target);
        periodOldBeginRemain.setBeginDate(oldPeriod.getBeginDate());
        periodOldBeginRemain.setEndDate(periodIntersect.getBeginDate().minusDays(1));
        periodOldBeginRemain.setValue(oldPeriod.getValue());
        previous = insertIntoList(previous, periodOldBeginRemain, periodList);
      }

      if (!recomputeBeginSet) {
        periodIntersect.setRecomputeFrom(periodIntersect.getBeginDate());
      }

      previous = insertIntoList(previous, periodIntersect, periodList);

      //Parte finale old
      if (periodIntersect.getEndDate() != null) {
        IPropertyInPeriod periodOldEndRemain = oldPeriod.newInstance();
        //periodOldEndRemain.setTarget(target);
        periodOldEndRemain.setBeginDate((periodIntersect.getEndDate()).plusDays(1));
        periodOldEndRemain.setValue(oldPeriod.getValue());
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
      //JPA.em().refresh(propertyInPeriod.getParent());
      for (IPropertyInPeriod periodRemoved : toRemove) {
        periodRemoved._delete();
        periodRemoved.getOwner().periods(periodRemoved.getType()).remove(periodRemoved);
      }
      //propertyInPeriod.getParent()._save();
      for (IPropertyInPeriod periodInsert : periodList) {
        periodInsert._save();
        periodInsert.getOwner().periods(periodInsert.getType()).add(periodInsert);
        //propertyInPeriod.getParent()._save();
      }
      propertyInPeriod.getOwner()._save();
      JPA.em().flush();
//      JPAPlugin.closeTx(false);
//      JPAPlugin.startTx(false);
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

    if (recomputeRecap.recomputeFrom.isAfter(LocalDate.now())) {
      recomputeRecap.needRecomputation = false;
    }


    if (recomputeRecap.recomputeFrom != null) {
      recomputeRecap.days = DateUtility.daysInInterval(
          new DateInterval(recomputeRecap.recomputeFrom, recomputeRecap.recomputeTo));
    }

    return recomputeRecap;

  }

}
