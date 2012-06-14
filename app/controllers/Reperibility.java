/**
 * 
 */
package controllers;

import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonVacation;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.bouncycastle.asn1.x509.sigi.PersonalData;
import org.joda.time.LocalDate;

import com.ning.http.client.Response;

import play.Logger;
import play.data.binding.As;
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

	public static void find(String type) {
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
				reperibilityPeriod = new ReperibilityPeriod(prd.personReperibility.person, prd.date, prd.date, (PersonReperibilityType) PersonReperibilityType.find("type = %s", type).first());
				reperibilityPeriods.add(reperibilityPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			} else {
				reperibilityPeriod.end = prd.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			}
		}
		render(reperibilityPeriods);
	}
	
	/**
	 * Aggiorna le informazioni relative alla Reperibilità del personale
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"id" : "49","start" : 2012-05-05,"end" : "2012-05-10", "reperibility_type_id" : "1"}, { "id" : "139","start" : "2012-05-12" , "end" : "2012-05-14", "reperibility_type_id" : "1" } , { "id" : "139","start" : "2012-05-17","end" : "2012-05-18", "reperibility_type_id" : "1" } ]' \ 
	 * 			http://localhost:9000/reperibility/update
	 * 
	 * @param body
	 */
	public static void update(@As(binder=JsonReperibilityPeriodsBinder.class) ReperibilityPeriods body) {
		Logger.debug("update: Received reperebilityPeriods %s", body);
		
		LocalDate day = null;
		for (ReperibilityPeriod reperibilityPeriod : body.periods) {
			
			if (reperibilityPeriod.start.isAfter(reperibilityPeriod.end)) {
				throw new IllegalArgumentException(
					String.format("ReperibilityPeriod person.id = %s has start date %s after end date %s", reperibilityPeriod.person.id, reperibilityPeriod.start, reperibilityPeriod.end));
			}
			
			day = reperibilityPeriod.start;
			while (day.isBefore(reperibilityPeriod.end.plusDays(1))) {
				
				//La persona deve essere tra i reperibili 
				if (reperibilityPeriod.person.reperibility == null) {
					throw new IllegalArgumentException(
						String.format("Person %s is not a reperible person", reperibilityPeriod.person));
				}
				
				//Se la persona è in ferie questo giorno non può essere reperibile 
				if (PersonVacation.find("date = ? and person = ?", day, reperibilityPeriod.person).fetch().size() > 0) {
					throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id is not compatible with a Vacaction in the same day %s", reperibilityPeriod.person, day));
				}

				//Se la persona è in ferie questo giorno non può essere reperibile 
				if (Absence.find("date = ? and person = ?", day, reperibilityPeriod.person).fetch().size() > 0) {
					throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id is not compatible with a Absence in the same day %s", reperibilityPeriod.person, day));
				}

				//Salvataggio del giorno di reperibilità
				//Se c'è un giorno di reperibilità già presente viene sostituito, altrimenti viene creato un PersonReperibilityDay nuovo
				PersonReperibilityDay personReperibilityDay = 
					PersonReperibilityDay.find("reperibilityType = ? AND date = ?", reperibilityPeriod.reperibilityType, day).first();
				
				if (personReperibilityDay == null) {
					personReperibilityDay = new PersonReperibilityDay();
					Logger.debug("Creo un nuovo personReperibilityDay per person = %s, day = %s, reperibilityDay = %s", reperibilityPeriod.person, day, reperibilityPeriod.reperibilityType );
				} else {
					Logger.debug("Aggiorno il personReperibilityDay = %s", personReperibilityDay);
				}
				
				personReperibilityDay.personReperibility = reperibilityPeriod.person.reperibility;
				personReperibilityDay.date = day;
				personReperibilityDay.reperibilityType = reperibilityPeriod.reperibilityType;
				//XXX: manca ancora l'impostazione dell'eventuale holidayDay, ovvero se si tratta di un giorno festivo
				
				personReperibilityDay.save();
				
				Logger.info("Inserito o aggiornato PersonReperibilityDay = %s", personReperibilityDay);
				
				day = day.plusDays(1);
			}
		}
		

	}
	
}
