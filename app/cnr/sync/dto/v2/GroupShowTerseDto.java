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

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.flows.Group;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in forma ridotta ed in Json per un gruppo di persone.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class GroupShowTerseDto {
  
  private Long id;
  private String name;
  private String description;
  private LocalDate endDate;
  private PersonShowTerseDto manager;
  private String externalId;
  private LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un GroupShowTerseDto contenente i valori 
   * dell'oggetto group passato.
   */
  public static GroupShowTerseDto build(Group group) {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val groupDto = modelMapper.map(group, GroupShowTerseDto.class);
    groupDto.setManager(PersonShowTerseDto.build(group.manager));
    return groupDto;
  }
}