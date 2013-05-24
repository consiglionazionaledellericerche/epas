/**
 * 
 */
package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import models.Absence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;

import org.h2.command.ddl.CreateAggregate;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadablePeriod;

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

	public enum SemRep {FS1S, FR1S, FS2S, FR2S}; 
	
	public static void personList() {
		Long type = Long.parseLong(params.get("type"));
		
		List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		Logger.debug("Reperibility personList called, found %s reperible person", personList.size());
		render(personList);
	}

	/**
	 * @author cristian, arianna
	 * Fornisce i periodi di reperibilità del personale reperibile di tipo 'type'
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 * per provarlo: curl -H "Accept: application/json" http://localhost:9001/reperibility/1/find/2012/11/26/2013/01/06
	 */
	public static void find() {
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = 
				PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND prd.reperibilityType = ? ORDER BY prd.date", from, to, reperibilityType).fetch();

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
	 * Fornisce la lista del personale reperibile di tipo 'type' 
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 */
	public static void who() {
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		List<Person> personList = new ArrayList<Person>();
		
		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = 
				PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND prd.reperibilityType = ? ORDER BY prd.date", from, to, reperibilityType).fetch();

		Logger.debug("Reperibility who called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());

		for (PersonReperibilityDay prd : reperibilityDays) {
			if (!personList.contains(prd.personReperibility.person)) {
				Logger.trace("inserisco il reperibile ", prd.personReperibility.person);
				personList.add(prd.personReperibility.person);
				Logger.trace("trovata person=%s", prd.personReperibility.person);
			}
		}
		Logger.debug("trovati %s reperibili: %s", personList.size(), personList);
		
		render(personList);
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
		
		// List of absence periods
		List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = new ArrayList<AbsenceReperibilityPeriod>();
		
		
		if (personList.size() == 0) {
			render(absenceReperibilityPeriods);
			return;
		}
				
		AbsenceReperibilityPeriod absenceReperibilityPeriod = null;
		
		absencePersonReperibilityDays = JPA.em().createQuery("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date BETWEEN :from AND :to AND pd.person IN (:personList) ORDER BY pd.person.id, pd.date")
			.setParameter("from", from)
			.setParameter("to", to)
			.setParameter("personList", personList)
			.getResultList();
		
		
		Logger.debug("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());
		
		for (Absence abs : absencePersonReperibilityDays) {
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
	
	
	/**
	 * @author arianna, cristian
	 * crea il file PDF con il calendario annuale delle reperibilità di tipi 'type' per l'anno 'year'
	 * (portale sistorg)
	 */
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
		
		int i = 0;
		//ImmutableTable.Builder<Person, String, Integer> builder1 = ImmutableTable.builder(); 
		//Table<Person, SemRep, Integer> reperibilitySumDays = ImmutableTable.<Person, Reperibility.SemRep, Integer>create();
		Table<Person, String, Integer> reperibilitySumDays = HashBasedTable.<Person, String, Integer>create();
		for (Table<Person, Integer, String> reperibilityMonth: reperibilityMonths) {
			i++;
			for (Person person: reperibilityMonth.rowKeySet()) {
				for (Integer dayOfMonth: reperibilityMonth.columnKeySet()) {
					if (reperibilityMonth.contains(person, dayOfMonth)) { 
						//SemRep semRep = SemRep.valueOf( String.format("%s%dS", reperibilityMonth.get(person, dayOfMonth).toUpperCase(), (i<=6 ?1:2)));
						String col = String.format("%s%dS", reperibilityMonth.get(person, dayOfMonth).toUpperCase(), (i<=6 ?1:2));
						
						//int n = reperibilitySumDays.contains(person, semRep) ? reperibilitySumDays.get(person, semRep) + 1 : 1;
						//reperibilitySumDays.put(person, semRep, Integer.valueOf(n));
						int n = reperibilitySumDays.contains(person, col) ? reperibilitySumDays.get(person, col) + 1 : 1;
						reperibilitySumDays.put(person, col, Integer.valueOf(n));
						
					} else {
						
					}
				}
			}
		}
		
		LocalDate firstOfYear = new LocalDate(year, 1, 1);
		renderPDF(year, firstOfYear, reperibilityMonths, reperibilitySumDays);
	}

	
	/**
	 * @author arianna
	 * crea il file PDF con il resoconto mensile delle reperibilità di tipo 'type' per
	 * il mese 'month' dell'anno 'year'
	 * (portale sistorg)
	 */
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		Long reperibilityId = params.get("type", Long.class);
		
		class PRP {
			int inizio;
			int fine;
			String mese;
			String tipo;
			
			public PRP (int inizio, int fine, String mese, String tipo) {
				this.inizio = inizio;
				this.fine = fine;
				this.mese = mese;
				this.tipo = tipo;
			}
			
			public String toString () {
				List<Integer> art = new ArrayList<Integer>();
				art.add(1);
				art.add(11);
				art.add(8);
				//String articolo = (art.contains(inizio)) ? "l'" : "il";
				//return (inizio == fine) ? String.format("%s%d", articolo, inizio) : String.format("dal %d al %d", inizio, fine);
				return (inizio == fine) ? String.format("%d / %s", inizio, mese) : String.format("%d-%d / %s", inizio, fine, mese);
			}
		}
		class PRD {
			int giorno;
			String tipo;
			
			public PRD (int giorno, String tipo) {
				this.giorno = giorno;
				this.tipo  = tipo;
			}
		}
		
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);	
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}
			
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
		String shortMonth = firstOfMonth.monthOfYear().getAsShortText();
		
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
		
		Table<Person, String, Integer> reperibilitySumDays = HashBasedTable.<Person, String, Integer>create();
		Table<Person, String, List<PRP>> reperibilityDateDays = HashBasedTable.<Person, String, List<PRP>>create();
		
		
		// for each person
		for (Person person: reperibilityMonth.rowKeySet()) {
			
			// lista dei periodi di reperibilità ferieali e festivi
			List<PRP> fsPeriods = new ArrayList<PRP>();
			List<PRP> frPeriods = new ArrayList<PRP>();
		
			PRD previousPersonReperibilityDay = null;
			PRP currentPersonReperibilityPeriod = null;
			
			// for each day of month
			for (Integer dayOfMonth: reperibilityMonth.columnKeySet()) {
				
				// counts the reperibility days 
				if (reperibilityMonth.contains(person, dayOfMonth)) { 
					String col = String.format("%s", reperibilityMonth.get(person, dayOfMonth).toUpperCase());
						
					int n = reperibilitySumDays.contains(person, col) ? reperibilitySumDays.get(person, col) + 1 : 1;
					reperibilitySumDays.put(person, col, Integer.valueOf(n));
				} 
				
				// create the reperibility periods divided by fs and fr
				if (reperibilityMonth.contains(person, dayOfMonth)) {
					if ((previousPersonReperibilityDay == null) || 
						(!reperibilityMonth.get(person, dayOfMonth).equals(previousPersonReperibilityDay.tipo)) ||
						((dayOfMonth - 1) != previousPersonReperibilityDay.giorno)) { 		
							currentPersonReperibilityPeriod = new PRP (dayOfMonth, dayOfMonth, shortMonth, reperibilityMonth.get(person, dayOfMonth));
					
							if (currentPersonReperibilityPeriod.tipo == "fs") {
								fsPeriods.add(currentPersonReperibilityPeriod);
							} else {
								frPeriods.add(currentPersonReperibilityPeriod);
							}
					}
					else {
						currentPersonReperibilityPeriod.fine = dayOfMonth;
					}
					previousPersonReperibilityDay = new PRD (dayOfMonth, reperibilityMonth.get(person, dayOfMonth));
				}
			}
			
			reperibilityDateDays.put(person, "FS", fsPeriods);
			reperibilityDateDays.put(person, "FR", frPeriods);
		}
		
		// read the reperebility type codes
		CompetenceCode codeFR = CompetenceCode.findById(2L);
		CompetenceCode codeFS = CompetenceCode.findById(3L);
		String codFr = codeFR.code;
		String codFs = codeFS.code;
		
		LocalDate today = new LocalDate();
		
		renderPDF(today, firstOfMonth, reperibilitySumDays, reperibilityDateDays, codFs, codFr);
	}

	
	/*
	 * Export the reperibility calendar in iCal for the person with id = personId with reperibility 
	 * of type 'type' for the 'year' year
	 */
	private static Calendar createCalendar(Long type, Long personId, int year) {
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);
		
		// check for the parameter
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);	
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		// read the reperibility person list 
		PersonReperibility personReperibility = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.personReperibilityType.id = ? AND pr.person.id = ?", type, personId).first();
		if (personReperibility == null) {
			notFound(String.format("Person id = %d is not associated to a reperibility of type = %s", personId, reperibilityType));
		}
		
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        TimeZone timezone = registry.getTimeZone("Europe/Rome");
//        timezone.getID();

        //Date date = new Date();
        
        // Create a calendar
        Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
        icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.getProperties().add(CalScale.GREGORIAN);
        icsCalendar.getProperties().add(Version.VERSION_2_0);
        
        // read the person reperibility days for the year
		LocalDate from = new LocalDate(Integer.parseInt(params.get("year")), 1, 1);
		LocalDate to = new LocalDate(Integer.parseInt(params.get("year")), 12, 31);
		
		List<PersonReperibilityDay> reperibilityDays = 
				PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND reperibilityType = ? AND personReperibility = ? ORDER BY prd.date", from, to, reperibilityType, personReperibility).fetch();

		Logger.debug("Reperibility find called from %s to %s, found %s reperibility days for person id = %s", from, to, reperibilityDays.size(), personId);

//		VEvent reperibilityPeriod = null;

		
		java.util.Calendar cal = new GregorianCalendar();
		cal.setTimeZone(timezone);
		
		Date startDate = null;
		Date endDate = null;
		int sequence = 1;
		
		for (PersonReperibilityDay prd : reperibilityDays) {
						

			cal.set(java.util.Calendar.MONTH, prd.date.getMonthOfYear() - 1);
			cal.set(java.util.Calendar.DAY_OF_MONTH, prd.date.getDayOfMonth());
			cal.set(java.util.Calendar.YEAR, prd.date.getYear());
			cal.set(java.util.Calendar.HOUR, 0);
			cal.set(java.util.Calendar.MINUTE, 0);
			cal.set(java.util.Calendar.SECOND, 0);
			cal.set(java.util.Calendar.MILLISECOND, 0);
			
			Date date = new Date(cal.getTimeInMillis());
			
			Logger.trace("Data reperibilita': date=%s", date);
			
			if ( startDate == null) {
				Logger.trace("Nessun periodo, nuovo periodo: startDate=%s", date);
				
				startDate = endDate = date;
				sequence = 1;
				continue;
			} 
			
			if ( date.getTime() - endDate.getTime() > 86400*1000 ) {
				Logger.trace("Termine periodo: startDate=%s, sequence=%s", startDate, sequence);
				icsCalendar.getComponents().add(createICalEvent(startDate, sequence));
				startDate = endDate = date;
				sequence = 1;
				Logger.trace("Nuovo periodo: startDate=%s", date);
			} else {
				sequence++;
				endDate = date;
				Logger.trace("Allungamento periodo: startDate=%s, endDate=%s, sequence.new=%s", startDate, endDate, sequence);
			}
			
		}
		
		Logger.trace("Termine periodo e calendario: startDate=%s, sequence=%s", startDate, sequence);
		icsCalendar.getComponents().add(createICalEvent(startDate, sequence));

		Logger.debug("Find %s periodi di reperibilità.", icsCalendar.getComponents().size());
        
        return icsCalendar;
	}
	
	private static VEvent createICalEvent(Date startDate, int sequence) {
		VEvent reperibilityPeriod = new VEvent(startDate, new Dur(sequence, 0, 0, 0), "Reperibilità Registro");
		reperibilityPeriod.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		return reperibilityPeriod;
	}

	
	public static void iCal() {
		Long type = params.get("type", Long.class);
		Long personId = params.get("personId", Long.class);
		int year = params.get("year", Integer.class);
		
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		try {
			Calendar calendar = createCalendar(type, personId, year);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        CalendarOutputter outputter = new CalendarOutputter();
	        outputter.output(calendar, bos);
	        response.setHeader("Content-Type", "application/ics");
	        InputStream is = new ByteArrayInputStream(bos.toByteArray());
	        renderBinary(is,"reperibilitaRegistro.ics");
	        bos.close();
	        is.close();
		} catch (IOException e) {
			Logger.error("Io exception building ical", e);
		} catch (ValidationException e) {
			Logger.error("Validation exception generating ical", e);
		}
	}

}
