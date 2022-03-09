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
import lombok.val;
import models.Badge;
import org.modelmapper.ModelMapper;

/**
 * Dati per l'aggiornamento via REST di un badge associato ad una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class BadgeUpdateDto {

  private String code;

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  /**
   * Nuova istanza di un oggetto Badge a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Badge build(BadgeUpdateDto badgeDto) {
    val badge = modelMapper.map(badgeDto, Badge.class);

    return badge;
  }

  /**
   * Aggiorna il codice dell'oggetto Badge passato con quello
   * presenti nell'instanza di questo DTO.
   */
  public void update(Badge badge) {
    badge.code = getCode();
  }
}