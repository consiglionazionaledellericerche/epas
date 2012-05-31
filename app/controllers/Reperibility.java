/**
 * 
 */
package controllers;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Person;
import models.PersonReperibilityDay;
import models.exports.ReperibilityPeriod;

import play.Logger;
import play.db.jpa.JPA;
import play.mvc.Controller;

/**
 * @author cristian
 *
 */
public class Reperibility extends Controller {

	public static void personList() {
		List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())").fetch();
		Logger.debug("Reperibilit personList called, found %s reperible person", personList.size());
		render(personList);
	}

	public static void find() {
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = 
				PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? ORDER BY prd.date", from, to).fetch();

		Logger.debug("Reperibility find called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());

		List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();
		ReperibilityPeriod reperibilityPeriod = null;

		for (PersonReperibilityDay prd : reperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (reperibilityPeriod == null || !reperibilityPeriod.person.equals(prd.personReperibility.person) || !reperibilityPeriod.end.plusDays(1).equals(prd.date)) {
				reperibilityPeriod = new ReperibilityPeriod(prd.personReperibility.person, prd.date, prd.date);
				reperibilityPeriods.add(reperibilityPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			} else {
				reperibilityPeriod.end = prd.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			}
		}
		render(reperibilityPeriods);
	}
}
