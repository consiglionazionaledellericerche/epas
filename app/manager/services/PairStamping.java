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
  
  public Stamping in;
  public Stamping out;
  
  public int timeInPair = 0;

  /**
   * Coppia di timbrature per pranzo. CNR centrale.
   */
  public boolean prPair = false;
  
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
    
    //La coppia valida la imposto nel caso di coppia definitiva (non contenente l'uscita fittizia
    // e se si tratta di una coppia in-out, il caso out-in è usato nel calcolo del buono pasto.
    if (!out.exitingNow && in.isIn() && out.isOut()) {
      int pairId = SEQUENCE_ID++;
      in.pairId = pairId;
      out.pairId = pairId;
    }

    // TODO: decidere se entrambe o almeno una.
    if ((in.stampType != null && in.stampType.equals(StampTypes.PAUSA_PRANZO))
        || (out.stampType != null && out.stampType.equals(StampTypes.PAUSA_PRANZO))) {
      prPair = true;
    }
  }

}
