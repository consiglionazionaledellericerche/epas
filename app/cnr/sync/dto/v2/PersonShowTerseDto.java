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

package cnr.sync.dto.v2;

import lombok.Data;
import models.Person;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per la Persona.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class PersonShowTerseDto {
  
  private Long id;
  private String fullname;
  private String fiscalCode;
  private String email;
  private String number; //Matricola
  private String eppn;

  /**
   * Nuova instanza di un PersonShowTerseDto contenente i valori 
   * dell'oggetto person passato.
   */
  public static PersonShowTerseDto build(Person person) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    return modelMapper.map(person, PersonShowTerseDto.class);
  }
}