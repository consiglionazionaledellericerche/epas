package manager;

import it.cnr.iit.epas.DateUtility;
import models.Stamping;

/**
 * Classe che modella due stampings logicamente accoppiate nel personday
 *  (una di ingresso ed una di uscita)
 */
public class PairStamping
{

	private static int SEQUENCE_ID = 1;
	
	int pairId;	//for hover template
	
	public Stamping in;
	public Stamping out;

	int timeInPair = 0;

	public PairStamping(Stamping in, Stamping out)
	{
		this.in = in;
		this.out = out;
		timeInPair = 0;
		timeInPair = timeInPair - DateUtility.toMinute(in.date);
		timeInPair = timeInPair + DateUtility.toMinute(out.date);
		
		this.pairId = SEQUENCE_ID++;
		in.pairId = this.pairId;
		out.pairId = this.pairId;
	}
			
}