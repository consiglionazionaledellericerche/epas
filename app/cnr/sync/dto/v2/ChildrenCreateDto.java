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
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.Person;
import models.PersonChildren;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione di un figlio/a via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class ChildrenCreateDto {
  
  @Required
  private String name;
  @Required
  private String surname;
  @Required
  private LocalDate bornDate;
  private String fiscalCode;
  private String externalId;

  @Required
  private Long personId;
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova istanza di un oggetto person a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static PersonChildren build(ChildrenCreateDto dto) {
    val children = modelMapper.map(dto, PersonChildren.class);
    if (dto.getPersonId() != null) {
      children.setPerson(Person.findById(dto.getPersonId()));  
    }
    children.setTaxCode(dto.getFiscalCode());
    return children;
  }
}