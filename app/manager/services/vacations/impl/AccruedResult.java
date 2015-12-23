package manager.services.vacations.impl;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import manager.services.vacations.IAccruedResult;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

import models.Absence;
import models.VacationCode;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * I giorni maturati nell'intervallo, tenuto conto dei postPartum e del tipo di richiesta
 * (in vacationsResponse).
 * @author alessandro
 *
 */
public class AccruedResult implements IAccruedResult {

  public static final int YEAR_VACATION_UPPER_BOUND = 28;

  @Getter protected final VacationsTypeResult vacationsResult;
  @Getter protected final AccruedConverter accruedConverter;

  @Getter protected List<AccruedResultInPeriod> accruedResultsInPeriod = Lists.newArrayList();

  @Getter protected DateInterval interval;

  @Getter protected List<Absence> postPartum = Lists.newArrayList();
  @Getter protected int days = 0;
  @Getter protected int accrued = 0;
  @Getter protected int fixed = 0;

  /**
   * Costruttore del risultato.
   * @param vacationsTypeResult il risultato a cui si riferisce.
   * @param interval l'intervallo effettivo su cui operare.
   */
  @Builder
  public AccruedResult(VacationsTypeResult vacationsTypeResult, DateInterval interval) {

    this.vacationsResult = vacationsTypeResult;
    this.interval = interval;
    this.accruedConverter = AccruedConverter.builder().build();
  }

  /**
   * Somma il risultato.
   * @param accruedResultInPeriod il risultato da sommare.
   * @return this
   */
  public AccruedResult addResult(AccruedResultInPeriod accruedResultInPeriod) {
    this.accruedResultsInPeriod.add(accruedResultInPeriod);
    this.postPartum.addAll(accruedResultInPeriod.postPartum);
    this.days += accruedResultInPeriod.days;
    this.accrued += accruedResultInPeriod.accrued;
    return this;
  }

  /**
   * Aggiusta il calcolo di ferie e permessi totali.
   *
   * @return this
   */
  public AccruedResult adjustDecision() {

    if (this.interval == null) {
      return this;  //niente da aggiustare..
    }

    // per ora i permessi non li aggiusto.
    if (this.vacationsResult.getTypeVacation().equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      return this;
    }

    if (this.accruedResultsInPeriod.isEmpty()) {
      return this;
    }

    DateInterval yearInterval = DateUtility
        .getYearInterval(vacationsResult.getVacationsRequest().getYear());

    int totalYearPostPartum = 0;
    int totalVacationAccrued = 0;

    AccruedResultInPeriod minAccruedResultInPeriod = null;


    for (AccruedResultInPeriod accruedResultInPeriod : this.accruedResultsInPeriod) {

      if (minAccruedResultInPeriod == null) {

        minAccruedResultInPeriod = accruedResultInPeriod;

      } else if (accruedResultInPeriod.getVacationCode().vacationDays
          < minAccruedResultInPeriod.getVacationCode().vacationDays ) {

        minAccruedResultInPeriod = accruedResultInPeriod;
      }
      totalYearPostPartum += accruedResultInPeriod.getPostPartum().size();
      totalVacationAccrued += accruedResultInPeriod.getAccrued();

    }

    //Aggiusto perchè l'algoritmo ne ha date troppe.
    if (totalVacationAccrued > YEAR_VACATION_UPPER_BOUND) {
      this.fixed = YEAR_VACATION_UPPER_BOUND - totalVacationAccrued;  //negative
    }

    //Aggiusto perchè l'algoritmo ne ha date troppo poche.
    //Condizione: no assenze post partum e periodo che copre tutto l'anno.
    if (totalYearPostPartum == 0
        && this.interval.getBegin().equals(yearInterval.getBegin())
            && this.interval.getEnd().equals(yearInterval.getEnd())) {

      if (minAccruedResultInPeriod.getVacationCode().vacationDays
          > totalVacationAccrued) {

        this.fixed = minAccruedResultInPeriod.getVacationCode().vacationDays
            - totalVacationAccrued; //positive
      }
    }

    return this;
  }


}