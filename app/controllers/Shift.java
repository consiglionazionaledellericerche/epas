package controllers;

import static play.modules.pdf.PDF.renderPDF;
import helpers.BadRequest;
import it.cnr.iit.epas.JsonShiftPeriodsBinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Absence;
import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftType;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.jsoup.HttpStatusException;

import play.Logger;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.Controller;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.ning.http.client.Response;

import models.Absence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonDay.PairStamping;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftCancelled;
import models.ShiftTimeTable;
import models.ShiftType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.ShiftSlot;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftCancelledPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import helpers.BadRequest;

import play.Logger;
import play.Play;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.Controller;
import sun.tools.java.Package;

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

		String type = params.get("type");		
		Logger.debug("Cerco persone del turno %s", type);
		
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
	 * @author arianna
	 * Get shifts from the DB and render to the sistorg portal calendar
	 */
	public static void timeTable(){
		response.accessControl("*");
		
		Logger.debug("Cercata la time table di un turno");
		
		// type validation
		String type = params.get("type");
		ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco la time table del turno di tipo %s", shiftType.type);
		
		ShiftTimeTable shiftTimeTable = shiftType.shiftTimeTable;
		
		render(shiftTimeTable);
		
	}
	
	/*
	 * @author arianna, dario
	 * Get shifts from the DB and render to the sistorg portal calendar
	 */
	public static void find(){
		response.accessControl("*");
		
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
	
		// get the normal shift  ????????????????????????? 
		List<PersonShiftDay> personShiftDay = PersonShiftDay.find("" +
				"SELECT psd FROM PersonShiftDay psd WHERE psd.date BETWEEN ? AND ? " +
				"AND psd.shiftType = ? " +
				"ORDER BY psd.shiftSlot, psd.date",
			//	"ORDER BY psd.shiftTimeTable.startShift, psd.date", 
				from, to, shiftType).fetch();
		Logger.debug("Shift find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, personShiftDay.size());
		

		ShiftPeriod shiftPeriod = null;
		
		for (PersonShiftDay psd : personShiftDay) {	
			
			LocalTime startShift = (psd.shiftSlot.equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.startMorning : psd.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (psd.getShiftSlot().equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.endMorning : psd.shiftType.shiftTimeTable.endAfternoon;

			if (shiftPeriod == null || !shiftPeriod.person.equals(psd.personShift.person) || !shiftPeriod.end.plusDays(1).equals(psd.date) || !shiftPeriod.startSlot.equals(startShift)){
				shiftPeriod = new ShiftPeriod(psd.personShift.person, psd.date, psd.date, psd.shiftType, false, psd.shiftSlot, startShift, endShift);
				shiftPeriods.add(shiftPeriod);
				Logger.debug("\nCreato nuovo shiftPeriod, person=%s, start=%s, end=%s, type=%s, fascia=%s, orario=%s - %s" , shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
			} else {
				shiftPeriod.end = psd.date;
				Logger.debug("Aggiornato ShiftPeriod, person=%s, start=%s, end=%s, type=%s, fascia=%s, orario=%s - %s", shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
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
		
		Logger.debug("Find %s shiftPeriods.", shiftPeriods.size());
		render(shiftPeriods);
		
	}
	
	
	/*
	 * @author arianna
	 * Update shifts read from the sistorg portal calendar
	 */
	public static void update(String type, Integer year, Integer month, @As(binder=JsonShiftPeriodsBinder.class) ShiftPeriods body) {
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
					
					//Se la persona è assente in questo giorno non può essere in turno (almeno che non sia cancellato)
					if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, shiftPeriod.person).fetch().size() > 0) {
						String msg = String.format("Assenza incompatibile di %s %s per il giorno %s", shiftPeriod.person.name, shiftPeriod.person.surname, day);
						
						BadRequest.badRequest(msg);
						//throw new HttpStatusException(msg, 400, "");	
						//throw new IllegalArgumentException(msg);	
					}
				
					//Salvataggio del giorno di turno
					//Se c'è un turno già presente viene sostituito, altrimenti viene creato un PersonShiftDay nuovo
					Logger.debug("Cerco turno shiftType = %s AND date = %s AND shiftSlot = %s", shiftType.description, day, shiftPeriod.shiftSlot);
					
					//String[] hmsStart = shiftPeriod.startShift.getStartShift().split(":");
					
					PersonShiftDay personShiftDay = 
						PersonShiftDay.find("shiftType = ? AND date = ? AND shiftSlot = ?", shiftType, day, shiftPeriod.shiftSlot).first();
					if (personShiftDay == null) {
						personShiftDay = new PersonShiftDay();
						Logger.debug("Creo un nuovo personShiftDay per person = %s, day = %s, shiftType = %s", shiftPeriod.person.name, day, shiftType.description);
					} else {
						Logger.debug("Aggiorno il personShiftDay = %s di %s", personShiftDay, personShiftDay.personShift.person.name);
					}
					personShiftDay.date = day;
					personShiftDay.shiftType = shiftType;
					personShiftDay.setShiftSlot(shiftPeriod.shiftSlot);
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
			
				day = day.plusDays(1);
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
	/**
	 * 
	 */
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
//		Long groupType = params.get("type", Long.class);
		
		//Logger.debug("sono nella exportMonthAsPDF con year=%s e month=%s", year, month);
		
		 ArrayList<String> shiftTypes = new ArrayList<String>();
		 shiftTypes.add("A"); 
		 shiftTypes.add("B");
		 Logger.debug("shiftTypes=%s", shiftTypes);
		
		
		// crea la tabella per registrare i turni delle persone nei giorni del mese (nome cognome, giorno mese, A/B)
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> shiftMonth = null;
		
		//  Used TreeBasedTable becouse of the alphabetical name order (persona, A/B, num. giorni)
		Table<String, String, Integer> shiftSumDays = TreeBasedTable.<String, String, Integer>create();
				
		// crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
		Table<String, String, List<Integer>> inconsistentAbsence = TreeBasedTable.<String, String, List<Integer>>create();
		Table<String, String, List<String>> inconsistentAbsence2 = TreeBasedTable.<String, String, List<String>>create();

		String thNoStampings = "Mancata timbratura";
		String thBadStampings = "Timbratura errata";
		String thAbsences = "Assenza";
		
		
		
		// lista dei giorni di assenza, mancata timbratura e timbratura inconsistente
		List<Integer> noStampingDays = new ArrayList<Integer>();
		List<String> noStampingDays2 = new ArrayList<String>();
		List<Integer> badStampingDays = new ArrayList<Integer>();
		List<String> badStampingDays2 = new ArrayList<String>();
		List<Integer> absenceDays = new ArrayList<Integer>();
		List<String> absenceDays2 = new ArrayList<String>();
				
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
				PersonShiftDay.find("SELECT psd FROM PersonShiftDay psd WHERE date BETWEEN ? AND ? AND psd.shiftType = ? ORDER by date", firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType).fetch();
		
			for (PersonShiftDay personShiftDay : personShiftDays) {
				Person person = personShiftDay.personShift.person;
				String personName = person.name.concat(" ").concat(person.surname);
					
				// legge l'rario di inizio e fine turno da rispettare (mattina o pomeriggio)
				LocalTime startShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorning : personShiftDay.shiftType.shiftTimeTable.startAfternoon;
				LocalTime endShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorning : personShiftDay.shiftType.shiftTimeTable.endAfternoon;
				
				// legge l'orario di inizio e fine pausa pranzo
				LocalTime startLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.startAfternoonLunchTime;
				LocalTime endLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.endAfternoonLunchTime;
				
				// registro il turno della persona per quel giorno
				//------------------------------------------------------
				builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);				
				//Logger.debug("Registro il turno %s di %s per il giorno %d", personShiftDay.shiftType.type, person, personShiftDay.date.getDayOfMonth());
		
				//check for the absence inconsistencies
				//------------------------------------------
				PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personShiftDay.date, person).first();
				Logger.debug("Prelevo il personDay %s per la persona %s - personDay=%s", personShiftDay.date, person, personDay);
				
				// if there are no events and it is not an holiday -> error
				if (personDay == null) {	
					
					if ( !person.isHoliday(personShiftDay.date) && personShiftDay.date.isBefore(LocalDate.now())) {
						Logger.info("Il turno di %s è incompatibile con la sua mancata timbratura nel giorno %s (personDay == null)", personName, personShiftDay.date);
						
						/*noStampingDays = (inconsistentAbsence.contains(personName, thNoStampings)) ? inconsistentAbsence.get(personName, thNoStampings) : new ArrayList<Integer>();
						noStampingDays.add(personShiftDay.date.getDayOfMonth());
						inconsistentAbsence.put(personName, thNoStampings, noStampingDays);*/
						
						noStampingDays2 = (inconsistentAbsence2.contains(personName, thNoStampings)) ? inconsistentAbsence2.get(personName, thNoStampings) : new ArrayList<String>();
						noStampingDays2.add(personShiftDay.date.toString("dd MMM"));
						inconsistentAbsence2.put(personName, thNoStampings, noStampingDays2);
					}
				} else {
	
					// check for the stampings in working days
					if (!person.isHoliday(personShiftDay.date)) {
						
						// check no stampings
						//-----------------------------
						if (personDay.stampings.isEmpty()) {
							Logger.info("Il turno di %s è incompatibile con la sue mancate timbrature nel giorno %s", personName, personDay.date);
							
							/*noStampingDays = (inconsistentAbsence.contains(personName, thNoStampings)) ? inconsistentAbsence.get(personName, thNoStampings) : new ArrayList<Integer>();
							noStampingDays.add(personShiftDay.date.getDayOfMonth());
							inconsistentAbsence.put(personName, thNoStampings, noStampingDays);*/
							
							noStampingDays2 = (inconsistentAbsence2.contains(personName, thNoStampings)) ? inconsistentAbsence2.get(personName, thNoStampings) : new ArrayList<String>();
							noStampingDays2.add(personShiftDay.date.toString("dd MMM"));
							inconsistentAbsence2.put(personName, thNoStampings, noStampingDays2);
						} else {
							// check consistent stampings
							//-----------------------------
							
							// legge le coppie di timbrature valide 
							List<PairStamping> pairStampings = PersonDay.PairStamping.getValidPairStamping(personDay.stampings);
							
							// se c'è una timbratura guardo se è entro il turno
							if ((personDay.stampings.size() == 1) &&
								((personDay.stampings.get(0).isIn() && personDay.stampings.get(0).date.toLocalTime().isAfter(startShift)) || 
								(personDay.stampings.get(0).isOut() && personDay.stampings.get(0).date.toLocalTime().isBefore(startShift)) )) {
								
								String stamp = (personDay.stampings.get(0).isIn()) ? personDay.stampings.get(0).date.toLocalTime().toString("HH:mm").concat("- **:**") : "- **:**".concat(personDay.stampings.get(0).date.toLocalTime().toString("HH:mm"));
									
								badStampingDays2 = (inconsistentAbsence2.contains(personName, thBadStampings)) ? inconsistentAbsence2.get(personName, thBadStampings) : new ArrayList<String>();
								badStampingDays2.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
								inconsistentAbsence2.put(personName, thBadStampings, badStampingDays2);
									
							// se è vuota -> manca qualche timbratura		
							} else if (pairStampings.isEmpty()) {							
									
								Logger.info("Il turno di %s è incompatibile con la sue  timbrature disallineate nel giorno %s", personName, personDay.date);
								
								/*badStampingDays = (inconsistentAbsence.contains(personName, thBadStampings)) ? inconsistentAbsence.get(personName, thBadStampings) : new ArrayList<Integer>();
								badStampingDays.add(personShiftDay.date.getDayOfMonth());
								inconsistentAbsence.put(personName, thBadStampings, badStampingDays);*/
								
								badStampingDays2 = (inconsistentAbsence2.contains(personName, thBadStampings)) ? inconsistentAbsence2.get(personName, thBadStampings) : new ArrayList<String>();
								badStampingDays2.add(personShiftDay.date.toString("dd MMM"));
								inconsistentAbsence2.put(personName, thBadStampings, badStampingDays2);
							
							// controlla che le coppie di timbrature coprano
							// gli intervalli di prima e dopo pranzo
							} else {
								
								boolean okBeforeLunch = false;  // intervallo prima di pranzo coperto
								boolean okAfterLunch = false;	// intervallo dopo pranzo coperto
											
								// per ogni coppia di timbrature
								for (PairStamping pairStamping : pairStampings) {
									
									// controlla se interseca l'intervallo prima e dopo pranzo del turno
									// controlla se interseca la coppia di timbrature
									 if (pairStamping.out.date.toLocalTime().isAfter(startShift) && pairStamping.in.date.toLocalTime().isBefore(startLunchTime)) {
										 okBeforeLunch = true;
									 }
									 if (pairStamping.out.date.toLocalTime().isAfter(endLunchTime) && pairStamping.in.date.toLocalTime().isBefore(endShift)) {
										 okAfterLunch = true;
									 }
								}
								
								// se non ha coperto i due intervalli da errore
								if (!okBeforeLunch || !okAfterLunch) {
									
									Logger.info("Il turno di %s nel giorno %s non è stato completato o c'è stata una uscita fuori pausa pranzo - entrata alle %s, uscita alle %s - " +
											"pausa pranzo da %s a %s", personName, personDay.date, pairStampings.get(0).in.date.toString("HH:mm"), pairStampings.get(1).out.date.toString("HH:mm"), pairStampings.get(0).out.date.toString("HH:mm"), pairStampings.get(1).in.date.toString("HH:mm"));
								
									/*badStampingDays = (inconsistentAbsence.contains(personName, thBadStampings)) ? inconsistentAbsence.get(personName, thBadStampings) : new ArrayList<Integer>();
									badStampingDays.add(personShiftDay.date.getDayOfMonth());
									inconsistentAbsence.put(personName, thBadStampings, badStampingDays);*/
									
									badStampingDays2 = (inconsistentAbsence2.contains(personName, thBadStampings)) ? inconsistentAbsence2.get(personName, thBadStampings) : new ArrayList<String>();
									badStampingDays2.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(pairStampings.get(0).in.date.toString("HH:mm").concat("-").concat(pairStampings.get(0).out.date.toString("HH:mm")).concat("  ")).concat(pairStampings.get(1).in.date.toString("HH:mm").concat("-").concat(pairStampings.get(1).out.date.toString("HH:mm"))));
									inconsistentAbsence2.put(personName, thBadStampings, badStampingDays2);
								}
							} // fine controllo coppie timbrature
						} // fine if esistenza timbrature
					} // fine se non è giorno festivo
					
					// check for absences
					if (!personDay.absences.isEmpty()) {
						for (Absence absence : personDay.absences) {
							if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
								Logger.info("Il turno di %s è incompatibile con la sua assenza nel giorno %s", personName, personShiftDay.date);
								
								/*absenceDays = (inconsistentAbsence.contains(personName, thAbsences)) ? inconsistentAbsence.get(personName, thAbsences) : new ArrayList<Integer>();							
								absenceDays.add(personShiftDay.date.getDayOfMonth());							
								inconsistentAbsence.put(personName, thAbsences, absenceDays);*/
								
								absenceDays2 = (inconsistentAbsence2.contains(personName, thAbsences)) ? inconsistentAbsence2.get(personName, thAbsences) : new ArrayList<String>();							
								absenceDays2.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsence2.put(personName, thAbsences, absenceDays2);
							}
						}
					}	
				} // fine personDay != null
				
			}
		}
	
		shiftMonth = builder.build();
		

		// for each person
		for (Person person: shiftMonth.rowKeySet()) {
			String personName = person.surname + " " + person.name;
			
			Logger.debug("Conto turni di %s", personName);
			
			// for each day of month
			for (Integer dayOfMonth: shiftMonth.columnKeySet()) {
				
				// counts the shift days 
				if (shiftMonth.contains(person, dayOfMonth)) { 
					String shiftType = String.format("%s", shiftMonth.get(person, dayOfMonth).toUpperCase());
						
					int n = shiftSumDays.contains(personName, shiftType) ? shiftSumDays.get(personName, shiftType) + 1 : 1;
					shiftSumDays.put(personName, shiftType, Integer.valueOf(n));
				} 
			}
		}
	
		ArrayList<String> ths = new ArrayList<String>(Arrays.asList(thAbsences, thNoStampings, thBadStampings));
		
		LocalDate today = new LocalDate();
		renderPDF(today, firstOfMonth, shiftSumDays, inconsistentAbsence2, ths);
	}

	/**
	 * @author arianna
	 * crea il file PDF con il calendario mensile dei turni di tipo 'A, B' per
	 * il mese 'month' dell'anno 'year'.
	 * (portale sistorg)
	 * 
	 * T.B.N. che il tipo dei turni in questo caso è fisso. Sarà variabile quando si introdurranno
	 * i gruppi e i tipi di reperibilità associati ad ogni gruppo
	 */
	public static void exportMonthCalAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);

		Logger.debug("sono nella exportMonthCalAsPDF con year=%s e month=%s", year, month);
		
		ArrayList<String> shiftTypes = new ArrayList<String>();
		shiftTypes.add("A"); 
		shiftTypes.add("B");

		Logger.debug("shiftTypes=%s", shiftTypes);
		
		class PSD {
			String tipoTurno;
			ShiftSlot fasciaTurno;
		
			public PSD (String tipoTurno, ShiftSlot fasciaTurno) {
				this.tipoTurno = tipoTurno;
				this.fasciaTurno = fasciaTurno;
			}

		}
		
		// Persons involved in a shift 
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
				//Logger.debug("Trovati %d turni di tipo %s",personShiftDays.size(), type);
			
			for (PersonShiftDay personShiftDay : personShiftDays) {
				Person person = personShiftDay.personShift.person;
				PSD psd = new PSD(personShiftDay.shiftType.type, personShiftDay.getShiftSlot());
					
				builder.put(person, personShiftDay.date.getDayOfMonth(), psd);
				//Logger.debug("Inserito in shiftCalendarMonth %s %s turno %s di %s", person, personShiftDay.date.getDayOfMonth(), psd.tipoTurno, psd.fasciaTurno);
			}
			
			//legge i turni cancellati e li registra nella tabella mensile
			Logger.debug("Cerco i turni cancellati di tipo '%s' e li inserisco nella tabella mensile", type);
			List<ShiftCancelled> shiftsCancelled = 
					ShiftCancelled.find("SELECT sc FROM ShiftCancelled sc WHERE date BETWEEN ? AND ? AND sc.type = ? ORDER by date", firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType).fetch();
			SD shift = new SD (null, null);
			for (ShiftCancelled sc: shiftsCancelled) {
				shiftCalendar.put(type, sc.date.getDayOfMonth(), shift);
				//Logger.debug("trovato turno cancellato di tipo %s del %s", type, sc.date);
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
					ShiftSlot f = shiftCalendarMonth.get(person, day).fasciaTurno;
					//Logger.debug("trovato turno (%s,%s) per (%s, %s)", currShift, f, person, day);
					
					
					if (!shiftCalendar.contains(currShift, day)) {
						shift = (shiftCalendarMonth.get(person, day).fasciaTurno.equals(ShiftSlot.MORNING)) ? new SD (person, null) : new SD (null, person);
						shiftCalendar.put(currShift, day, shift);
						//Logger.debug("creato shift (%s, %s) con shift.mattina=%s e shift.pomeriggio=%s", currShift, day, shift.mattina, shift.pomeriggio);
					} else {
						shift = shiftCalendar.get(currShift, day);
						if (shiftCalendarMonth.get(person, day).fasciaTurno.equals(ShiftSlot.MORNING)) {
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
