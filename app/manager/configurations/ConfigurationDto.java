package manager.configurations;

import controllers.Configurations;

import lombok.Data;

import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;

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
  
  /**
   * Default constructor.
   */
  public ConfigurationDto() {
    
  }
  
  /**
   * Constructor from configuration (contiene i valori del dto iniziale).
   */
  public ConfigurationDto(EpasParam epasParam, LocalDate beginDate, 
      LocalDate calculatedEnd, Object value ) {
    
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

    if (epasParam.epasParamValueType.equals(EpasParamValueType.BOOLEAN)) {
      this.booleanNewValue = (Boolean)value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.INTEGER)) {
      this.integerNewValue = (Integer)value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.IP_LIST)) {
      this.stringNewValue = EpasParamValueType.formatValue((IpList)value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALDATE)) {
      this.localdateNewValue = (LocalDate)value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.EMAIL)) {
      this.stringNewValue = (String)value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.DAY_MONTH)) {
      this.stringNewValue = EpasParamValueType.formatValue((MonthDay)value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.MONTH)) {
      this.integerNewValue = (Integer)value;
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME)) {
      this.stringNewValue = EpasParamValueType.formatValue((LocalTime)value);
    }
    if (epasParam.epasParamValueType.equals(EpasParamValueType.LOCALTIME_INTERVAL)) {
      this.stringNewValue = EpasParamValueType
          .formatValue((LocalTimeInterval)value);
    }
  }
}
