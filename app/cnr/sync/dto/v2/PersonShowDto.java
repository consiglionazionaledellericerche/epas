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

import com.beust.jcommander.internal.Sets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Person;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per la Persona.
 *
 * @author Cristian Lucchesi
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonShowDto extends PersonShowTerseDto {
  
  private String name;
  private String surname;
  private String othersSurnames;
  private String telephone;
  private String fax;
  private String mobile;
  private Integer qualification;
  private Set<String> badges = Sets.newHashSet();
  private OfficeDto office;
  private LocalDateTime updatedAt;

  /**
   * Nuova instanza di un PersonShowDto contenente i valori 
   * dell'oggetto person passato.
   */
  public static PersonShowDto build(Person person) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    modelMapper.typeMap(Person.class, PersonShowDto.class).addMappings(mapper -> {
      mapper.map(src -> src.getQualification().getQualification(),
          PersonShowDto::setQualification);
    });
    val personDto = modelMapper.map(person, PersonShowDto.class);
    personDto.setBadges(
        person.getBadges().stream().map(b -> b.getCode()).collect(Collectors.toSet()));
    return personDto;
    
  }
}
