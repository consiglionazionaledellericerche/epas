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

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import models.Contract;
import models.MealTicket;
import models.Person;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;

/**
 * Blocco di buoni pasto.
 *
 * @author Alessandro Martelli
 * 
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class BlockMealTicket {

  private Contract contract;
  private String codeBlock;
  private BlockType blockType;
  private List<MealTicket> mealTickets;

  protected BlockMealTicket(String codeBlock, BlockType blockType) {

    this.codeBlock = codeBlock;
    this.blockType = blockType;
    this.mealTickets = Lists.newArrayList();
  }

  /**
   * La dimensione del blocchetto.
   */
  public Integer getDimBlock() {
    return this.mealTickets.size();
  }

  /**
   * Il numero di buoni rimanenti all'interno del blocchetto. N.B. Il metodo ritorna un valore
   * valido solo se la variabile lazy used è valorizzata per tutti i buoni pasto del blocchetto.
   */
  public Integer getRemaining() {
    Integer consumed = this.getConsumed();
    if (consumed == null) {
      return null;
    }
    return this.getDimBlock() - this.getConsumed();
  }

  /**
   * Il numero di buoni consumati all'interno del blocchetto. N.B. Il metodo ritorna un valore
   * valido solo se la variabile lazy used è valorizzata per tutti i buoni pasto del blocchetto.
   */
  public Integer getConsumed() {
    Integer count = 0;
    for (MealTicket mealTicket : this.mealTickets) {
      if (mealTicket.used == null) {
        return null;
      }
      if (mealTicket.used) {
        count++;
      }
    }
    return count;
  }

  /**
   * Il giorno di attribuzione del blocco.
   */
  public LocalDate getDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).date;
    }
    return null;
  }

  /**
   * Il giorno di scadenza del blocchetto.
   *
   * @return il giorno di scadenza dei buoni appartenenti al blocchetto.
   */
  public LocalDate getExpireDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).expireDate;
    }
    return null;
  }

  /**
   * L'amministratore che ha fatto l'inserimento del blocchetto.
   *
   * @return l'amministratore assegnatario del blocchetto.
   */
  public Person getAdmin() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).admin;
    }
    return null;
  }

  /**
   * La data di consegna del blocchetto (inserimento in ePAS).
   *
   * @return la data di consegna (inserimento ePAS) del blocchetto.
   */
  public LocalDate getReceivedDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).date;
    }
    return null;
  }

  /**
   * Il primo numero dei buoni del blocco.
   *
   * @return il primo number del blocco.
   */
  public int getFirst() {
    return this.mealTickets.get(0).number;
  }

  /**
   * L'ultimo numero dei buoni del blocco.
   *
   * @return l'ultimo number del blocco.
   */
  public int getLast() {
    return this.mealTickets.get(this.mealTickets.size() - 1).number;
  }

  /**
   * Se l'istanza contiene tutti blocchi returned. Blocchi misti non sono permessi e causano
   * una eccezione.
   *
   * @return Se l'istanza contiene tutti blocchi returned, false altrimenti.
   */
  public boolean isReturned() {
    boolean returned = this.mealTickets.get(0).returned;
    for (MealTicket mealTicket : this.mealTickets) {
      Verify.verify(mealTicket.returned == returned);
    }
    return returned;
  }

}
