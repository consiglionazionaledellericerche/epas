package manager;

import models.Stamping;

import org.joda.time.Minutes;

/**
 * Classe che modella due stampings logicamente accoppiate nel personday
 *  (una di ingresso ed una di uscita)
 */
public class PairStamping{

	private static int SEQUENCE_ID = 1;
	
	int pairId;	//for hover template
	
	public Stamping in;
	public Stamping out;

	int timeInPair = 0;
	
	boolean prPair = false;

	public PairStamping(Stamping in, Stamping out) {
		this.in = in;
		this.out = out;
		timeInPair = Minutes.minutesBetween(in.date, out.date).getMinutes();;
		
		this.pairId = SEQUENCE_ID++;
		in.pairId = this.pairId;
		out.pairId = this.pairId;
		
		// TODO: decidere se entrambe o almeno una.
		if ( (in.stampType != null && in.stampType.identifier.equals("pr")) 
				|| (out.stampType != null && out.stampType.identifier.equals("pr")) ) {
			prPair = true;
		}
	}
			
}