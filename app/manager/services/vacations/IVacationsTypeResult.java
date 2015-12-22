package manager.services.vacations;

import com.google.common.collect.ImmutableList;

import manager.services.vacations.impl.VacationsRecap.VacationsRequest;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

import models.Absence;

/**
 * Il risultato per il TypeVacation per la richiesta VacationsRequest. 
 * 
 * @author alessandro
 */
public interface IVacationsTypeResult {

  /**
   * I Dati della richiesta (comuni ad ogni tipo di assenza).
   * @return la richiesta. 
   */
  VacationsRequest getVacationsRequest();
  
  /**
   * Il tipo di assenza del risultato.
   * @return il tipo.
   */
  TypeVacation getTypeVacation();
  
  /**
   * Le assenze usate del tipo di assenza.
   * @return la lista di assenze.
   */
  ImmutableList<Absence> getAbsencesUsed();
  
  /**
   * Le assenza da inizilizzazione del tipo di assenza.
   * @return source.
   */
  int getSourced();
  
  /**
   * Il risultato delle assenze totali.
   */
  IAccruedResult getTotalResult();

  /**
   * Il risultato delle assenze maturate.
   */
  IAccruedResult getAccruedResult();
  
  /**
   * Assenze usate.
   */
  Integer getUsed();
  
  /**
   * Numero di assenze totali.
   */
  Integer getTotal();
  
  /**
   * Numero di assenze maturate.
   */
  Integer getAccrued();

  /**
   * Rimanenti totali (indipendentemente che siano prendibili, non maturate o scadute).
   */
  Integer getNotYetUsedTotal();

  /**
   * Rimanenti maturate (prendibili). Per i determinati solo le maturate, per gli indeterminati
   * tutte. Per le ferie dell'anno precedente considera la data di scadenza ferie della sede.
   */
  Integer getNotYetUsedAccrued();
  
}
