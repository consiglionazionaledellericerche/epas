package manager;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.exports.ReperibilityPeriod;

/**
 * 
 * @author dario e Arianna
 *
 */
public class ReperibilityManager {

	/**
	 * 
	 * @param reperibilityDays la lista dei giorni di reperibilità effettuati
	 * @param prt il tipo di reperibilità
	 * @return la lista dei periodi di reperibilità effettuati
	 */
	public static List<ReperibilityPeriod> getPersonReperibilityPeriods(List<PersonReperibilityDay> reperibilityDays, PersonReperibilityType prt){
		
		List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();
		ReperibilityPeriod reperibilityPeriod = null;

		for (PersonReperibilityDay prd : reperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (reperibilityPeriod == null || !reperibilityPeriod.person.equals(prd.personReperibility.person) || !reperibilityPeriod.end.plusDays(1).equals(prd.date)) {
				reperibilityPeriod = new ReperibilityPeriod(prd.personReperibility.person, prd.date, prd.date, prt);
				reperibilityPeriods.add(reperibilityPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			} else {
				reperibilityPeriod.end = prd.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			}
		}
		return reperibilityPeriods;
	}
}
