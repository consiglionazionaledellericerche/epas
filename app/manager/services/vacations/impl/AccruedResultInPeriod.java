package manager.services.vacations.impl;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Getter;

import manager.services.vacations.IAccruedResult;
import manager.services.vacations.IAccruedResultInPeriod;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

import models.Absence;
import models.VacationCode;

import java.util.List;

/**
 * I giorni maturati nell'intervallo, tenuto conto dei postPartum, del tipo di richiesta 
 * (in accruedState) e del vacationCode.
 * @author alessandro
 *
 */
public class AccruedResultInPeriod extends AccruedResult implements IAccruedResultInPeriod {

  @Getter private VacationCode vacationCode;
  
  /**
   * Costruttore del risultato nel periodo.
   * @param parentAccruedResult il risultato di cui è un fattore.
   * @param interval l'intervallo effettivo su cui lavorare
   * @param vacationCode il tipo di assenza.
   */
  private AccruedResultInPeriod(AccruedResult parentAccruedResult, DateInterval interval, 
      VacationCode vacationCode) {
    super(parentAccruedResult.getVacationsResult(), interval);
    this.vacationCode = vacationCode;
  }
  
  public static AccruedResultInPeriod buildAccruedResultInPeriod(
      AccruedResult parentAccruedResult, DateInterval interval, 
      VacationCode vacationCode) {
    return new AccruedResultInPeriod(parentAccruedResult, interval, vacationCode);
  }
  
  /**
   * Preleva le assenze di competenza dell'intervallo.
   * @param absences tutte le assenze post partum
   * @return this
   */
  public AccruedResultInPeriod setPostPartumAbsences(List<Absence> absences) {
    
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

  /**
   * Effettua il calcolo.
   * @return this
   */
  public AccruedResultInPeriod compute() {
    
    if (this.interval == null) { 
      return this;
    }

    //TODO: verificare che nel caso dei permessi non considero i giorni postPartum.
    this.days = DateUtility.daysInInterval(this.interval) - this.postPartum.size();  
    
    //calcolo i giorni maturati col metodo di conversione
    if (vacationsResult.getTypeVacation().equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {

      //this.days = DateUtility.daysInInterval(this.interval);
      
      if (this.vacationCode.equals("21+3") 
          || this.vacationCode.description.equals("22+3")) {

        this.accrued = this.accruedConverter.permissionsPartTime(this.days);
      } else {
        this.accrued = this.accruedConverter.permissions(this.days);
      }

    } else {

      //this.days = DateUtility.daysInInterval(this.interval) - this.postPartum.size();

      if (this.vacationCode.description.equals("26+4")) {
        this.accrued = this.accruedConverter.vacationsLessThreeYears(this.days);
      }
      if (this.vacationCode.description.equals("28+4")) {
        this.accrued = this.accruedConverter.vacationsMoreThreeYears(this.days);
      }
      if (this.vacationCode.description.equals("21+3")) {
        this.accrued = this.accruedConverter.vacationsPartTimeLessThreeYears(this.days);
      }
      if (this.vacationCode.description.equals("22+3")) {
        this.accrued = this.accruedConverter.vacationsPartTimeMoreThreeYears(this.days);
      }
    }

    return this;
  }
  

  
  
}