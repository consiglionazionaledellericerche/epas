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

	public Integer inOut;
	public BadgeReader badgeReader;
	public StampType stampType;
	public Long personId;
	
	public LocalDateTime dateTime;
	
}
