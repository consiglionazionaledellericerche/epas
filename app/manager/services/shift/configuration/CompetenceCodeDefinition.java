package manager.services.shift.configuration;

public enum CompetenceCodeDefinition {

  WORKING_DAY_SHIFT("T1"),
  NIGHT_SHIFT("T2"),
  HOLIDAY_SHIFT("T3");
  
  public String code;
  
  private CompetenceCodeDefinition(String code) {
    this.code = code;
  }
}
