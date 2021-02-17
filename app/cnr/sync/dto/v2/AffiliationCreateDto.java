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

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.val;
import models.Person;
import models.flows.Affiliation;
import models.flows.Group;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di una affiliazione di una persona ad
 * un gruppo.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class AffiliationCreateDto {

  @Required
  private Long groupId;
  @Required
  private Long personId;
  private BigDecimal percentage;
  @Required
  private LocalDate beginDate;
  private LocalDate endDate;
  private String externalId;
  
  /**
   * Nuova istanza di un oggetto affiliation a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Affiliation build(AffiliationCreateDto affiliationDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val affiliation = modelMapper.map(affiliationDto, Affiliation.class);
    if (affiliationDto.getGroupId() != null) {
      affiliation.setGroup(Group.findById(affiliationDto.getGroupId()));  
    }
    if (affiliationDto.getPersonId() != null) {
      affiliation.setPerson(Person.findById(affiliationDto.getPersonId()));  
    }
    return affiliation;
  }
}