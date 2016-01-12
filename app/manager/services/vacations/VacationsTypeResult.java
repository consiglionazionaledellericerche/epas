package manager.services.vacations;

import com.google.common.collect.ImmutableList;

import it.cnr.iit.epas.DateUtility;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import models.Absence;

import org.joda.time.LocalDate;

/**
 * Il risultato per il TypeVacation per la richiesta vacationsRequest,
 * considerando le absenceUsed e i sourced.
 *
 * @author alessandro
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class VacationsTypeResult {
  
  public enum TypeVacation {
    VACATION_LAST_YEAR,
    VACATION_CURRENT_YEAR,
    PERMISSION_CURRENT_YEAR,
  }

  // DATI INPUT
  private VacationsRequest vacationsRequest;
  private TypeVacation typeVacation;
  private ImmutableList<Absence> absencesUsed;
  private int sourced;

  //DATI OUTPUT
  private AccruedResult totalResult;
  private AccruedResult accruedResult;
  
  private boolean isExpired = false;
  
  private LocalDate lowerLimit;
  private LocalDate upperLimit;
  
  private boolean isContractLowerLimit = false;
  private boolean isContractUpperLimit = false;
 
  /**
   * Numero di assenze usate.
   */
  public Integer getUsed() {
    return this.absencesUsed.size() + this.sourced;
  }

  /**
   * Numero di assenze totali.
   */
  public Integer getTotal() {
    return this.totalResult.getAccrued() + this.totalResult.getFixed();
  }

  /**
   * Numero di assenze maturate.
   */
  public Integer getAccrued() {

    return this.accruedResult.getAccrued() + this.totalResult.getFixed();
  }

  /**
   * Rimanenti totali (indipendentemente che siano prendibili, non maturate o scadute).
   */
  public Integer getNotYetUsedTotal() {
    return this.getTotal() - this.getUsed();
  }
  
  /**
   * Rimanenti maturate.
   */
  public Integer getNotYetUsedAccrued() {
    return this.getAccrued() - this.getUsed();
  }

  /**
   * Rimanenti prendibili. Per i determinati solo le maturate, per gli indeterminati
   * tutte. Per le ferie dell'anno precedente considera la data di scadenza ferie della sede.
   */
  public Integer getNotYetUsedTakeable() {

    // caso delle ferie anno passato.
    if (this.typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      LocalDate expireDate = this.vacationsRequest.getExpireDateLastYear();

      if (this.vacationsRequest.getAccruedDate().isAfter(expireDate)) {
        return 0;
      }
      return this.getAccrued() - this.getUsed();
    }

    //altri casi permessi e ferie anno corrente
    if (!DateUtility.isInfinity(this.vacationsRequest.getContractDateInterval().getEnd())) {
      //per i determinati considero le maturate (perchè potrebbero decidere di cambiare contratto)
      return this.getAccrued() - this.getUsed();
    } else {
      return this.getTotal() - this.getUsed();
    }

  }

  /**
   * La data di scadenza utilizzo ferie.
   */
  public LocalDate getExpireDate() {

    LocalDate computedEndContract = this.vacationsRequest.getContractDateInterval().getEnd();

    if (this.typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      if (computedEndContract.isBefore(this.vacationsRequest.getExpireDateLastYear())) {
        return computedEndContract;
      }
      return this.vacationsRequest.getExpireDateLastYear();
    }

    if (this.typeVacation.equals(TypeVacation.VACATION_CURRENT_YEAR)) {
      if (computedEndContract.isBefore(this.vacationsRequest.getExpireDateCurrentYear())) {
        return computedEndContract;
      }
      return this.vacationsRequest.getExpireDateCurrentYear();
    }

    if (this.typeVacation.equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      LocalDate endYear = new LocalDate(this.vacationsRequest.getYear(), 12, 31);
      if (computedEndContract.isBefore(endYear)) {
        return computedEndContract;
      }
      return endYear;
    }
    return null;
  }

}



