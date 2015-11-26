/**
 *
 */
package helpers.attestati;

/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione dati delle
 * assenze/competenze/buoni mensa inviati al sistema degli attestati del CNR.
 *
 * @author cristian
 */
public final class RispostaElaboraDati {
  private final String cognomeNome;
  private final String matricola;
  private boolean ok = false;
  private String absencesSent = null;
  private String competencesSent = null;
  private String mealTicketSent = null;
  private String trainingHoursSent = null;
  private String problems = null;

  public RispostaElaboraDati(String cognomeNome, String matricola) {
    this.cognomeNome = cognomeNome;
    this.matricola = matricola;
  }

  public String getCognomeNome() {
    return cognomeNome;
  }

  public String getMatricola() {
    return matricola;
  }

  public boolean isOk() {
    return ok;
  }

  public void setOk(boolean ok) {
    this.ok = ok;
  }

  public String getAbsencesSent() {
    return absencesSent;
  }

  public void setAbsencesSent(String absencesSent) {
    this.absencesSent = absencesSent;
  }

  public String getCompetencesSent() {
    return competencesSent;
  }

  public void setCompetencesSent(String compentecesSent) {
    this.competencesSent = compentecesSent;
  }

  public String getMealTicketSent() {
    return mealTicketSent;
  }

  public void setMealTicketSent(String mealTicketSent) {
    this.mealTicketSent = mealTicketSent;
  }

  public String getProblems() {
    return problems;
  }

  public void setProblems(String errors) {
    this.problems = errors;
  }

  public String getTrainingHoursSent() {
    return trainingHoursSent;
  }

  public void setTrainingHoursSent(String trainingHoursSent) {
    this.trainingHoursSent = trainingHoursSent;
  }
}
