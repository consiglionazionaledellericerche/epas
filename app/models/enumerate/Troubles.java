package models.enumerate;

public enum Troubles {

  UNCOUPLED_FIXED("timbratura disaccoppiata persona fixed"),
  NO_ABS_NO_STAMP("no assenze giornaliere e no timbrature"),
  UNCOUPLED_WORKING("timbratura disaccoppiata giorno feriale"),
  UNCOUPLED_HOLIDAY("timbratura disaccoppiata giorno festivo"),
  NOT_ENOUGTH_WORKTIME("tempo a lavoro insufficiente");

  public String description;

  Troubles(String description) {
    this.description = description;
  }

}
