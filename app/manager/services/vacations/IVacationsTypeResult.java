package manager.services.vacations;

import com.google.common.collect.ImmutableList;

import it.cnr.iit.epas.DateUtility;

import lombok.Getter;

import manager.services.vacations.impl.AccruedResult;
import manager.services.vacations.impl.VacationsRecap;
import manager.services.vacations.impl.VacationsTypeResult;
import manager.services.vacations.impl.VacationsRecap.VacationsRequest;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

import models.Absence;

import org.joda.time.LocalDate;

/**
 * Il risultato per il TypeVacation per la richiesta VacationsRequest. 
 * 
 * @author alessandro
 */
public interface IVacationsTypeResult {

  /**
   * I Dati della richiesta (comuni ad ogni tipo di assenza).
   * @return
   */
  VacationsRequest getVacationsRequest();
  
  /**
   * Il tipo di assenza del risultato.
   * @return
   */
  TypeVacation getTypeVacation();
  
  /**
   * Le assenze usate del tipo di assenza.
   * @return
   */
  ImmutableList<Absence> getAbsencesUsed();
  
  /**
   * Le assenza da inizilizzazione del tipo di assenza.
   * @return
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
   * @return
   */
  Integer getUsed();

  /**
   * Logica per assenze rimanenti.
   * @return
   */
  Integer getNotYetUsed();
  
  /**
   * Assenze rimanenti sul totale che il dipendente avrebbe potuto prendere. (Tabellone Danila)
   * @return
   */
  Integer getRemaining();
  
}
