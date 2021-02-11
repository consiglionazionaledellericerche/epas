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

package synch.perseoconsumers.people;

/**
 * Le informazioni sulla persona utili a epas da Perseo.
 *
 * @author Alessandro Martelli
 *
 */
public class PerseoPerson {
  public Long id;
  public String firstname;
  public String surname;
  public String email;
  public String number;
  public Integer qualification;
  public Long departmentId;
  public String updatedAt;
  public String eppn;
}
