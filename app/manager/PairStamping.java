package manager;

import it.cnr.iit.epas.DateUtility;

import models.Stamping;
import models.enumerate.StampTypes;

/**
 * Classe che modella due stampings logicamente accoppiate nel PersonDay. (una di ingresso ed una di
 * uscita)
 */
public class PairStamping {

  private static int SEQUENCE_ID = 1;
  public Stamping in;
  public Stamping out;
  int pairId;    //for hover template
  int timeInPair = 0;

  boolean prPair = false;

  public PairStamping(Stamping in, Stamping out) {
    this.in = in;
    this.out = out;

    // Vecchio calcolo reintrodotto.
    timeInPair = 0;
    timeInPair = timeInPair - DateUtility.toMinute(in.date);
    timeInPair = timeInPair + DateUtility.toMinute(out.date);

    // TODO: Se si vuole usare le JodaTime assicuriamoci che facciano
    // per bene il loro lavoro!! In questa implementazione considera anche
    // i secondi e quindi spesso arrotonda  l'elapsed al minuto inferiore.
    //timeInPair = Minutes.minutesBetween(in.date, out.date).getMinutes();
    //

    this.pairId = SEQUENCE_ID++;
    in.pairId = this.pairId;
    out.pairId = this.pairId;

    // TODO: decidere se entrambe o almeno una.
    if ((in.stampType != null && in.stampType.equals(StampTypes.PAUSA_PRANZO))
        || (out.stampType != null && out.stampType.equals(StampTypes.PAUSA_PRANZO))) {
      prPair = true;
    }
  }

}
