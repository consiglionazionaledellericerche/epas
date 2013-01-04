/**
 * 
 */
package models.exports;

import org.joda.time.LocalDateTime;

import models.BadgeReader;
import models.StampType;

/**
 * @author cristian
 *
 */
public class StampingFromClient {

	public enum TipoMatricolaFirma {
	    matricolaCNR, // la matricola sul badge e' uguale alla matricola CNR
	    idTabellaINT, // la matricola sul badge e' l'id della tabella Persone preceduto da INT (es. INT123)
	    matricolaBadge, // la matricola sul badge e' quella del campo matricola badge
	    idTabella // la matricola sul badge e' l'id della tabella Person
	}
	public Integer inOut;
	public BadgeReader badgeReader;
	public StampType stampType;
	public Long matricolaFirma;
	public TipoMatricolaFirma tipoMatricolaFirma;
	
	public LocalDateTime dateTime;
	
}
