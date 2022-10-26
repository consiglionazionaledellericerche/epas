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

import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.Office;
import models.Person;
import models.flows.Group;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione di una persona via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class GroupCreateDto {
  
  @Required
  private String name;
  private String description;
  private Boolean sendFlowsEmail;
  @Required
  private Long officeId;
  @Required
  private Long managerId;  
  private String externalId;
  private LocalDate endDate;
  
  /**
   * Nuova istanza di un oggetto person a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Group build(GroupCreateDto groupDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val group = modelMapper.map(groupDto, Group.class);
    if (groupDto.getOfficeId() != null) {
      group.setOffice(Office.findById(groupDto.getOfficeId()));  
    }
    if (groupDto.getManagerId() != null) {
      group.setManager(Person.findById(groupDto.getManagerId()));  
    }
    return group;
  }
}