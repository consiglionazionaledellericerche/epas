/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.configurations.EpasParam;
import manager.recaps.recomputation.RecomputeRecap;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;

/**
 * Manager per la gestione dei periodi.
 *
 * @author Alessandro Martelli
 *
 */
@Slf4j
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
    return updatePeriods(propertyInPeriod, persist, true);
  }

  /**
   * Inserisce il nuovo period all'interno dei periodi in target.
   *
   * @param propertyInPeriod il periodo da inserire (ex.ContractWorkingTimeType)
   * @param persist true persiste la nuova lista di periodi.
   * @param validateAllPeriodCovered se impostata a true controlla che tutto il periodo dell'owner
   *     del propertyInPeriod sia coperto.
   * @return la nuova lista dei periodi
   */
  public final List<IPropertyInPeriod> updatePeriods(
      IPropertyInPeriod propertyInPeriod, boolean persist, boolean validateAllPeriodCovered) {
    boolean recomputeBeginSet = false;

    //controllo iniziale consistenza periodo.
    if (propertyInPeriod.getBeginDate() != null 
        &&  propertyInPeriod.calculatedEnd() != null 
        && propertyInPeriod.getBeginDate().isAfter(propertyInPeriod.calculatedEnd())) {
      log.warn("Scartato aggiornamento PropertyInPeriod per data inconsistente. "
          + "propertyInPeriod.owner = {}, propertyInPeriod.type = {}. "
          + "beginDate = {}, calculatedEnd = {}", 
          propertyInPeriod.getOwner(), propertyInPeriod.getType(),
          propertyInPeriod.getBeginDate(),
          propertyInPeriod.calculatedEnd());
      return propertyInPeriod.getOwner().periods(propertyInPeriod.getType())
          .stream().collect(Collectors.toList());
    }
    
    //copia dei periodi ordinata
    List<IPropertyInPeriod> originals = Lists.newArrayList();
    for (IPropertyInPeriod originalPeriod :
          propertyInPeriod.getOwner().periods(propertyInPeriod.getType())) {
      originals.add(originalPeriod);
    }
    originals = originals.stream().sorted().collect(Collectors.toList());

    DateInterval periodInterval =
        new DateInterval(propertyInPeriod.getBeginDate(), propertyInPeriod.getEndDate());
    List<IPropertyInPeriod> periodList = Lists.newArrayList();
    IPropertyInPeriod previous = null;

    List<IPropertyInPeriod> toRemove = Lists.newArrayList();

    for (IPropertyInPeriod oldPeriod : originals) {
      DateInterval oldInterval = new DateInterval(oldPeriod.getBeginDate(), oldPeriod.getEndDate());

      //non cambia il valore del periodo nessuna modifica su quel oldPeriod
      if (propertyInPeriod.periodValueEquals(oldPeriod.getValue())) {
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
      if (DateUtility.isInfinity(periodIntersect.getEndDate())) {
        periodIntersect.setEndDate(null);
      }
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
    
    //caso iniziale, se non vi era alcun periodo allora quello nuovo lo inserisco.
    if (originals.isEmpty()) {
      periodList.add(propertyInPeriod);
    }
    
    //Validazione copertura periodi.
    Verify.verify(validatePeriods(propertyInPeriod.getOwner(), periodList));
    
    //Fix merge period
    

    if (persist) {
      for (IPropertyInPeriod periodRemoved : toRemove) {
        periodRemoved._delete();
      }
      if (propertyInPeriod.getType().equals(EpasParam.WORKING_OFF_SITE)) {
        log.debug("...");
      }
      for (IPropertyInPeriod periodInsert : periodList) {
        periodInsert._save();
      }
      propertyInPeriod.getOwner()._save();
      JPA.em().flush();
      JPA.em().refresh(propertyInPeriod.getOwner());
    }

    return periodList;

  }
  
  /**
   * true se il periodo si sovrappone a quelli esistenti, false altrimenti.
   *
   * @param period il periodo da analizzare
   * @param currentPeriods i periodi presenti sul db
   * @return true se il periodo si sovrappone a quelli esistenti, false altrimenti.
   */
  public final boolean isOverlapped(IPropertyInPeriod period, 
      Collection<IPropertyInPeriod> currentPeriods) {
    Range<LocalDate> periodRange;
    if (period.calculatedEnd() != null) {
      periodRange = Range.closed(period.getBeginDate(), period.calculatedEnd());
    } else {
      periodRange = Range.downTo(period.getBeginDate(), BoundType.CLOSED);
    }
    
    for (IPropertyInPeriod oldPeriod : currentPeriods) {
      Range<LocalDate> oldPeriodRange;
      if (oldPeriod.getEndDate() != null) {
        oldPeriodRange = Range.closed(oldPeriod.getBeginDate(), oldPeriod.getEndDate());
      } else {
        oldPeriodRange = Range.downTo(oldPeriod.getBeginDate(), BoundType.CLOSED);
      }

      if (periodRange.isConnected(oldPeriodRange)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Preleva le data iniziale più piccola e la data finale più grande di quelle
   * presenti nei periodi passati.
   *
   * @param periods i periodi di cui prelevare data iniziale e finale
   * @return un DateInterval composto con data iniziale più piccola dei vari 
   *     periodi e data finale più grande dei vari periodi
   */
  private final Optional<DateInterval> getDateIntervalPeriod(List<IPropertyInPeriod> periods) {
    //Costruzione intervallo covered
    periods = periods.stream().sorted().collect(Collectors.toList());
    LocalDate begin = null;
    LocalDate end = null;
    for (IPropertyInPeriod period : periods) {
      if (begin == null) {
        begin = period.getBeginDate();
        end = period.calculatedEnd();
        continue;
      }
      
      // Non dovrebbero essercene altri.
      if (end == null) {
        return Optional.absent();
      }
      
      // Deve iniziare subito dopo la fine del precedente.
      if (!end.plusDays(1).isEqual(period.getBeginDate())) {
        return Optional.absent();
      }
      
      end = period.calculatedEnd();
    } 
    return Optional.of(new DateInterval(begin, end));
  }
  
  /**
   * Controlla che per ogni giorno dell'owner vi sia uno e uno solo valore.
   *
   * @return esito
   */
  private final boolean validatePeriods(IPropertiesInPeriodOwner owner, 
      List<IPropertyInPeriod> periods) {
    val beginEndPeriods = getDateIntervalPeriod(periods);
    if (!beginEndPeriods.isPresent()) {
      return false;
    }
    //Confronto fra intervallo covered e quello dell'owner
    return DateUtility.areIntervalsEquals(beginEndPeriods.get(),
        new DateInterval(owner.getBeginDate(), owner.calculatedEnd()));
   
  }
  
  /**
   * Inserisce un periodo nella nuova lista ordinata. Se il periodo precedente ha lo stesso valore
   * effettua la merge.
   *
   * @param previous previous
   * @param present present
   * @param periodList periodList
   * @return l'ultimo periodo della lista
   */
  private IPropertyInPeriod insertIntoList(IPropertyInPeriod previous,
      IPropertyInPeriod present, List<IPropertyInPeriod> periodList) {

    if (previous != null && previous.periodValueEquals(present.getValue()))  {
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
   * Quando si cambiano le date di inizio e fine dell'owner questo algoritmo sistema i periodi: <br>
   * 1) Elimina i periodi che non appartegono più all'intervallo dell'owner <br>
   * 2) Aggiusta il primo periodo impostando la sua data inizio 
   *    alla nuova data inizio dell'owner.<br>
   * 3) Aggiusta l'ultimo periodo impostando la sua data fine alla nuova data fine dell'owner.<br>
   * Persiste il nuovo stato.
   *
   * @param owner owner
   */
  public void updatePropertiesInPeriodOwner(IPropertiesInPeriodOwner owner) {
    
    DateInterval ownerInterval = new DateInterval(owner.getBeginDate(), owner.calculatedEnd());
    
    for (Object type : owner.types()) {
      boolean toRefresh = false;
      // 1) Cancello quelli che non appartengono più a contract
      for (IPropertyInPeriod propertyInPeriod : owner.periods(type)) {
        if (DateUtility.intervalIntersection(ownerInterval, 
            new DateInterval(propertyInPeriod.getBeginDate(), propertyInPeriod.getEndDate())) 
            == null) {
          propertyInPeriod._delete();
          toRefresh = true;
        }
      }
      if (toRefresh) {
        JPA.em().refresh(owner);
        JPA.em().flush();
      }

      List<IPropertyInPeriod> periods = Lists.newArrayList(owner.periods(type));
      if (periods.isEmpty()) {
        continue; //caso di parametro ancora non definito
      }
      periods = periods.stream().sorted(Comparator.comparing(IPropertyInPeriod::getBeginDate))
          .collect(Collectors.toList());

      // Sistemo il primo
      IPropertyInPeriod first = periods.get(0);
      first.setBeginDate(ownerInterval.getBegin());
      first._save();

      // Sistemo l'ultimo
      IPropertyInPeriod last = periods.get(periods.size() - 1);
      last.setEndDate(ownerInterval.getEnd());
      if (DateUtility.isInfinity(last.getEndDate())) {
        last.setEndDate(null);
      }
      last._save();

      JPA.em().flush();
    }
  }

  /**
   * Costruisce il riepilogo delle modifiche da effettuare.
   *
   * @param begin begin del target
   * @param end end del target
   * @param periods i nuovi periods del target
   * @param init per forzare come limite inferiore la data inizio ricalcolo
   * @return il riepilogo dei ricalcoli da effettuare.
   */
  public RecomputeRecap buildRecap(LocalDate begin, Optional<LocalDate> end,
      List<IPropertyInPeriod> periods, Optional<LocalDate> init) {

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
    
    if (init.isPresent()) {
      if (recomputeRecap.recomputeFrom.isBefore(init.get())) {
        recomputeRecap.recomputeFrom = init.get();
      }
      if (recomputeRecap.recomputeTo.isPresent() 
          && recomputeRecap.recomputeFrom.isAfter(recomputeRecap.recomputeTo.get())) {
        recomputeRecap.recomputeFrom = recomputeRecap.recomputeTo.get();
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
   * Assegna i giorni su cui far valere il periodo.
   *
   * @param recomputeRecap il recomputeRecap contenente i riepiloghi
   */
  public void setDays(RecomputeRecap recomputeRecap) {
    if (recomputeRecap.recomputeFrom != null
        && !recomputeRecap.recomputeFrom.isAfter(LocalDate.now())) {
      recomputeRecap.days = DateUtility.daysInInterval(
          DateInterval.withBegin(recomputeRecap.recomputeFrom, recomputeRecap.recomputeTo));
    } else {
      recomputeRecap.days = 0;
    }
  }

  private void setNeedRecap(RecomputeRecap recomputeRecap) {
    if (recomputeRecap.recomputeFrom == null
        || recomputeRecap.recomputeFrom.isAfter(LocalDate.now())) {
      recomputeRecap.needRecomputation = false;
    } else {
      recomputeRecap.needRecomputation = true;
    }

  }

}
