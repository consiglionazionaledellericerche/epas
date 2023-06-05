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


package synch.perseoconsumers.office;

import com.google.common.base.MoreObjects;

/**
 * DTO che rappresenta i dati degli istituti presenti su Perseo.
 */
public class PerseoInstitute {
  public int id; //perseoId
  public String name;
  public String code;
  public String cds;
  public String dismissionDate;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id).add("name", name).add("code", code).add("cds", cds)
            .toString();
  }
 
}
