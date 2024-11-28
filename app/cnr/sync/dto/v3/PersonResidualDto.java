/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import lombok.Builder;
import lombok.Data;
import lombok.val;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Configuration;
import models.Person;

@Data
public class PersonResidualDto {

  private String number;
  private Integer residual;
  private String date;
  
  
  public static PersonResidualDto build(PersonStampingRecap psDto) {
    
    val personResidualDto = new PersonResidualDto();
    if (psDto != null) {
      personResidualDto.setNumber(psDto.person.getNumber());
      personResidualDto.setDate(LocalDate.now().toString());
      personResidualDto.setResidual(psDto.contractMonths.stream().mapToInt(cm -> cm.getValue().getRemainingMinutesLastYear() 
          + cm.getValue().getRemainingMinutesCurrentYear()).sum());
    }
    return personResidualDto;
  }
}
