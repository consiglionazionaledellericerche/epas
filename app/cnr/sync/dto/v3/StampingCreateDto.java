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
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Stamping;
import models.enumerate.StampTypes;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di una timbratura di una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class StampingCreateDto extends StampingUpdateDto {

  @Required
  private String badgeNumber;
  
  @Required
  private LocalDateTime dateTime;

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  /**
   * Nuova istanza di un oggetto Stamping a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Stamping build(StampingCreateDto stampingDto) {
    val stamping = modelMapper.map(stampingDto, Stamping.class);

    if (stampingDto.getReasonType() != null) {
      stamping.setStampType(StampTypes.byCode(stampingDto.getReasonType().name()));
    }

    return stamping;
  }

}