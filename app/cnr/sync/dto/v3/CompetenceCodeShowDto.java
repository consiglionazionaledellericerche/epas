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
import models.CompetenceCode;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;
import org.modelmapper.ModelMapper;

/**
 * DTO per l'esportazione via REST delle informazioni 
 * di un codice di competenza (straordinari, turni, etc).
 *
 * @author Cristian Lucchesi
 * @version 3
 *
 */
@StaticInject
@ToString
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetenceCodeShowDto extends CompetenceCodeShowTerseDto {

  private boolean disabled;

  private LimitType limitType;
  private Integer limitValue;
  private LimitUnit limitUnit;
  
  private CompetenceCodeGroupShowTerseDto competenceCodeGroup;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un StampingShowTerseDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static CompetenceCodeShowDto build(CompetenceCode competenceCode) {
    val dto = modelMapper.map(competenceCode, CompetenceCodeShowDto.class);
    if (competenceCode.getCompetenceCodeGroup() != null) {
      dto.setCompetenceCodeGroup(
          CompetenceCodeGroupShowTerseDto.build(competenceCode.getCompetenceCodeGroup()));
    }
    return dto;
  }
}