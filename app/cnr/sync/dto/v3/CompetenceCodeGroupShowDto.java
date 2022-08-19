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
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.CompetenceCodeGroup;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;
import org.modelmapper.ModelMapper;
import org.testng.collections.Lists;

/**
 * Dati esportati in Json per il PersonDay completi di persona.
 *
 * @author Cristian Lucchesi
 * @version 3
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetenceCodeGroupShowDto extends CompetenceCodeGroupShowTerseDto {

  private LimitType limitType;
  private Integer limitValue;
  private LimitUnit limitUnit;
  
  private List<CompetenceCodeShowTerseDto> competenceCodes = Lists.newArrayList();

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un PersonDayShowDto contenente i valori 
   * dell'oggetto personDay passato.
   */
  public static CompetenceCodeGroupShowDto build(CompetenceCodeGroup ccg) {
    val dto = modelMapper.map(ccg, CompetenceCodeGroupShowDto.class);
    dto.setCompetenceCodes(ccg.competenceCodes.stream()
        .map(cc -> CompetenceCodeShowTerseDto.build(cc)).collect(Collectors.toList()));
    return dto;
  }
}