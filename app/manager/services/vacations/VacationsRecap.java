package manager.services.vacations;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Contiene il riepilogo ferie per un certo anno di un contratto.
 *
 * @author alessandro
 *
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class VacationsRecap {
  
  /**
   * I dati della richiesta.
   */
  private VacationsRequest vacationsRequest;

  /**
   * Risultato ferie anno passato.
   */
  private VacationsTypeResult vacationsLastYear;
  
  /**
   * Risultato ferie anno corrente.
   */
  private VacationsTypeResult vacationsCurrentYear;
  
  /**
   * Risultato permessi anno corrente.
   */
  private VacationsTypeResult permissions;
  

}
