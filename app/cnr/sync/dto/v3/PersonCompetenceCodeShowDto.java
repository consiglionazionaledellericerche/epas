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
import injection.StaticInject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;
import lombok.val;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.PersonCompetenceCodes;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;
import models.enumerate.StampTypes;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;
import play.data.validation.Unique;

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
public class PersonCompetenceCodeShowDto {

  private Long id;
  private LocalDate beginDate;
  private LocalDate endDate;
  
  private CompetenceCodeShowTerseDto competenceCode;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un StampingShowTerseDto contenente i valori 
   * dell'oggetto stamping passato.
   */
  public static PersonCompetenceCodeShowDto build(PersonCompetenceCodes personCompetenceCodes) {
    val dto = modelMapper.map(personCompetenceCodes, PersonCompetenceCodeShowDto.class);
    dto.setCompetenceCode(CompetenceCodeShowTerseDto.build(personCompetenceCodes.competenceCode));
    return dto;
  }
}