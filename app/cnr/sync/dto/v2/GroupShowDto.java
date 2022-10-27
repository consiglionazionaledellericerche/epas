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

import com.beust.jcommander.internal.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.flows.Group;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per un gruppo di persone.
 *
 * @author Cristian Lucchesi
 *
 */ 
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupShowDto extends GroupShowTerseDto {
  
  private Set<PersonShowTerseDto> people = Sets.newHashSet();
  private OfficeDto office;
  
  /**
   * Nuova instanza di un GroupShowDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static GroupShowDto build(Group group) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowDto.class);
    groupDto.setManager(PersonShowTerseDto.build(group.getManager()));
    groupDto.setPeople(
        group.getPeople().stream().map(p -> PersonShowTerseDto.build(p))
          .collect(Collectors.toSet()));
    return groupDto;
  }
}