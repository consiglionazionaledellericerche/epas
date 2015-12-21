package manager.services.vacations.impl;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;
import lombok.Getter;

import manager.services.vacations.impl.AccruedDecision.TypeAccrued;

import models.Absence;
import models.VacationPeriod;

import java.util.List;

public class VacationPeriodDecision {
  
  private final AccruedDecision accruedDecision;
  private final AccruedConverter accruedConverter;
  
  @Getter private final VacationPeriod vacationPeriod;
  @Getter private final DateInterval interval;

  @Getter private List<Absence> postPartum;
  @Getter private int days = 0;
  @Getter private int accrued = 0;
  
  @Builder
  private VacationPeriodDecision(AccruedDecision accruedDecision,
      VacationPeriod vacationPeriod) {
    
    this.accruedDecision = accruedDecision;
    this.vacationPeriod = vacationPeriod;
    this.interval = DateUtility.intervalIntersection(vacationPeriod.getDateInterval(), 
        accruedDecision.getRequestInterval());
    this.accruedConverter = AccruedConverter.accruedConverter();
  }
  
  /**
   * Preleva le assenze di competenza dell'intervallo.
   * @param absences tutte le assenze post partum
   * @return this
   */
  public VacationPeriodDecision setPostPartumAbsences(List<Absence> absences) {
    
    this.postPartum = Lists.newArrayList();
    
    if (this.interval == null) {
      return this;
    }
    
    for (Absence ab : absences) {
      if (DateUtility.isDateIntoInterval(ab.personDay.date, this.interval)) {
        this.postPartum.add(ab);
      }
    }
    return this;
  }
  
  public VacationPeriodDecision setAccrued() {
    
    if (this.interval != null) {

      this.days = DateUtility.daysInInterval(this.interval) - this.postPartum.size();

      //calcolo i giorni maturati col metodo di conversione
      if (accruedDecision.getTypeAccrued().equals(TypeAccrued.PERMISSION_CURRENT_YEAR_ACCRUED) 
          || accruedDecision.getTypeAccrued().equals(TypeAccrued.PERMISSION_CURRENT_YEAR_ACCRUED)) {

        if (this.vacationPeriod.vacationCode.equals("21+3") 
            || this.vacationPeriod.vacationCode.description.equals("22+3")) {

          this.accrued = this.accruedConverter.permissionsPartTime(this.days);
        } else {
          this.accrued = this.accruedConverter.permissions(this.days);
        }

      } else {

        if (this.vacationPeriod.vacationCode.description.equals("26+4")) {
          this.accrued = this.accruedConverter.vacationsLessThreeYears(this.days);
        }
        if (this.vacationPeriod.vacationCode.description.equals("28+4")) {
          this.accrued = this.accruedConverter.vacationsMoreThreeYears(this.days);
        }
        if (this.vacationPeriod.vacationCode.description.equals("21+3")) {
          this.accrued = this.accruedConverter.vacationsPartTimeLessThreeYears(this.days);
        }
        if (this.vacationPeriod.vacationCode.description.equals("22+3")) {
          this.accrued = this.accruedConverter.vacationsPartTimeMoreThreeYears(this.days);
        }
      }

    }
    return this;
  }

}