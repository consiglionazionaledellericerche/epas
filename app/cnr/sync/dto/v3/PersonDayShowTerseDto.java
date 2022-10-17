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
import com.google.common.collect.Lists;
import common.injection.StaticInject;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.PersonDay;
import org.modelmapper.ModelMapper;

/**
 * DTO per esportare via JSON le informazioni principali di un PersonDay.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(of = "id")
public class PersonDayShowTerseDto {

  private Long id;
  private LocalDate date;
  private int timeAtWork;
  private int difference;
  private int progressive;
  private boolean isTicketAvailable;
  private boolean isHoliday;

  private List<StampingShowTerseDto> stampings = Lists.newArrayList();

  private List<AbsenceShowTerseDto> absences = Lists.newArrayList();
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un PersonDayShowTerseDto contenente i valori 
   * dell'oggetto personDay passato.
   */
  public static PersonDayShowTerseDto build(PersonDay pd) {
    val pdDto = modelMapper.map(pd, PersonDayShowTerseDto.class);

    pdDto.setAbsences(
        pd.getAbsences().stream().map(a -> AbsenceShowTerseDto.build(a))
          .collect(Collectors.toList())); 
   
    pdDto.setStampings(
        pd.getStampings().stream().map(s -> StampingShowTerseDto.build(s))
        .collect(Collectors.toList()));
    return pdDto;
  }
}