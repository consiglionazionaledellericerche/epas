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
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.Badge;
import org.modelmapper.ModelMapper;

/**
 * DTO per esportare via JSON le informazioni principali di un badge.
 *
 * @author Cristian Lucchesi
 *
 */
@ToString
@Data
@EqualsAndHashCode
public class BadgeShowTerseDto {

  private Long id;
  private String code;
  private PersonShowTerseDto person;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BadgeShowTerseDto contenente i valori 
   * dell'oggetto badge passato.
   */
  public static BadgeShowTerseDto build(Badge badge) {
    val dto = modelMapper.map(badge, BadgeShowTerseDto.class);
    dto.setPerson(PersonShowTerseDto.build(badge.getPerson()));
    return dto;
  }

}