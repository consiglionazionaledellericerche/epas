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
import common.injection.StaticInject;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.Data;
import lombok.ToString;
import lombok.val;
import models.Stamping;
import models.Stamping.WayType;
import org.modelmapper.ModelMapper;

/**
 * DTO per l'esportazione via REST delle informazioni di una timbratura.
 *
 * @author Cristian Lucchesi
 * @version 3
 */
@StaticInject
@ToString
@Data
public class StampingShowDto {

  private Long id;
  private LocalDateTime date;
  private WayType way;
  private String stampType;
  private String place;
  private String reason;
  private boolean markedByAdmin;
  private boolean markedByEmployee;
  private boolean markedByTelework;
  private String note;
  private String stampingZone;
  private PersonShowTerseDto person;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un StampingShowTerseDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static StampingShowDto build(Stamping stamping) {
    val dto = modelMapper.map(stamping, StampingShowDto.class);
    dto.setPerson(PersonShowTerseDto.build(stamping.getPersonDay().getPerson()));
    dto.stampType = stamping.getStampType() != null ? stamping.getStampType().getCode() : null;
    return dto;
  }
}