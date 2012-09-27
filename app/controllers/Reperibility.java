/**
 * 
 */
package controllers;

import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonVacation;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.apache.commons.io.IOUtils;
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
		Long type = Long.parseLong(params.get("type"));
		
		List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		Logger.debug("Reperibility personList called, found %s reperible person", personList.size());
		render(personList);
	}

	public static void find() {
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		Long type = Long.parseLong(params.get("type"));
		
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
				reperibilityPeriod = new ReperibilityPeriod(prd.personReperibility.person, prd.date, prd.date, (PersonReperibilityType) PersonReperibilityType.findById(type));
				reperibilityPeriods.add(reperibilityPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			} else {
				reperibilityPeriod.end = prd.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			}
		}
		Logger.debug("Find %s reperibilityPeriods. ReperibilityPeriods = %s", reperibilityPeriods.size(), reperibilityPeriods);
		render(reperibilityPeriods);
	}
	
	/**
	 * Legge le assenze dei reperibili di una determinata tipologia in un dato intervallo di tempo
	 * (portale sistorg)
	 */
	public static void absence() {
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		Long type = Long.parseLong(params.get("type"));
		
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		// read the reperibility person list 
		List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		Logger.debug("Reperibility personList called, found %s reperible person of type %s", personList.size(), type);
		
		// Lists of absence for a single reperibility person and for all persons
		List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();
		List<Absence> absenceReperibilityDays = new ArrayList<Absence>();
		
		// List of absence periods
		List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = new ArrayList<AbsenceReperibilityPeriod>();
		
		// for each person in the list read the absence days in the DB
		for (Person person : personList) {
			absencePersonReperibilityDays = Absence.find("SELECT abs FROM Absence abs WHERE abs.date BETWEEN ? AND ? AND abs.person = ? ORDER BY abs.date", from, to, person).fetch();

			Logger.debug("Absence of the person %s find called from %s to %s, found %s reperibility days", person.id, from, to, absenceReperibilityDays.size());
			absenceReperibilityDays.addAll(absencePersonReperibilityDays);
		}
		
		AbsenceReperibilityPeriod absenceReperibilityPeriod = null;

		for (Absence abs : absenceReperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (absenceReperibilityPeriod == null || !absenceReperibilityPeriod.person.equals(abs.person) || !absenceReperibilityPeriod.end.plusDays(1).equals(abs.date)) {
				absenceReperibilityPeriod = new AbsenceReperibilityPeriod(abs.person, abs.date, abs.date, (PersonReperibilityType) PersonReperibilityType.findById(type));
				absenceReperibilityPeriods.add(absenceReperibilityPeriod);
				Logger.trace("Creato nuovo absenceReperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			} else {
				absenceReperibilityPeriod.end = abs.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			}
		}
		Logger.debug("Find %s reperibilityPeriods. ReperibilityPeriods = %s", absenceReperibilityPeriods.size(), absenceReperibilityPeriods);
		render(absenceReperibilityPeriods);
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
		Logger.debug("chiamata update con REQUEST_METHOD = %s", request.method);
		
		if (request.method.equals("OPTIONS")) {
			response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
			response.setHeader("Access-Control-Allow-Methods", "PUT, POST, GET, OPTIONS");
			response.setHeader("Access-Control-Max-Age", "1000");
			response.setHeader("Access-Control-Allow-Headers", "X-PINGARUNER");
			response.setHeader("Content-type", "application/json");
		}else if (request.method.equals("PUT")) {
			Logger.debug("nella update sono nel ramo put");
		}
		Logger.debug("update: Received reperebilityPeriods %s", body);
		
		if (body == null) {
			badRequest();	
		}
		
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
