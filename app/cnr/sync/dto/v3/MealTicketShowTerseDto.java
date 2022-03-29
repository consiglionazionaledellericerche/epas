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
import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.Badge;
import models.Contract;
import models.MealTicket;
import models.Office;
import models.Person;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * DTO per esportare via JSON le informazioni principali di un buono pasto.
 *
 * @author Cristian Lucchesi
 *
 */
@ToString
@Data
@EqualsAndHashCode
public class MealTicketShowTerseDto {

  private Long id;
  
  private PersonShowTerseDto person;

  //public Contract contract;

  private Integer year;

  private LocalDate date;

  private String block; /*esempio 5941 3165 01 */
  
  private BlockType blockType;

  private Integer number;

  private String code; /* concatenzazione block + number */

  //public LocalDate expireDate;
  
  //private boolean returned = false;
  
  //public Office office;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un MealTicketShowTerseDto contenente i valori 
   * dell'oggetto mealTicket passato.
   */
  public static MealTicketShowTerseDto build(MealTicket mealTicket) {
    val dto = modelMapper.map(mealTicket, MealTicketShowTerseDto.class);
    dto.setPerson(PersonShowTerseDto.build(mealTicket.contract.person));
    return dto;
  }

}