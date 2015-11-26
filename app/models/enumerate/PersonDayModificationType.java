package models.enumerate;

/**
 * enumerato relativo alle possibili modifiche del tempo di lavoro del personDay
 *
 * @author dario
 */
public enum PersonDayModificationType {

  p("Tempo calcolato togliendo dal tempo di lavoro la durata dell'intervallo pranzo"),
  d("Considerato presente se non ci sono codici di assenza (orario di lavoro autodichiarato)"),
  x("Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte");

  private String description;

  PersonDayModificationType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
