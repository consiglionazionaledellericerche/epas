package models.enumerate;

/**
 * @author daniele
 * @since 26/05/17.
 */
public enum EventColor {
  ONE("#C7E55A", "#09822D", "black"),
  TWO("#d9edf7", "#1A159F", "white"),
  THREE("#FFDC47", "#4B4705", "#4B4705"),
  FOUR("#f2dede", "#ebccd1", "#A94442")
  ;

  public final String backgroundColor;
  public final String borderColor;
  public final String textColor;

  EventColor(String backgroundColor, String borderColor, String textColor) {
    this.backgroundColor = backgroundColor;
    this.borderColor = borderColor;
    this.textColor = textColor;
  }
}
