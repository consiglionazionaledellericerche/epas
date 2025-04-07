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

package manager.attestati.dto.insert;

import models.Certification;

/**
 * DTO per inserimento riga competenze di una sede.
 */
public class InserimentoRigaCompetenza extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaCompetenza(Certification certification) {

    codiceSede = certification.getPerson().getCurrentOffice().get().getCodeId();
    anno = certification.getYear();
    mese = certification.getMonth();

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.getPerson().getNumber();
    InsertRigaCompetenza rigaCompetenza = new InsertRigaCompetenza();
    rigaCompetenza.codiceCompetenza = deserializeCode(certification.getContent());
    rigaCompetenza.numOre = deserializeNumber(certification.getContent());
    dipendente.righeCompetenza.add(rigaCompetenza);
  }

  private String deserializeCode(String key) {
    return key.split(";")[0];
  }

  private int deserializeNumber(String key) {
    return Integer.parseInt(key.split(";")[1]);
  }

}
