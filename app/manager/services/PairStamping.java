package manager.services;

import it.cnr.iit.epas.DateUtility;

import models.Stamping;
import models.enumerate.StampTypes;

/**
 * Classe che modella due stampings logicamente accoppiate nel PersonDay. (una di ingresso ed una di
 * uscita)
 */
public class PairStamping {

  /**
   * Id univoco alla sequenza..
   */
  private static int SEQUENCE_ID = 1;
  
  public Stamping first;
  public Stamping second;
  
  public int timeInPair = 0;

  /**
   * Coppia di timbrature per pranzo. CNR centrale.
   */
  public boolean prPair = false;
  
  /**
   * Costruisce la coppia di timbrature.
   * @modify in.pairId e out.pairId se appartengono ad una coppia valida. 
   * @param first ingresso
   * @param second uscita
   */
  public PairStamping(Stamping first, Stamping second) {

    this.first = first;
    this.second = second;

    timeInPair = 0;
    timeInPair = timeInPair - DateUtility.toMinute(first.date);
    timeInPair = timeInPair + DateUtility.toMinute(second.date);
    
    //La coppia valida la imposto nel caso di coppia definitiva (non contenente l'uscita fittizia
    // e se si tratta di una coppia in-out, il caso out-in è usato nel calcolo del buono pasto.
    if (!second.exitingNow && first.isIn() && second.isOut()) {
      int pairId = SEQUENCE_ID++;
      first.pairId = pairId;
      second.pairId = pairId;
    }

    // TODO: decidere se entrambe o almeno una.
    if ((first.stampType != null && first.stampType.equals(StampTypes.PAUSA_PRANZO))
        || (second.stampType != null && second.stampType.equals(StampTypes.PAUSA_PRANZO))) {
      prPair = true;
    }
  }
  
  public String toString() {
    return String.format("[%s,%s]", first.date.toString("HH:mm:ss"),
        second.date.toString("HH:mm:ss"));
  }

}
