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

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.Badge;
import org.modelmapper.ModelMapper;

/**
 * DTO per l'esportazione via REST delle informazioni di un badge.
 *
 * @author Cristian Lucchesi
 * @version 3
 */
@StaticInject
@ToString
@Data
@EqualsAndHashCode(callSuper = true)
public class BadgeShowDto extends BadgeShowTerseDto {

  private Long badgeReaderId;
  private Long badgeSystemId;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BadgeShowDto contenente i valori 
   * dell'oggetto badge passato.
   */
  public static BadgeShowDto build(Badge badge) {
    val dto = modelMapper.map(badge, BadgeShowDto.class);
    dto.setBadgeReaderId(badge.badgeReader.id);
    if (badge.badgeSystem != null) {
      dto.setBadgeSystemId(badge.badgeSystem.id);
    }
    return dto;
  }
}
