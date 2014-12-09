package manager;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

/**
 * 
 * @author alessandro
 *
 */
public class AbsenceManager {

	/**
	 * Il primo codice utilizzabile per l'anno selezionato come assenza nel seguente ordine 31,32,94
	 * @param person
	 * @param actualDate
	 * @return
	 */
	public static AbsenceType whichVacationCode(Person person, LocalDate date){

		Contract contract = person.getCurrentContract();

		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}

		if(vr.vacationDaysLastYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "31").first();

		if(vr.persmissionNotYetUsed > 0)
			return AbsenceType.find("byCode", "94").first();
		
		if(vr.vacationDaysCurrentYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "32").first();

		return null;
	}
	
	
	
	
	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 32.
	 * @param person
	 * @param date
	 * @return l'absenceType 32 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	public static AbsenceType takeAnother32(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.vacationDaysCurrentYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "32").first();
		
		return null;
		
	}
	
	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
	 * @param person
	 * @param date
	 * @return l'absenceType 31 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	public static AbsenceType takeAnother31(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.vacationDaysLastYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "31").first();

		return null;
	}
	
	/**
	 * Verifica che la persona alla data possa prendere un giorno di permesso codice 94.
	 * @param person
	 * @param date
	 * @return l'absenceType 94 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	public static AbsenceType takeAnother94(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.persmissionNotYetUsed > 0)
			return AbsenceType.find("byCode", "94").first();

		return null;
	}
	
	/**
	 * Verifica la possibilità che la persona possa usufruire di un riposo compensativo nella data specificata.
	 * Se voglio inserire un riposo compensativo per il mese successivo a oggi considero il residuo a ieri.
	 * N.B Non posso inserire un riposo compensativo oltre il mese successivo a oggi.
	 * @param person
	 * @param date
	 * @return 
	 */
	public static boolean canTakeCompensatoryRest(Person person, LocalDate date)
	{
		
		//Data da considerare 
		
		// (1) Se voglio inserire un riposo compensativo per il mese successivo considero il residuo a ieri.
		//N.B Non posso inserire un riposo compensativo oltre il mese successivo.
		LocalDate dateToCheck = date;
		//Caso generale
		if( dateToCheck.getMonthOfYear() == LocalDate.now().getMonthOfYear() + 1) 
		{
			dateToCheck = LocalDate.now();
		}
		//Caso particolare dicembre - gennaio
		else if( dateToCheck.getYear() == LocalDate.now().getYear() + 1 
				&& dateToCheck.getMonthOfYear() == 1 && LocalDate.now().getMonthOfYear() == 12) 
		{
			
			dateToCheck = LocalDate.now();
		}
		
		// (2) Calcolo il residuo alla data precedente di quella che voglio considerare.
		if(dateToCheck.getDayOfMonth()>1)
			dateToCheck = dateToCheck.minusDays(1);

		Contract contract = person.getContract(dateToCheck);
		
		PersonResidualYearRecap c = 
				PersonResidualYearRecap.factory(contract, dateToCheck.getYear(), dateToCheck);
		
		if(c == null)
			return false;
			
		PersonResidualMonthRecap mese = c.getMese(dateToCheck.getMonthOfYear());
		
		if(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato 
				> mese.person.getWorkingTimeType(dateToCheck)
				.getWorkingTimeTypeDayFromDayOfWeek(dateToCheck.getDayOfWeek()).workingTime) {
			return true;
		} 
	
		return false;
		
	}
	
	
}
