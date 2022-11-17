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
import com.google.common.collect.Sets;
import common.injection.StaticInject;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.BadgeSystem;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per un BadgeSystem (gruppo badge).
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@ToString
@Data
@EqualsAndHashCode(callSuper = true)
public class BadgeSystemShowDto extends BadgeSystemShowTerseDto {

  private Set<BadgeShowTerseDto> badges = Sets.newHashSet();

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BadgeSystemShowDto contenente i valori 
   * dell'oggetto badgeReader passato.
   */
  public static BadgeSystemShowDto build(BadgeSystem badgeSystem) {
    val dto = modelMapper.map(badgeSystem, BadgeSystemShowDto.class);

    dto.setBadgeReaders(badgeSystem.getBadgeReaders().stream()
        .map(bs -> BadgeReaderShowMinimalDto.buildMinimal(bs)).collect(Collectors.toSet()));
    dto.setBadges(badgeSystem.getBadges().stream().map(BadgeShowTerseDto::build)
        .collect(Collectors.toSet()));
    return dto;
  }
}
