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

package cnr.sync.dto;

/**
 * DTO per rappresentare i dati una persona via REST.
 */
public class PersonDto {

  public int id;
  public String createdAt;
  public String updatedAt;
  public String firstname;
  public String otherNames;
  public String surname;
  public String otherSurnames;
  public String taxCode;
  public String birthDate;
  public String email;
  public String emailCnr;
  public String uidCnr;
  public Address domicile;
  public Address residence;
  public int number;
  public String department;

  /**
   * Sotto classe definizione dell'indirizzo.
   *
   * @author dario
   *
   */
  protected class Address {
    public String street;
    public String city;
    public String state;
    public int postalcode;
    public String country;
  }
}