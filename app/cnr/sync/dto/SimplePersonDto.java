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

import com.google.common.base.Function;
import models.Person;

/**
 * DTO per rappresentare via REST un estratto dei dati principali di una persona.
 */
public class SimplePersonDto {
  
  public long id;
  public String firstname;
  public String surname;
  public String updatedAt;
  public String email;
  public String uidCnr;

  /**
   * Applica la conversione da dto a oggetto.
   *
   * @author dario
   *
   */
  public enum FromPerson implements Function<Person, SimplePersonDto> {
    ISTANCE;
    
    @Override
    public SimplePersonDto apply(Person person) {
      SimplePersonDto personDto = new SimplePersonDto();
      personDto.id = person.id;
      personDto.firstname = person.getName();
      personDto.surname = person.getSurname();
      personDto.email = person.getEmail();
      personDto.uidCnr = person.getEppn();
      return personDto;
    }
  }
}