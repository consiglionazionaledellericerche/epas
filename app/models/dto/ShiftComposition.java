package models.dto;

import lombok.Builder;
import lombok.Data;
import manager.services.shift.configuration.OrganizationShiftConfiguration;

@Data
public class ShiftComposition {

  private int quantity;
  private OrganizationShiftConfiguration organizationShiftConfiguration;
  
  public ShiftComposition(OrganizationShiftConfiguration organizationShiftConfiguration, int quantity) {
    this.organizationShiftConfiguration =organizationShiftConfiguration;
    this.quantity = quantity;
  }
  
}
