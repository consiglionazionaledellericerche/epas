package models.enumerate;

/**
 * @author daniele
 * @since 26/05/17.
 */
public enum EventColor {


  ORANGE("#ffe5ad", "#ffcd61", "#966700"),
  GREEN("#dff0d8", "#b9e098", "#3c763d"),
  LIGHTPURPLE("#e5cee6", "#a77ca9", "#8a518e"),
  BLUE("#d9edf7", "#bce8f1", "#31708f"),
  YELLOW("#fcf8e3", "#f5d89c", "#8a6d3b"),
  RED("#f2dede", "#ebccd1", "#a94442"); // da usare per le assenze e basta


  public final String backgroundColor;
  public final String borderColor;
  public final String textColor;

  EventColor(String backgroundColor, String borderColor, String textColor) {
    this.backgroundColor = backgroundColor;
    this.borderColor = borderColor;
    this.textColor = textColor;
  }
}
