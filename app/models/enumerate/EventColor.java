package models.enumerate;

/**
 * Enumerato per la gestione dei colori degli eventi per i turni.
 * @author daniele
 * @since 26/05/17.
 */
public enum EventColor {

  GREEN("#dff0d8", "#b9e098", "#3c763d"),
  YELLOW("#fcf8e3", "#f5d89c", "#8a6d3b"),
  BLUE("#d9edf7", "#bce8f1", "#31708f"),
  BROWN("#f2ddb8", "#d1b27f", "#7f6943"),
  LIGHTPURPLE("#f4ecf9", "#e7cde8", "#8a518e"),
  ORANGE("#ffe5ad", "#ffcd61", "#966700"),
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
