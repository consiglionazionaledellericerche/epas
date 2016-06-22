package manager.services.mealtickets;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;

import models.Contract;
import models.MealTicket;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Servizio meal tickets.
 * 
 * @author alessandro
 *
 */
public interface IMealTicketsService {

  Optional<MealTicketRecap> create(Contract contract);
  
  /**
   * Ritorna l'intervallo valido ePAS per il contratto riguardo la gestione dei buoni pasto. (scarto
   * la parte precedente a source se definita, e la parte precedente alla data inizio utilizzo per
   * la sede della persona).
   *
   * @return null in caso non vi siano giorni coperti dalla gestione dei buoni pasto.
   */
  Optional<DateInterval> getContractMealTicketDateInterval(Contract contract);
  
  
  /**
   * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock e dagli
   * estremi.
   * @param codeBlock codice blocco
   * @param first il primo codice
   * @param last l'ultimo codice
   * @param expireDate la data di scadenza
   * @return la lista dei buoni
   */
  List<MealTicket> buildBlockMealTicket(Long codeBlock, Integer first, Integer last,
      LocalDate expireDate);

  /**
   * Verifica che nel contratto precedente a contract siano avanzati dei buoni pasto assegnati. In
   * tal caso per quei buoni pasto viene modificata la relazione col contratto successivo e cambiata
   * la data di attribuzione in modo che ricada all'inizio del nuovo contratto.
   *
   * @return il numero di buoni pasto trasferiti fra un contratto e l'altro.
   */
  int mealTicketsLegacy(Contract contract);

  
}
