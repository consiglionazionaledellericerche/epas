package manager.services.vacations;

import manager.services.vacations.impl.VacationsRecap.VacationsRequest;

/**
 * Contiene il riepilogo ferie per un certo anno di un contratto.
 * 
 * @author alessandro
 *
 */
public interface IVacationsRecap {
  
  /**
   * I dati della richiesta per generare il recap.
   * @return la richiesta.
   */
  VacationsRequest getVacationsRequest();

  /**
   * Riepilogo ferie anno passato.
   * @return il recap.
   */
  IVacationsTypeResult getVacationsLastYear();
  
  /**
   * Riepilogo ferie anno corrente.
   * @return il recap.
   */
  IVacationsTypeResult getVacationsCurrentYear();
  
  /**
   * Riepilogo permessi.
   * @return il recap.
   */
  IVacationsTypeResult getPermissions();
  
  /**
   * True se le ferie dell'anno passato sono scadute.
   */
  boolean isExpireLastYear();

  /**
   * True se il contratto scade prima della fine dell'anno.
   */
  boolean isExpireBeforeEndYear();

  /**
   * True se il contratto inizia dopo l'inizio dell'anno.
   */
  boolean isActiveAfterBeginYear();
}
