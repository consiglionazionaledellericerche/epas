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
import lombok.Data;
import lombok.val;
import manager.configurations.EpasParam;
import models.Configuration;

@Data
public class ConfigurationOfficeDto {

  private EpasParam epasParam;
  private String fieldValue;
  private String beginDate;
  private String endDate;
  
  public static ConfigurationOfficeDto build(Configuration configuration) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val configurationDto = modelMapper.map(configuration, ConfigurationOfficeDto.class);
    if (configuration.getEpasParam() != null) {
      configurationDto.setEpasParam(configuration.getEpasParam());
    }
    return configurationDto;
  }
}
