package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.JsonShiftPeriodsBinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import models.Absence;
import models.CompetenceCode;
import models.Person;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftCancelled;
import models.ShiftTimeTable;
import models.ShiftType;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftCancelledPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import play.Logger;
import play.Play;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.Controller;

/**
 * 
 * @author arianna, dario
 * Implements work shifts
 *
 */
public class Shift extends Controller{

	/*
	 * @author arianna
	 * Restituisce la lista delle persone in un determinato turno
	 * 
	 */
	public static void personList(){
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", Play.configuration.getProperty("address.sistorg"));

		
		String type = params.get("type");		
		ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco Turnisti di tipo %s", shiftType.type);
		
		List<Person> personList = new ArrayList<Person>();
		personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type AND (psst.beginDate IS NULL OR psst.beginDate <= now()) AND (psst.endDate IS NULL OR psst.endDate >= now())")
				.setParameter("type", type)
				.getResultList(); 
		
		Logger.debug("Shift personList called, found %s shift person", personList.size());
		
		for (Person p: personList) {
			Logger.debug("name=%s surname=%s id=%di jolly=%s", p.name, p.surname, p.id, p.personShift.jolly);
		}
		render(personList);
	}
	
	/*
	 * @author arianna, dario
	 * Get shifts from the DB and render to the sistorg portal calendar
	 */
	public static void find(){
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", Play.configuration.getProperty("address.sistorg"));
		
		// type validation
		String type = params.get("type");
		ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco turni di tipo %s", shiftType.type);
		
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));
		
		List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
		
		// get the normal shift
		List<PersonShiftDay> personShiftDay = PersonShiftDay.find("" +
				"SELECT psd FROM PersonShiftDay psd WHERE psd.date BETWEEN ? AND ? " +
				"AND psd.shiftType = ? " +
				"ORDER BY psd.shiftTimeTable.startShift, psd.date", 
				from, to, shiftType).fetch();
		Logger.debug("Shift find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, personShiftDay.size());
		

		ShiftPeriod shiftPeriod = null;
		
		for (PersonShiftDay psd : personShiftDay) {	
			if (shiftPeriod == null || !shiftPeriod.person.equals(psd.personShift.person) || !shiftPeriod.end.plusDays(1).equals(psd.date) || !shiftPeriod.shiftTimeTable.getStartShift().equals(psd.shiftTimeTable.getStartShift() )){
				shiftPeriod = new ShiftPeriod(psd.personShift.person, psd.date, psd.date, psd.shiftType, false, psd.shiftTimeTable);
				shiftPeriods.add(shiftPeriod);
				Logger.trace("\nCreato nuovo shiftPeriod, person=%s, start=%s, end=%s, type=%s, timetable=%s" , shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftTimeTable);
			} else {
				shiftPeriod.end = psd.date;
				Logger.trace("Aggiornato ShiftPeriod, person=%s, start=%s, end=%s, type=%s, timetable=%s", shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftTimeTable);
			}
		}
		
		// get the cancelled shifts of type shiftType
		List<ShiftCancelled> shiftCancelled = ShiftCancelled.find("SELECT sc FROM ShiftCancelled sc WHERE sc.date BETWEEN ? AND ? " +"" +
				"AND sc.type = ? " +
				"ORDER BY sc.date", 
				from, to, shiftType).fetch();
		Logger.debug("ShiftCancelled find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, shiftCancelled.size());
	
		shiftPeriod = null;
		
		for (ShiftCancelled sc : shiftCancelled) {
			if (shiftPeriod == null || !shiftPeriod.end.plusDays(1).equals(sc.date)){
				shiftPeriod = new ShiftPeriod(sc.date, sc.date, sc.type, true);
				shiftPeriods.add(shiftPeriod);
				Logger.trace("Creato nuovo shiftPeriod di cancellati, start=%s, end=%s, type=%s" , shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
			} else {
				shiftPeriod.end = sc.date;
				Logger.trace("Aggiornato ShiftPeriod di cancellati, start=%s, end=%s, type=%s\n", shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
			}
		}
		
		Logger.debug("Find %s shiftPeriods. ShiftPeriods = %s", shiftPeriods.size(), shiftPeriods);
		render(shiftPeriods);
		
	}
	
	
	/*
	 * @author arianna
	 * Update shifts read from the sistorg portal calendar
	 */
	public static void update(String type, Integer year, Integer month, @As(binder=JsonShiftPeriodsBinder.class) ShiftPeriods body){
		Logger.debug("update: Received shiftPeriods %s", body);
		
		if (body == null) {
			badRequest();	
		}
		
		// type validation
		ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		if (shiftType == null) {
			throw new IllegalArgumentException(String.format("ShiftType type = %s doesn't exist", type));			
		}
		
		Logger.debug("shiftType=%s", shiftType.description);
		
		//Il mese e l'anno ci servono per "azzerare" eventuale giorni di turno rimasti vuoti
		LocalDate monthToManage = new LocalDate(year, month, 1);
		
		//Conterrà i giorni del mese che devono essere attribuiti a qualche turnista 
		Set<Integer> daysOfMonthToAssign = new HashSet<Integer>();	
		Set<Integer> daysOfMonthForCancelled = new HashSet<Integer>();	
		for (int i = 1 ; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
			daysOfMonthToAssign.add(i);
			daysOfMonthForCancelled.add(i);
		}
		Logger.trace("Lista dei giorni del mese = %s", daysOfMonthToAssign);
		
		LocalDate day = null;
		for (ShiftPeriod shiftPeriod : body.periods) {
			
			// start and end date validation
			if (shiftPeriod.start.isAfter(shiftPeriod.end)) {
				throw new IllegalArgumentException(
					String.format("ShiftPeriod person.id = %s has start date %s after end date %s", shiftPeriod.person.id, shiftPeriod.start, shiftPeriod.end));
			}
			
			day = shiftPeriod.start;
			while (day.isBefore(shiftPeriod.end.plusDays(1))) {
				// normal shift
				if (!shiftPeriod.cancelled) {
					//La persona deve essere tra i turnisti 
					PersonShift personShift = PersonShift.find("SELECT ps FROM PersonShift ps WHERE ps.person = ?", shiftPeriod.person).first();
					if (personShift == null) {
						throw new IllegalArgumentException(
							String.format("Person %s is not a shift person", shiftPeriod.person));
					}
					
					//Se la persona è in ferie questo giorno non può essere in turno (almeno che non sia cancellato)
					//if ((Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, shiftPeriod.person).fetch().size() > 0) && (! "X".equals(type))){
					if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, shiftPeriod.person).fetch().size() > 0) {
						throw new IllegalArgumentException(
							String.format("ShiftPeriod person.id %d is not compatible with a Absence in the same day %s", shiftPeriod.person.id, day));
					}
				
					//Salvataggio del giorno di turno
					//Se c'è un turno già presente viene sostituito, altrimenti viene creato un PersonShiftDay nuovo
					Logger.debug("Cerco turno shiftType = %s AND date = %s AND shiftTimeTable.startShift = %s", shiftType.description, day, shiftPeriod.shiftTimeTable.getStartShift());
					String[] hmsStart = shiftPeriod.shiftTimeTable.getStartShift().split(":");
					
					PersonShiftDay personShiftDay = 
						PersonShiftDay.find("shiftType = ? AND date = ? AND shiftTimeTable.startShift = ?", shiftType, day, new LocalDateTime(1970, 01, 01, Integer.parseInt(hmsStart[0]), Integer.parseInt(hmsStart[1]))).first();
					
					if (personShiftDay == null) {
						personShiftDay = new PersonShiftDay();
						Logger.debug("Creo un nuovo personShiftDay per person = %s, day = %s, shiftType = %s", shiftPeriod.person.name, day, shiftType.description);
					} else {
						Logger.debug("Aggiorno il personShiftDay = %s di %s", personShiftDay, personShiftDay.personShift.person.name);
					}
					personShiftDay.date = day;
					personShiftDay.shiftType = shiftType;
					personShiftDay.shiftTimeTable = shiftPeriod.shiftTimeTable;
					personShiftDay.personShift = personShift;
					
					personShiftDay.save();
					Logger.info("Aggiornato PersonShiftDay = %s con %s\n", personShiftDay, personShiftDay.personShift.person);
					
					//Questo giorno è stato assegnato
					daysOfMonthToAssign.remove(day.getDayOfMonth());

				} else {
				// cancelled shift
					// Se non c'è già il turno cancellato lo creo
					Logger.debug("Cerco turno cancellato shiftType = %s AND date = %s", shiftType.type, day);
					ShiftCancelled shiftCancelled = 
							ShiftCancelled.find("type = ? AND date = ?", shiftType, day).first();
					Logger.debug("shiftCancelled = %s", shiftCancelled);
					
					if (shiftCancelled == null) {
						shiftCancelled = new ShiftCancelled();
						shiftCancelled.date = day;
						shiftCancelled.type = shiftType;
						
						Logger.debug("Creo un nuovo ShiftCancelled=%s per day = %s, shiftType = %s", shiftCancelled, day, shiftType.description);
						
						shiftCancelled.save();
						Logger.debug("Creato un nuovo ShiftCancelled per day = %s, shiftType = %s", day, shiftType.description);
					}
					
					//Questo giorno è stato annullato
					daysOfMonthForCancelled.remove(day.getDayOfMonth());
				}
			
				day = (LocalDate)day.plusDays(1);
			}
		}
		
		Logger.info("Turni da rimuovere = %s", daysOfMonthToAssign);
		
		for (int dayToRemove : daysOfMonthToAssign) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);
			
			int cancelled = JPA.em().createQuery("DELETE FROM PersonShiftDay WHERE shiftType = :shiftType AND date = :dateToRemove)")
					.setParameter("shiftType", shiftType)
					.setParameter("dateToRemove", dateToRemove)
					.executeUpdate();
			if (cancelled == 1) {
				Logger.info("Rimosso turno di tipo %s del giorno %s", shiftType.description, dateToRemove);
			}
		}
		
		Logger.info("Turni cancellati da rimuovere = %s", daysOfMonthForCancelled);
		
		for (int dayToRemove : daysOfMonthForCancelled) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);
			
			int cancelled = JPA.em().createQuery("DELETE FROM ShiftCancelled WHERE type = :shiftType AND date = :dateToRemove)")
					.setParameter("shiftType", shiftType)
					.setParameter("dateToRemove", dateToRemove)
					.executeUpdate();
			if (cancelled == 1) {
				Logger.info("Rimosso turno cancellato di tipo %s del giorno %s", shiftType.description, dateToRemove);
			}
		}
	}
	
	
	/**
	 * @author arianna
	 * crea il file PDF con il resoconto mensile dei turni di tipo 'A e B' per
	 * il mese 'month' dell'anno 'year'
	 * (portale sistorg)
	 * 
	 * T.B.N. che il tipo dei turni in questo caso è fisso. Sarà variabile quando si introdurranno
	 * i gruppi e i tipi di reperibilità associati ad ogni gruppo
	 */
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
//		Long groupType = params.get("type", Long.class);
		
		Logger.debug("sono nella exportMonthAsPDF con year=%s e month=%s", year, month);
		
		 ArrayList<String> shiftTypes = new ArrayList<String>();
		 shiftTypes.add("A"); 
		 shiftTypes.add("B");
		 Logger.debug("shiftTypes=%s", shiftTypes);
		
		
		// crea la tabella
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> shiftMonth = null;
		
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
		for (String type: shiftTypes)
		{	
			Logger.debug("controlla type=%s", type);
			ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();	
			if (shiftType == null) {
				notFound(String.format("ShiftType = %s doesn't exist", shiftType));			
			}
			
			// seleziona le persone nel turno 'shiftType' da inizio a fine mese
			List<PersonShiftDay> personShiftDays = 
				PersonShiftDay.find("SELECT prd FROM PersonShiftDay prd WHERE date BETWEEN ? AND ? AND prd.shiftType = ? ORDER by date", firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType).fetch();
		
			for (PersonShiftDay personShiftDay : personShiftDays) {
				Person person = personShiftDay.personShift.person;
					
				builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);
			}
		}
		shiftMonth = builder.build();
		
		//Table<Person, String, Integer> shiftSumDays = HashBasedTable.<Person, String, Integer>create();
		Table<String, String, Integer> shiftSumDays = TreeBasedTable.<String, String, Integer>create();
		
		
		// for each person
		for (Person person: shiftMonth.rowKeySet()) {
			String personName = person.surname + " " + person.name;
			
			Logger.debug("Conto turni di %s", personName);
			
			// for each day of month
			for (Integer dayOfMonth: shiftMonth.columnKeySet()) {
				
				// counts the shift days 
				if (shiftMonth.contains(person, dayOfMonth)) { 
					String col = String.format("%s", shiftMonth.get(person, dayOfMonth).toUpperCase());
						
					int n = shiftSumDays.contains(personName, col) ? shiftSumDays.get(personName, col) + 1 : 1;
					shiftSumDays.put(personName, col, Integer.valueOf(n));
					Logger.debug("inserita riga %s, %s %s", person, col, n);
				} 
			}
			
		}
	
		LocalDate today = new LocalDate();
		renderPDF(today, firstOfMonth, shiftSumDays);
	}

	/**
	 * @author arianna
	 * crea il file PDF con il calendario mensile dei turni di tipo 'A, B' per
	 * il mese 'month' dell'anno 'year'.
	 * (portale sistorg)
	 * 
	 * T.B.N. che il tipo dei tyrni in questo caso è fisso. Sarà variabile quando si introdurranno
	 * i gruppi e i tipi di reperibilità associati ad ogni gruppo
	 */
	public static void exportMonthCalAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
//		Long groupType = params.get("type", Long.class);
		Logger.debug("sono nella exportMonthCalAsPDF con year=%s e month=%s", year, month);
		
		ArrayList<String> shiftTypes = new ArrayList<String>();
		shiftTypes.add("A"); 
		shiftTypes.add("B");

		Logger.debug("shiftTypes=%s", shiftTypes);
		
		class PSD {
			String fasciaTurno;
			String tipoTurno;
			
			public PSD (String tipoTurno, String fasciaTurno) {
				this.tipoTurno = tipoTurno;
				this.fasciaTurno = fasciaTurno;
			}

		}
		
		class SD {
			Person mattina;
			Person pomeriggio;
			
			public SD (Person mattina, Person pomeriggio) {
				this.mattina = mattina;
				this.pomeriggio = pomeriggio;
			}
		}
		
		// crea la tabella dei turni (persona, giorno) -> (Tipo turno, fascia, turno)
		ImmutableTable.Builder<Person, Integer, PSD> builder = ImmutableTable.builder(); 
		Table<Person, Integer, PSD> shiftCalendarMonth = null;
		
		// crea la tabella dei turni mensile (tipo turno, giorno) -> (persona turno mattina, persona turno pomeriggio)
		Table<String, Integer, SD> shiftCalendar = HashBasedTable.<String, Integer, SD>create();
		
		// prende il primo giorno del mese
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
		
		for (String type: shiftTypes)
		{	
			Logger.debug("controlla type=%s", type);
			ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();	
			if (shiftType == null) {
				notFound(String.format("ShiftType = %s doesn't exist", shiftType));			
			}
			
			// legge i giorni di turno del tipo 'type' da inizio a fine mese 
			List<PersonShiftDay> personShiftDays = 
				PersonShiftDay.find("SELECT prd FROM PersonShiftDay prd WHERE date BETWEEN ? AND ? AND prd.shiftType = ? ORDER by date, prd.shiftTimeTable.startShift", firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType).fetch();
			Logger.debug("Trovati %d turni di tipo %s",personShiftDays.size(), type);
			
			for (PersonShiftDay personShiftDay : personShiftDays) {
				Person person = personShiftDay.personShift.person;
				PSD psd = new PSD(personShiftDay.shiftType.type, personShiftDay.shiftTimeTable.getStartShift());
					
				builder.put(person, personShiftDay.date.getDayOfMonth(), psd);
				Logger.debug("Inserito in shiftCalendarMonth %s %s turno %s di %s", person, personShiftDay.date.getDayOfMonth(), psd.tipoTurno, psd.fasciaTurno);
			}
			
			//legge i turni cancellati e li registra nella tabella mensile
			Logger.debug("Cerco i turni cancellati di tipo '%s' e li inserisco nella tabella mensile", type);
			List<ShiftCancelled> shiftsCancelled = 
					ShiftCancelled.find("SELECT sc FROM ShiftCancelled sc WHERE date BETWEEN ? AND ? AND sc.type = ? ORDER by date", firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType).fetch();
			SD shift = new SD (null, null);
			for (ShiftCancelled sc: shiftsCancelled) {
				shiftCalendar.put(type, sc.date.getDayOfMonth(), shift);
				Logger.debug("trovato turno cancellato di tipo %s del %s", type, sc.date);
			}
		}
		
		shiftCalendarMonth = builder.build();
		
	
		Logger.debug("Costruisce il calendario ...");
		for (int day: shiftCalendarMonth.columnKeySet()) {
			String currShift = "";
			SD shift = null;

			Logger.debug("giorno %s", day);
			for (Person person: shiftCalendarMonth.rowKeySet()) {
				Logger.debug("person %s", person);
				if (shiftCalendarMonth.contains(person, day)) {
					currShift = shiftCalendarMonth.get(person, day).tipoTurno;
					String f = shiftCalendarMonth.get(person, day).fasciaTurno;
					Logger.debug("trovato turno (%s,%s) per (%s, %s)", currShift, f, person, day);
					
					//if ((shift == null) || (! currShift.equals(prevShift))) {
					if (!shiftCalendar.contains(currShift, day)) {
						shift = (shiftCalendarMonth.get(person, day).fasciaTurno.contains("07:00")) ? new SD (person, null) : new SD (null, person);
						shiftCalendar.put(currShift, day, shift);
						Logger.debug("creato shift (%s, %s) con shift.mattina=%s e shift.pomeriggio=%s", currShift, day, shift.mattina, shift.pomeriggio);
					} else {
						shift = shiftCalendar.get(currShift, day);
						if (shiftCalendarMonth.get(person, day).fasciaTurno.contains("07:00")) {
							Logger.debug("Completo turno di %s con la mattina di %s", day, person);
							shift.mattina = person;
						} else {
							Logger.debug("Completo turno di %s con il pomeriggio di %s", day, person);
							shift.pomeriggio = person;
						}
						shiftCalendar.put(currShift, day, shift);
					}
				}
			}
		}
		
		LocalDate today = new LocalDate();
		renderPDF(today, firstOfMonth, shiftCalendar);
	}
	
	/*
	 * @author arianna
	 * Restituisce la lista delle assenze delle persone di un certo turno in un certo periodo di tempo
	 * 
	 */
	public static void absence() {
		
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "*");

		String type = params.get("type");
		
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));
		
		ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco Turnisti di tipo %s", shiftType.type);
		
		List<Person> personList = new ArrayList<Person>();
		personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type AND (psst.beginDate IS NULL OR psst.beginDate <= now()) AND (psst.endDate IS NULL OR psst.endDate >= now())")
		//personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type")
				.setParameter("type", type)
				.getResultList(); 
		
		Logger.debug("Shift personList called, found %s shift person", personList.size());
		
		// Lists of absence for a single shift person and for all persons
		List<Absence> absencePersonShiftDays = new ArrayList<Absence>();
				
		// List of absence periods
		List<AbsenceShiftPeriod> absenceShiftPeriods = new ArrayList<AbsenceShiftPeriod>();

		if (personList.size() == 0) {
			render(absenceShiftPeriods);
			return;
		}
				
		AbsenceShiftPeriod absenceShiftPeriod = null;
		
		absencePersonShiftDays = JPA.em().createQuery("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date BETWEEN :from AND :to AND pd.person IN (:personList) ORDER BY pd.person.id, pd.date")
			.setParameter("from", from)
			.setParameter("to", to)
			.setParameter("personList", personList)
			.getResultList();
		
		
		Logger.debug("Trovati %s giorni di assenza", absencePersonShiftDays.size());
		
		for (Absence abs : absencePersonShiftDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (absenceShiftPeriod == null || !absenceShiftPeriod.person.equals(abs.personDay.person) || !absenceShiftPeriod.end.plusDays(1).equals(abs.personDay.date)) {
				absenceShiftPeriod = new AbsenceShiftPeriod(abs.personDay.person, abs.personDay.date, abs.personDay.date, (ShiftType) ShiftType.findById(shiftType.id));
				absenceShiftPeriods.add(absenceShiftPeriod);
				Logger.trace("Creato nuovo absenceShiftPeriod, person=%s, start=%s, end=%s", absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
			} else {
				absenceShiftPeriod.end = abs.personDay.date;
				Logger.trace("Aggiornato absenceShiftPeriod, person=%s, start=%s, end=%s", absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
			}
		}
		Logger.debug("Find %s absenceShiftPeriod. AbsenceShiftPeriod = %s", absenceShiftPeriods.size(), absenceShiftPeriods.toString());
		render(absenceShiftPeriods);
	}
}
