package models.enumerate;

public enum Troubles {

  UNCOUPLED_FIXED("timbratura disaccoppiata persona fixed"),
  NO_ABS_NO_STAMP("no assenze giornaliere e no timbrature"),
  UNCOUPLED_WORKING("timbratura disaccoppiata giorno feriale"),
  UNCOUPLED_HOLIDAY("timbratura disaccoppiata giorno festivo");

  public String description;

  private Troubles(String description) {
    this.description = description;
  }

  public boolean isStampingTrouble(Troubles trouble) {
    return trouble.equals(UNCOUPLED_FIXED)
            || trouble.equals(UNCOUPLED_WORKING)
            || trouble.equals(UNCOUPLED_HOLIDAY);
  }
}
