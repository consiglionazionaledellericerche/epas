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

import cnr.sync.dto.v2.GroupShowDto;
import cnr.sync.dto.v2.PersonShowTerseDto;
import helpers.validators.PeriodEndDateCheck;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import models.Institute;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Dati esportati in Json per l'Ufficio.
 *
 * @author Cristian Lucchesi
 *
 */
@ToString
@Data
public class OfficeShowTerseDto {

  private Long id;
  private Long perseoId;
  private String name;

  //Codice della sede, per esempio per la sede di Pisa è "044000"
  private String code;

  //sedeId, serve per l'invio degli attestati, per esempio per la sede di Pisa è "223400"
  private String codeId;
  private String address;
  private LocalDate joiningDate;
  private LocalDate beginDate;
  private LocalDate endDate;

  private Long instituteId;
  private boolean headQuarter = false;
  private LocalDateTime updatedAt;

  /**
   * Nuova instanza di un OfficeShowTerseDto contenente i valori 
   * dell'oggetto Office passato.
   */
  public static OfficeShowTerseDto build(Office office) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val officeDto = modelMapper.map(office, OfficeShowTerseDto.class);
    if (office.getInstitute() != null) {
      officeDto.setInstituteId(office.getInstitute().getId());
    }
    return officeDto;
  }
}