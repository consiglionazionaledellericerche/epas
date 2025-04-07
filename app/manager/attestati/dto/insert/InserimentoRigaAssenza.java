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
 * DTO per inserimento riga assenze di una sede.
 */
public class InserimentoRigaAssenza extends RichiestaInserimentoAttestati {

  /**
   * Constructor.
   */
  public InserimentoRigaAssenza(Certification certification) {

    codiceSede = certification.getPerson().getCurrentOffice().get().getCodeId();
    anno = certification.getYear();
    mese = certification.getMonth();

    ItemDipendente dipendente = new ItemDipendente();
    dipendenti.add(dipendente);

    dipendente.matricola = certification.getPerson().getNumber();
    InsertRigaAssenza rigaAssenza = new InsertRigaAssenza();
    rigaAssenza.codiceAssenza = deserializeCode(certification.getContent());
    rigaAssenza.giornoInizio = deserializeBegin(certification.getContent());
    rigaAssenza.giornoFine = deserializeEnd(certification.getContent());
    dipendente.righeAssenza.add(rigaAssenza);
  }

  private String deserializeCode(String key) {
    return key.split(";")[0];
  }

  private int deserializeBegin(String key) {
    return Integer.parseInt(key.split(";")[1]);
  }

  private int deserializeEnd(String key) {
    return Integer.parseInt(key.split(";")[2]);
  }

}
