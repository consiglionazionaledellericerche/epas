/**
 * 
 */
package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;


import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.joda.time.LocalDate;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import play.Logger;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.Controller;

import static play.modules.pdf.PDF.*;


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

	/**
	 * per provarlo: curl -H "Accept: application/json" http://localhost:9001/reperibility/1/find/2012/11/26/2013/01/06
	 */
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
	 * @author arianna
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
			
			List<PersonDay> personDayList = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date BETWEEN ? AND ? AND pd.person = ? ORDER BY pd.date", from, to, person).fetch();
			for(PersonDay pd : personDayList){
				if(pd.absences.size() > 0){
					for(Absence abs : pd.absences){
						absencePersonReperibilityDays.add(abs);
						Logger.debug("Type of absence: %s", abs.absenceType);
					}
				}
			}
			
			Logger.debug("Absence of the person %s find called from %s to %s, found %s reperibility days", person.id, from, to, absenceReperibilityDays.size());
			absenceReperibilityDays.addAll(absencePersonReperibilityDays);
		}
		
		AbsenceReperibilityPeriod absenceReperibilityPeriod = null;
		
		Logger.trace("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());
		for (Absence abs : absenceReperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (absenceReperibilityPeriod == null || !absenceReperibilityPeriod.person.equals(abs.personDay.person) || !absenceReperibilityPeriod.end.plusDays(1).equals(abs.personDay.date)) {
				absenceReperibilityPeriod = new AbsenceReperibilityPeriod(abs.personDay.person, abs.personDay.date, abs.personDay.date, (PersonReperibilityType) PersonReperibilityType.findById(type));
				absenceReperibilityPeriods.add(absenceReperibilityPeriod);
				Logger.trace("Creato nuovo absenceReperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			} else {
				absenceReperibilityPeriod.end = abs.personDay.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			}
		}
		Logger.debug("Find %s absenceReperibilityDays. AbsenceReperibilityPeriod = %s", absenceReperibilityPeriods.size(), absenceReperibilityPeriods.toString());
		render(absenceReperibilityPeriods);
	}
	
	/**
	 * @author cristian, arianna
	 * Aggiorna le informazioni relative alla Reperibilità del personale
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"id" : "49","start" : 2012-12-05,"end" : "2012-12-10", "reperibility_type_id" : "1"}, { "id" : "139","start" : "2012-12-12" , "end" : "2012-12-14", "reperibility_type_id" : "1" } , { "id" : "139","start" : "2012-12-17","end" : "2012-12-18", "reperibility_type_id" : "1" } ]' \ 
	 * 			http://localhost:9000/reperibility/1/update/2012/12
	 * 
	 * @param body
	 */
	public static void update(Long type, Integer year, Integer month, @As(binder=JsonReperibilityPeriodsBinder.class) ReperibilityPeriods body) {

		Logger.debug("update: Received reperebilityPeriods %s", body);
		
		if (body == null) {
			badRequest();	
		}
		
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		
		if (reperibilityType == null) {
			throw new IllegalArgumentException(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		//Il mese e l'anno ci servono per "azzerare" eventuale giorni di reperibilità rimasti vuoti
		LocalDate monthToManage = new LocalDate(year, month, 1);
		
		//Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile 
		Set<Integer> daysOfMonthToAssign = new HashSet<Integer>();
		
		for (int i = 1 ; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
			daysOfMonthToAssign.add(i);
		}
		Logger.trace("Lista dei giorni del mese = %s", daysOfMonthToAssign);
		
		LocalDate day = null;
		
		for (ReperibilityPeriod reperibilityPeriod : body.periods) {
			
			reperibilityPeriod.reperibilityType = reperibilityType;
			
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
				if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, reperibilityPeriod.person).fetch().size() > 0) {
					throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id %d is not compatible with a Vacaction in the same day %s", reperibilityPeriod.person.id, day));
				}

				//Se la persona è in ferie questo giorno non può essere reperibile 
				if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, reperibilityPeriod.person).fetch().size() > 0) {
					throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id %d is not compatible with a Absence in the same day %s", reperibilityPeriod.person.id, day));
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
				
				//Questo giorno è stato assegnato
				daysOfMonthToAssign.remove(day.getDayOfMonth());
				
				Logger.info("Inserito o aggiornato PersonReperibilityDay = %s", personReperibilityDay);
				
				day = day.plusDays(1);
			}
		}
		
		Logger.info("Giorni di reperibilità da rimuovere = %s", daysOfMonthToAssign);
		
		for (int dayToRemove : daysOfMonthToAssign) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);
			
			int cancelled = JPA.em().createQuery("DELETE FROM PersonReperibilityDay WHERE reperibilityType = :reperibilityType AND date = :dateToRemove)")
			.setParameter("reperibilityType", reperibilityType)
			.setParameter("dateToRemove", dateToRemove)
			.executeUpdate();
			if (cancelled == 1) {
				Logger.info("Rimossa reperibilità di tipo %s del giorno %s", reperibilityType, dateToRemove);
			}
		}

	}
	
	public static void exportYearAsPDF() {
		int year = params.get("year", Integer.class);
		Long reperibilityId = params.get("type", Long.class);
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}
		
		List<Table<Person, Integer, String>> reperibilityMonths = new ArrayList<Table<Person, Integer, String>>();
		
		for (int i = 1; i <=12; i++) {
			
			LocalDate firstOfMonth = new LocalDate(year, i, 1);
			
			List<PersonReperibilityDay> personReperibilityDays = 
					JPA.em().createQuery("SELECT prd FROM PersonReperibilityDay prd WHERE date BETWEEN :firstOfMonth AND :endOfMonth AND reperibilityType = :reperibilityType ORDER by date")
					.setParameter("firstOfMonth", firstOfMonth)
					.setParameter("endOfMonth", firstOfMonth.dayOfMonth().withMaximumValue())
					.setParameter("reperibilityType", reperibilityType)
					.getResultList();
			
			ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
			Table<Person, Integer, String> reperibilityMonth = null;
			
			for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
				Person person = personReperibilityDay.personReperibility.person;
				
				builder.put(person, personReperibilityDay.date.getDayOfMonth(), DateUtility.isHoliday(person, personReperibilityDay.date) ? "fs" : "fr");
			}
			reperibilityMonth = builder.build();
			reperibilityMonths.add(reperibilityMonth);
		}
		renderPDF(year, reperibilityMonths);
	}
		
}
