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
import lombok.val;
import models.absences.AbsenceType;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per la tipologia di Assenza.
 *
 * @author Cristian Lucchesi
 * @version 3
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class AbsenceTypeShowDto extends AbsenceTypeShowTerseDto {

  private boolean timeForMealTicket;

  private Set<String> justifiedTypesPermitted = Sets.newHashSet();
  
  private Integer replacingTime;
 
  private String documentation; 
  
  private boolean reperibilityCompatible;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un AbsenceTypeShowDto contenente i valori 
   * dell'oggetto absenceType passato.
   */
  public static AbsenceTypeShowDto build(AbsenceType at) {
    val dto = modelMapper.map(at, AbsenceTypeShowDto.class);
    dto.setJustifiedTypesPermitted(
        at.getJustifiedTypesPermitted().stream()
          .map(jt -> jt.getName().name()).collect(Collectors.toSet()));    
    return dto;
  }
}