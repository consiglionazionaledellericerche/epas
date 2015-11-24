package dao.wrapper;

import models.Office;

import org.joda.time.LocalDate;

/**
 * Office potenziato.
 * 
 * @author alessandro	
 */
public interface IWrapperOffice extends IWrapperModel<Office> {

	/**
	 * @return la data di installazione della sede. 
	 */
	LocalDate initDate();
	
}
