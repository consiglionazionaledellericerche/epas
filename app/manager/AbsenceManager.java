package manager;

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
	
	
	
}
