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

package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.PersonDay;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per il PersonDay completi di persona.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonDayShowDto extends PersonDayShowTerseDto {

  private PersonShowTerseDto person;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un PersonDayShowDto contenente i valori 
   * dell'oggetto personDay passato.
   */
  public static PersonDayShowDto build(PersonDay pd) {
    PersonDayShowDto personDto = 
        modelMapper.map(PersonDayShowTerseDto.build(pd), PersonDayShowDto.class);
    personDto.setPerson(PersonShowTerseDto.build(pd.getPerson()));
    return personDto;
  }
}
