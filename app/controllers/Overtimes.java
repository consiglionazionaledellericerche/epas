package controllers;

import static play.modules.pdf.PDF.renderPDF;

import org.joda.time.LocalDate;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;



import play.Logger;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.Controller;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;
import it.cnr.iit.epas.JsonRequestedOvertimeBinder;
import it.cnr.iit.epas.JsonRequestedPersonsBinder;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonHourForOvertime;
import models.PersonMonth;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;

import models.exports.OvertimesData;
import models.exports.PersonsCompetences;
import models.exports.PersonsList;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import models.PersonTags;


/*
 * @autor arianna
 * 
 * Implements methods used by sist-org in order
 * to keep overtime information
 */

public class Overtimes extends Controller {
	
	/*
	 * keeps person overtimes: 
	 * (residuo del mese, totale residuo anno precedente, tempo disponibile x straordinario)
	 * 
	 */

	public static void getPersonOvertimes() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		String email = params.get("email");
		int year = Integer.parseInt(params.get("year"));
		int month = Integer.parseInt(params.get("month"));
		
		Logger.debug("chiamata la getPersonOvertimes() con email=%s, year=%d, month=%d", email, year, month);
		
		// get the person with the given email
		Person person = Person.find("SELECT p FROM Person p WHERE p.contactData.email = ?", email).first();
		if (person == null) {
			notFound(String.format("Person with email = %s doesn't exist", email));			
		}
		Logger.debug("Find persons %s with email %s", person.name, email);
		

		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", person, month, year).first();
		if(personMonth == null)
			personMonth = new PersonMonth(person, year, month);
		
		/*OvertimesData personOvertimesData = new OvertimesData(PersonTags.toHourTime(new Integer(personMonth.totaleResiduoAnnoCorrenteAFineMese())), PersonTags.toHourTime(new Integer(personMonth.residuoDelMese())), PersonTags.toHourTime(new Integer(personMonth.tempoDisponibilePerStraordinari())));*/
		OvertimesData personOvertimesData = new OvertimesData(personMonth.totaleResiduoAnnoCorrenteAFineMese(), personMonth.residuoDelMese(), personMonth.tempoDisponibilePerStraordinari());
		Logger.debug("Trovato totaleResiduoAnnoCorrenteAFineMese=%s, residuoDelMese()=%s, tempoDisponibilePerStraordinari()=%s", personMonth.totaleResiduoAnnoCorrenteAFineMese(), personMonth.residuoDelMese(), personMonth.tempoDisponibilePerStraordinari());
		
		render(personOvertimesData);
	}
	
	/*
	 * Get the amount of overtimes the supervisor has for personel distribution
	 */
	public static void getSupervisorTotalOvertimes() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		String email = params.get("email");
		
		Logger.debug("chiamata la getSupervisorTotalOvertimes() con email=%s", email);
		
		// get the person with the given email
		Person person = Person.find("SELECT p FROM Person p WHERE p.contactData.email = ?", email).first();
		if (person == null) {
			notFound(String.format("Person with email = %s doesn't exist", email));			
		}
		Logger.debug("Find persons %s with email %s", person.name, email);
		

		PersonHourForOvertime personHourForOvertime = PersonHourForOvertime.find("Select ph from PersonHourForOvertime ph where ph.person = ?", person).first();
		if(personHourForOvertime == null)
			personHourForOvertime = new PersonHourForOvertime(person, 0);
		
		Logger.debug("Trovato personHourForOvertime con person=%s, numberOfHourForOvertime=%s", personHourForOvertime.person, personHourForOvertime.numberOfHourForOvertime);
		
		render(personHourForOvertime);
	}
	
	/*
	 * Set the overtimes requested by the responsible
	 */
	public static void setRequestOvertime(Integer year, Integer month, @As(binder=JsonRequestedOvertimeBinder.class) PersonsCompetences body) {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		Logger.debug("update: Received PersonsCompetences %s", body);	
		if (body == null) {
			badRequest();	
		}
		
		for (Competence competence : body.competences) {
			Competence oldCompetence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
					competence.person, year, month, competence.competenceCode).first();
			if (oldCompetence != null) {
				// update the requested hours
				oldCompetence.setValueApproved(competence.getValueApproved(), competence.getReason());
				oldCompetence.save();
				
				Logger.debug("Aggiornata competenza %s", oldCompetence);
			} else {
				// insert a new competence with the requested hours an reason
				competence.setYear(year);
				competence.setMonth(month);
				competence.save();
				
				Logger.debug("Creata competenza %s", competence);
			}
		}
	}
	
	/*
	 * Set personnel overtimes requested by the supervisor
	 */
	public static void setSupervisorTotalOvertimes(Integer hours, String email) throws Exception {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		try {
			Person person = Person.find("SELECT p FROM Person p WHERE p.contactData.email = ?", email).first();
			if (person == null) {
				throw new IllegalArgumentException(String.format("Person with email = %s doesn't exist", email));			
			}
			Logger.debug("Find persons %s with email %s", person.name, email);
			
			PersonHourForOvertime personHourForOvertime = PersonHourForOvertime.find("Select ph from PersonHourForOvertime ph where ph.person = ?", person).first();
			if (personHourForOvertime == null) {
				personHourForOvertime = new PersonHourForOvertime(person, hours);
				Logger.debug("Created  PersonHourForOvertime with persons %s and  hours=%s", person.name, hours);
			} else {
				personHourForOvertime.setNumberOfHourForOvertime(hours);
				Logger.debug("Updated  PersonHourForOvertime of persons %s with hours=%s", person.name, hours);
			}
			personHourForOvertime.save();
		} catch (Exception e) {
			Logger.error(e, "Problem during findjing person with email.");
			throw e;
		}
	}
	
	/**
	 * @author arianna
	 * crea il file PDF con il resoconto mensile delle ore di straordinario
	 * di una lista di persone identificate con l'email
	 * (portale sistorg)
	 * 
	 * curl -H "Content-Type: application/json" -X POST -d '[ {"email" : "stefano.ruberti@iit.cnr.it"}, { "email" : "andrea.vivaldi@iit.cnr.it"} , { "email" : "lorenzo.luconi@iit.cnr.it" } ]' http://scorpio.nic.it:9001/overtimes/exportMonthAsPDF/2013/05
	 */
	public static void exportMonthAsPDF(Integer year, Integer month, @As(binder=JsonRequestedPersonsBinder.class) PersonsList body) {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		Logger.debug("update: Received PersonsCompetences %s", body);	
		if (body == null) {
			badRequest();	
		}
			
		Table<Person, String, Integer> overtimesMonth = HashBasedTable.<Person, String, Integer>create();
		
		CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
		Logger.debug("find  CompetenceCode %s con CompetenceCode.code=%s", competenceCode, competenceCode.code);	
		
		for (Person person : body.persons) {
			Competence competence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
					person, year, month, competenceCode).first();
			Logger.debug("find  Competence %s per person=%s, year=%s, month=%s, competenceCode=%s", competence, person, year, month, competenceCode);	

			if (competence != null) {
				
				overtimesMonth.put(person, competence.reason != null ? competence.reason : "", competence.valueApproved);
				Logger.debug("Inserita riga person=%s reason=%s and  valueApproved=%s", person, competence.reason, competence.valueApproved);
			} else {
				
				overtimesMonth.put(person, "", 0);
				Logger.debug("Inserita riga person=%s reason='' and  valueApproved=0", person);
			}
		}
		
		LocalDate today = new LocalDate();
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
		
		renderPDF(today, firstOfMonth, overtimesMonth);
	}

}
