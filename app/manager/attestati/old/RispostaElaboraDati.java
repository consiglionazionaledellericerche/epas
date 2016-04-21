package manager.attestati.old;

import lombok.Data;

import models.Person;

/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione dati delle
 * assenze/competenze/buoni mensa inviati al sistema degli attestati del CNR.
 *
 * @author cristian
 */
@Data
public final class RispostaElaboraDati {
  private Dipendente dipendente;
  private final String cognomeNome;
  private final String matricola;
  private boolean ok = false;
  private String absencesSent = null;
  private String competencesSent = null;
  private String mealTicketSent = null;
  private String trainingHoursSent = null;
  private String problems = null;
}
