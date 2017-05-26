package models.enumerate;

/**
 * @author daniele
 * @since 26/05/17.
 */
public enum EventColor {
  ORANGE("#FFB847", "#FFAA24", "#633E05"),
  ACQUA_GREEN("#6CC8B9", "#158673", "#06322B"),
  ONE("#B1D13D", "#09822D", "black"),
  TWO("#41718E", "#1A159F", "white"),
  THREE("#FFDC47", "#4B4705", "#4B4705");

  public final String backgroundColor;
  public final String borderColor;
  public final String textColor;

  EventColor(String backgroundColor, String borderColor, String textColor) {
    this.backgroundColor = backgroundColor;
    this.borderColor = borderColor;
    this.textColor = textColor;
  }
}
