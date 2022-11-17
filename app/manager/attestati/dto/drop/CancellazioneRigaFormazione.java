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

package manager.attestati.dto.drop;

import com.google.common.collect.Lists;
import java.util.List;
import models.Certification;

/**
 * DTO per la cancellazione riga formazione di una sede.
 */
public class CancellazioneRigaFormazione {

  public int codiceSede;
  public int anno;
  public int mese;

  public List<ItemDipendente> dipendenti = Lists.newArrayList();

  /**
   * DTO per dipendente con le sue righe di formazione.
   */
  public static class ItemDipendente {
    public String matricola;
    public List<DropItem> righeFormazione = Lists.newArrayList();
  }

  /**
   * DTO che rappresenta una riga da cancellare.
   */
  public static class DropItem {
    public int id;
  }

  /**
   * Constructor.
   */
  public CancellazioneRigaFormazione(Certification certification) {
    codiceSede = Integer.parseInt(certification.getPerson().getOffice().getCodeId());
    anno = certification.getYear();
    mese = certification.getMonth();

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.getPerson().getNumber();

    DropItem dropItem = new DropItem();
    dropItem.id = certification.getAttestatiId();
    dipendente.righeFormazione.add(dropItem);
  }

}