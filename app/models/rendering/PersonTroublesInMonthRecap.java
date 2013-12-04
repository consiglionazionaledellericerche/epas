package models.rendering;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Person;
import models.PersonDayInTrouble;

/**
 * Classe che modella il riepilogo delle timbrature mancanti per la persona tramite una lista di PersonDayInTrouble.
 * @author alessandro
 *
 */
public class PersonTroublesInMonthRecap {
	
	public Person person;
	public String troublesAutoFixed = "";
	public String troublesNoAbsenceNoStampings = "";
	public String troublesUncoupledStampings = "";

	
	/**
	 * Costruttore
	 * @param person
	 * @param monthBegin
	 * @param monthEnd
	 */
	public PersonTroublesInMonthRecap(Person person, LocalDate monthBegin, LocalDate monthEnd) {
		this.person = person;
		List<PersonDayInTrouble> troubles = PersonDayInTrouble.find(
				"select trouble from PersonDayInTrouble trouble, PersonDay pd where trouble.personDay = pd and pd.person = ? and trouble.fixed = false and pd.date between ? and ? order by pd.date",
				person, monthBegin, monthEnd).fetch();
		
		for(PersonDayInTrouble trouble : troubles)
		{
			if(trouble.cause.equals("timbratura disaccoppiata persona fixed"))
			{
				this.troublesAutoFixed = this.troublesAutoFixed + trouble.personDay.date.getDayOfMonth() + ", ";
			}
			
			if(trouble.cause.equals("no assenze giornaliere e no timbrature"))
			{
				this.troublesNoAbsenceNoStampings = this.troublesNoAbsenceNoStampings + trouble.personDay.date.getDayOfMonth() + ", ";
			}
			
			if(trouble.cause.equals("timbratura disaccoppiata"))
			{
				this.troublesUncoupledStampings = this.troublesUncoupledStampings + trouble.personDay.date.getDayOfMonth() + ", ";
			}
				
				
			
		}
		if(this.troublesAutoFixed.endsWith(", "))
			this.troublesAutoFixed = this.troublesAutoFixed.substring(0, this.troublesAutoFixed.length()-2);
		
		if(this.troublesNoAbsenceNoStampings.endsWith(", "))
			this.troublesNoAbsenceNoStampings = this.troublesNoAbsenceNoStampings.substring(0, this.troublesNoAbsenceNoStampings.length()-2);
		
		if(this.troublesUncoupledStampings.endsWith(", "))
			this.troublesUncoupledStampings = this.troublesUncoupledStampings.substring(0, this.troublesUncoupledStampings.length()-2);
	} 
	
	

}
