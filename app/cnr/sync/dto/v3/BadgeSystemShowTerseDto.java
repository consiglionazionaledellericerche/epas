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

import cnr.sync.dto.v2.OfficeDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
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
 * DTO per esportare via JSON le informazioni principali di un gruppo badge.
 *
 * @author Cristian Lucchesi
 *
 */
@ToString
@Data
@EqualsAndHashCode(callSuper = true)
public class BadgeSystemShowTerseDto extends BadgeSystemShowMinimalDto {

  private String description;

  private Set<BadgeReaderShowMinimalDto> badgeReaders = Sets.newHashSet();
  
  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un BadgeSystemTerseDto contenente i valori 
   * dell'oggetto badgeSystem passato.
   */
  public static BadgeSystemShowTerseDto build(BadgeSystem badgeSystem) {
    val dto = modelMapper.map(badgeSystem, BadgeSystemShowTerseDto.class);
    dto.setOffice(OfficeDto.build(badgeSystem.getOffice()));
    dto.setBadgeReaders(badgeSystem.getBadgeReaders().stream()
        .map(br -> BadgeReaderShowMinimalDto.buildMinimal(br))
        .collect(Collectors.toSet()));
    return dto;
  }
}
