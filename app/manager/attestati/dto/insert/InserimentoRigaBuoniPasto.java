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
 * DTO per inserimento riga buoni pasto di una sede.
 */
public class InserimentoRigaBuoniPasto extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaBuoniPasto(Certification certification) {
    codiceSede = certification.getPerson().getCurrentOffice().get().getCodeId();
    anno = certification.getYear();
    mese = certification.getMonth();

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.getPerson().getNumber();
    dipendente.numBuoniPasto = Integer.parseInt(certification.getContent().split(";")[0]);
    dipendente.numBuoniPastoElettronici = Integer
        .parseInt(certification.getContent().split(";")[1]);
  }

}
