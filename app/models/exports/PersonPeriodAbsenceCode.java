package models.exports;

import java.util.Date;

import org.joda.time.LocalDate;

/**
 * 
 * @author dario
 * @author alessandro
 * oggetto per l'esportazione di un periodo di assenza per una certa persona relativa a un certo codice
 */
public class PersonPeriodAbsenceCode {

	public String name;
	public String surname;
	public String code;
	public LocalDate dateFrom;
	public LocalDate dateTo;
	
	
}
