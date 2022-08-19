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

import com.google.common.collect.Lists;
import java.util.List;

/**
 * DTO per richiesta inserimento dati su attestati.
 */
public class RichiestaInserimentoAttestati {

  public String codiceSede;
  public int anno;
  public int mese;

  public List<ItemDipendente> dipendenti = Lists.newArrayList();

  /**
   * DTO per raprresentare tutti i dati da inviare per un dipendente.
   */
  public static class ItemDipendente {

    public String matricola;
    public Integer numBuoniPasto = null;
    public Integer numBuoniPastoElettronici = null;
    public List<InsertRigaAssenza> righeAssenza = Lists.newArrayList();
    public List<InsertRigaCompetenza> righeCompetenza = Lists.newArrayList();
    public List<InsertRigaFormazione> righeFormazione = Lists.newArrayList();
  }

  /**
   * DTO per raprresentare la riga con le competenze.
   */
  public static class InsertRigaCompetenza {

    public String codiceCompetenza;
    public int numOre;
  }

  /**
   * DTO per raprresentare la riga ccn le ore di formazione.
   */
  public static class InsertRigaFormazione {

    public int giornoInizio;
    public int giornoFine;
    public int numOre;
  }

  /**
   * DTO per raprresentare la riga con le assenze.
   */
  public static class InsertRigaAssenza {

    public String codiceAssenza;
    public int giornoInizio;
    public int giornoFine;
  }

}