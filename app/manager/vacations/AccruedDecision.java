package manager.vacations;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;
import lombok.Getter;

import models.Absence;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public class AccruedDecision {
  
  public enum TypeAccrued {
    VACATION_LAST_YEAR_TOTAL,
    VACATION_CURRENT_YEAR_TOTAL,
    PERMISSION_CURRENT_YEAR_TOTAL,
    
    VACATION_CURRENT_YEAR_ACCRUED,
    PERMISSION_CURRENT_YEAR_ACCRUED;
  }
  
  private static final int YEAR_VACATION_UPPER_BOUND = 28;

  private final AccruedComponent accruedComponent;
  
  @Getter private final TypeAccrued typeAccrued;
  @Getter private List<VacationPeriodDecision> vacationPeriodDecisions = Lists.newArrayList();
  @Getter private DateInterval requestInterval;
  @Getter private int totalAccrued = 0;
  @Getter private int fixed = 0;
  @Getter private int totalPostPartum = 0;
  @Getter private int totalDays = 0;

  /**
   * Costruisce la decisione.
   * @param accruedComponent lo stato della richiesta
   * @param typeAccrued il tipo della richiesta
   */
  @Builder
  public AccruedDecision(AccruedComponent accruedComponent, TypeAccrued typeAccrued) {
    
    this.accruedComponent = accruedComponent;
    this.typeAccrued = typeAccrued;
    this.vacationPeriodDecisions = Lists.newArrayList();
    
    LocalDate beginYear = new LocalDate(accruedComponent.getYear(), 1, 1);
    LocalDate endYear = new LocalDate(accruedComponent.getYear(), 12, 31);
    
    if (typeAccrued.equals(TypeAccrued.VACATION_LAST_YEAR_TOTAL)) {
      beginYear = new LocalDate(accruedComponent.getYear() - 1, 1, 1);
      endYear = new LocalDate(accruedComponent.getYear() - 1, 12, 31);
    }

    //Calcolo l'intersezione fra l'anno e il contratto attuale
    this.requestInterval = new DateInterval(beginYear, endYear);

    if (typeAccrued.equals(TypeAccrued.VACATION_CURRENT_YEAR_ACCRUED) 
        || typeAccrued.equals(TypeAccrued.PERMISSION_CURRENT_YEAR_ACCRUED)) {
      
      this.requestInterval = new DateInterval(new LocalDate(accruedComponent.getYear(), 1, 1), 
          accruedComponent.getAccruedDate());
    }
    
    this.requestInterval = DateUtility.intervalIntersection(requestInterval, 
        accruedComponent.getContractDateInterval());

    if (this.requestInterval == null) {
      return;
    }

    for (VacationPeriod vp : accruedComponent.getContractVacationPeriod()) {
      
      VacationPeriodDecision vacationPeriodDecision = VacationPeriodDecision.builder()
          .accruedDecision(this)
          .vacationPeriod(vp)
          .build()
          .setPostPartumAbsences(accruedComponent.getPostPartum())
          .setAccrued();
      
      this.vacationPeriodDecisions.add(vacationPeriodDecision);
    }
    
    // Fix dei casi particolari quando capita il cambio piano.
    this.adjustDecision();

    for (VacationPeriodDecision vacationPeriodDecision : vacationPeriodDecisions) {
      
      this.totalDays += vacationPeriodDecision.getDays();
      this.totalAccrued += vacationPeriodDecision.getAccrued();
      this.totalPostPartum += vacationPeriodDecision.getPostPartum().size();
    }
    
    return;
  }
  
  /**
   * Aggiusta il calcolo di ferie e permessi totali.
   * 
   * @return l'aggiustamento necessario.
   */
  private AccruedDecision adjustDecision() {
    
    // per ora i valori in maturazione non li aggiusto.
    if (this.typeAccrued.equals(TypeAccrued.PERMISSION_CURRENT_YEAR_ACCRUED) 
        || this.typeAccrued.equals(TypeAccrued.VACATION_CURRENT_YEAR_ACCRUED)) {
      return this;
    }
    
    if (this.vacationPeriodDecisions.isEmpty()) {
      return this;
    }
    
    DateInterval yearInterval = DateUtility.getYearInterval(accruedComponent.getYear());
    
    int totalYearPostPartum = 0;
    int totalVacationAccrued = 0;
    
    VacationPeriodDecision minVacationPeriodDecision = null;


    for (VacationPeriodDecision vacationPeriodDecision : vacationPeriodDecisions) {

      if (minVacationPeriodDecision == null) {
        
        minVacationPeriodDecision = vacationPeriodDecision;
        
      } else if (vacationPeriodDecision.getVacationPeriod().vacationCode.vacationDays 
          < minVacationPeriodDecision.getVacationPeriod().vacationCode.vacationDays) {
        
        minVacationPeriodDecision = vacationPeriodDecision;
      }
      totalYearPostPartum += vacationPeriodDecision.getPostPartum().size();
      totalVacationAccrued += vacationPeriodDecision.getAccrued();
      
    }

    //Aggiusto perchè l'algoritmo ne ha date troppe.
    if (totalVacationAccrued > YEAR_VACATION_UPPER_BOUND) {
      this.fixed = YEAR_VACATION_UPPER_BOUND - totalVacationAccrued;  //negative
    }

    //Aggiusto perchè l'algoritmo ne ha date troppo poche.
    //Condizione: no assenze post partum e periodo che copre tutto l'anno.
    if (totalYearPostPartum == 0 
        && this.requestInterval.getBegin().equals(yearInterval.getBegin())
            && this.requestInterval.getEnd().equals(yearInterval.getEnd())) {
      
      if (minVacationPeriodDecision.getVacationPeriod().vacationCode.vacationDays 
          > totalVacationAccrued) {
        
        this.fixed = minVacationPeriodDecision.getVacationPeriod().vacationCode.vacationDays 
            - totalVacationAccrued; //positive
      }
    }

    return this;
  }
  
  
  
}


