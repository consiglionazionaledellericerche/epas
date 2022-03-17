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
import models.PersonChildren;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per il figlio/figlia di una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class ChildrenShowDto {
  
  private Long id;
  private String name;
  private String surname;
  private LocalDate bornDate;
  private String fiscalCode;
  private String externalId;
  public LocalDateTime updatedAt;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un PersonShowTerseDto contenente i valori 
   * dell'oggetto person passato.
   */
  public static ChildrenShowDto build(PersonChildren children) {
    val dto = modelMapper.map(children, ChildrenShowDto.class);
    dto.setFiscalCode(children.taxCode);
    return dto;
  }
}