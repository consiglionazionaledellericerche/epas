/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.services.mealtickets;

import com.google.common.base.Optional;
import it.cnr.iit.epas.DateInterval;
import java.util.List;
import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Office;
import models.dto.MealTicketComposition;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;

/**
 * Servizio meal tickets.
 *
 * @author Alessandro Martelli
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
   *
   * @param codeBlock codice blocco
   * @param first il primo codice
   * @param last l'ultimo codice
   * @param expireDate la data di scadenza
   * @return la lista dei buoni
   */
  List<MealTicket> buildBlockMealTicket(String codeBlock, BlockType blockType, Integer first, 
      Integer last, LocalDate expireDate, Office office);

  /**
   * Verifica che nel contratto precedente a contract siano avanzati dei buoni pasto assegnati. In
   * tal caso per quei buoni pasto viene modificata la relazione col contratto successivo e cambiata
   * la data di attribuzione in modo che ricada all'inizio del nuovo contratto.
   *
   * @return il numero di buoni pasto trasferiti fra un contratto e l'altro.
   */
  int mealTicketsLegacy(Contract contract);
  
  /**
   * Genera una composizione che determina con che tipo di buoni pasto si copre la maturazione
   * mensile dei buoni in base ai giorni di presenza.
   *
   * @param recap il recap della situazione dei buoni pasto
   * @param monthRecap il recap sul contratto mensile
   * @param contract il contratto su cui verificare la situazione dei buoni pasto
   * @return la composizione con cui gestire la maturazione dei buoni pasto in un anno/mese.
   */
  MealTicketComposition whichBlock(List<BlockMealTicket> list, ContractMonthRecap monthRecap, 
      Contract contract);

  
}
