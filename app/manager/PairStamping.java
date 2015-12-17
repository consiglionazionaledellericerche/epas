package manager;

import it.cnr.iit.epas.DateUtility;

import models.Stamping;

/**
 * Classe che modella due stampings logicamente accoppiate nel PersonDay. (una di ingresso ed una di
 * uscita)
 */
public class PairStamping {

  /**
   * Id univoco alla sequenza..
   */
  private static int SEQUENCE_ID = 1;
  
  public Stamping in;
  public Stamping out;
  
  int timeInPair = 0;

  /**
   * Coppia di timbrature per pranzo. CNR centrale.
   */
  boolean prPair = false;
  
  /**
   * Costruisce la coppia di timbrature.
   * @modify in.pairId e out.pairId se appartengono ad una coppia valida. 
   * @param in ingresso
   * @param out uscita
   */
  public PairStamping(Stamping in, Stamping out) {

    this.in = in;
    this.out = out;

    timeInPair = 0;
    timeInPair = timeInPair - DateUtility.toMinute(in.date);
    timeInPair = timeInPair + DateUtility.toMinute(out.date);

    if (!out.exitingNow) {
      int pairId = SEQUENCE_ID++;
      in.pairId = pairId;
      out.pairId = pairId;
    }

    // TODO: decidere se entrambe o almeno una.
    if ((in.stampType != null && in.stampType.identifier.equals("pr"))
            || (out.stampType != null && out.stampType.identifier.equals("pr"))) {
      prPair = true;
    }
  }

}
