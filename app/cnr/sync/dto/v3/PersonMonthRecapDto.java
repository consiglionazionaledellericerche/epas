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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.val;
import org.testng.collections.Lists;

/**
 * DTO per l'esportazione via REST delle informazioni del riepilogo mensile
 * assenze/presenze in un determinato mese.
 *
 * @version 3
 * @author Cristian Lucchesi
 *
 */
@AllArgsConstructor
@Builder
@Data
public class PersonMonthRecapDto {

  private Integer year;
  private Integer month;
  private PersonShowTerseDto person;
  private int basedWorkingDays;

  @Builder.Default
  private List<PersonDayShowTerseDto> personDays = Lists.newArrayList();

  @Override
  public boolean equals(Object other) {
    if (! (other instanceof PersonMonthRecapDto)) {
      return false;
    }
    val otherRecap = (PersonMonthRecapDto) other;
    return (year != null && year.equals(otherRecap.getYear()) 
        && (month != null && month.equals(otherRecap.getMonth()))
        && (person != null && person.getId() != null && otherRecap.getPerson() != null 
        && person.getId().equals(otherRecap.getPerson().getId())));
  }

  @Override
  public int hashCode() {
    return person.hashCode() + year.hashCode() + month.hashCode();
  }
}
