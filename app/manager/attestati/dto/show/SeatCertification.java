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

package manager.attestati.dto.show;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Json sulla situazione della persona per il mese specificato da Attestati.
 *
 * @author Alessandro Martelli
 */
public class SeatCertification {

  public int codiceSede;
  public int anno;
  public int mese;
  public List<PersonCertification> dipendenti = Lists.newArrayList();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(SeatCertification.class)
        .add("codiceSede", codiceSede)
        .add("anno", anno)
        .add("mese", mese)
        .add("dipendenti", dipendenti)
        .toString();
  }

  /**
   * Rappresentazione della riga di certificazione mensile di una persona.
   */
  public static class PersonCertification {
    public String matricola;
    public boolean validato;
    public int numBuoniPasto;
    public int numBuoniPastoElettronici;
    public List<RigaAssenza> righeAssenza = Lists.newArrayList();
    public List<RigaCompetenza> righeCompetenza = Lists.newArrayList(); //??
    public List<RigaFormazione> righeFormazione = Lists.newArrayList();

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(PersonCertification.class)
          .add("matricola", matricola)
          .add("validato", validato)
          .add("numBuoniPasto", numBuoniPasto)
          .add("numBuoniPastoElettronici", numBuoniPastoElettronici)
          .add("righeAssenza", righeAssenza)
          .toString();
    }
  }

}
