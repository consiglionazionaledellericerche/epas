package models.exports;

import java.util.Date;

import org.joda.time.LocalDate;

/**
 * 
 * @author dario
 * @author alessandro
 * @author arianna (aggiunto id)
 * oggetto per l'esportazione di un periodo di assenza per una certa persona relativa a un certo codice
 */
public class PersonPeriodAbsenceCode {

	public long personId;
	public String name;
	public String surname;
	public String code;
	public String start;
	public String end;
}
