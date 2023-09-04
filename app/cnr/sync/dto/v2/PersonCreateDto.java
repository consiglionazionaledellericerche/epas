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

package cnr.sync.dto.v2;

import lombok.Data;
import lombok.val;
import models.Office;
import models.Person;
import models.Qualification;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione di una persona via REST.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
public class PersonCreateDto {
  
  @Required
  private String name;
  @Required
  private String surname;
  private String othersSurnames;
  private String fiscalCode;
  @Required
  private String email;
  private String number; //Matricola
  private String eppn;
  private String telephone;
  private String fax;
  private String mobile;
  private LocalDate birthday;
  private String residence;
  @Required
  private Integer qualification;
  @Required
  private Long officeId;
  
  /**
   * Nuova istanza di un oggetto person a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Person build(PersonCreateDto personDto) {
    ModelMapper modelMapper = new ModelMapper();
    val person = modelMapper.map(personDto, Person.class);
    if (personDto.getQualification() != null) {
      person.setQualification(
          ((Qualification) Qualification.findAll().stream()
              .filter(q -> 
                ((Qualification) q).getQualification() == personDto.getQualification().intValue())
              .findFirst().get()));
    }
    if (personDto.getOfficeId() != null) {
      person.setOffice(Office.findById(personDto.getOfficeId()));  
    }
    person.setBeginDate(LocalDate.now());
    return person;
  }
}