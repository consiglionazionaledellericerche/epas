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

package manager.configurations;

import lombok.Data;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

/**
 * DTO per la configurazione.
 */
@Data
public class ConfigurationDto {

  public LocalDate validityBegin;
  public LocalDate validityEnd;
  
  public Integer validityYear;
  public Boolean toTheEnd = false;
  
  public Boolean booleanNewValue;
  public String stringNewValue;
  public Integer integerNewValue;
  public LocalDate localdateNewValue;
  public BlockType blockTypeNewValue;
  
  /**
   * Default constructor.
   */
  public ConfigurationDto() {
    //Empty constructor
  }
  
  /**
   * Constructor from configuration (contiene i valori del dto iniziale).
   */
  public ConfigurationDto(EpasParam epasParam, LocalDate beginDate, 
      LocalDate calculatedEnd, Object value) {
    
    if (epasParam.isGeneral()) {
      this.validityBegin = beginDate;
      this.validityEnd = calculatedEnd;
    }
    if (epasParam.isYearly()) {
      this.validityYear = LocalDate.now().getYear();
    }
    if (epasParam.isPeriodic()) {
      this.validityBegin = beginDate;
      this.validityEnd = calculatedEnd;
    }
    
    if (epasParam.epasParamValueType.equals(EpasParamValueType.ENUM)) {
      this.blockTypeNewValue = (BlockType) value;
    }

    if (epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      this.booleanNewValue = (Boolean) value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
      this.integerNewValue = (Integer) value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST)) {
      this.stringNewValue = EpasParamValueType.formatValue((IpList) value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
      this.localdateNewValue = (LocalDate) value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL)) {
      this.stringNewValue = (String) value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH)) {
      this.stringNewValue = EpasParamValueType.formatValue((MonthDay) value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
      this.integerNewValue = (Integer) value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME)) {
      this.stringNewValue = EpasParamValueType.formatValue((LocalTime) value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME_INTERVAL)) {
      this.stringNewValue = EpasParamValueType
          .formatValue((LocalTimeInterval) value);
    }
  }
}
