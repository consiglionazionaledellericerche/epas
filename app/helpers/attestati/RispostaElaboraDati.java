package helpers.attestati;

import lombok.Data;

/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione dati delle
 * assenze/competenze/buoni mensa inviati al sistema degli attestati del CNR.
 *
 * @author cristian
 */
@Data
public final class RispostaElaboraDati {
  private final String cognomeNome;
  private final String matricola;
  private boolean ok = false;
  private String absencesSent = null;
  private String competencesSent = null;
  private String mealTicketSent = null;
  private String trainingHoursSent = null;
  private String problems = null;
}
