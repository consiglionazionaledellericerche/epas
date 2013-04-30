package controllers;

import org.joda.time.LocalDate;


import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;
import it.cnr.iit.epas.JsonRequestedOvertimeBinder;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Competence;
import models.Person;
import models.PersonMonth;

import models.exports.OvertimesData;
import models.exports.PersonsCompetences;
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
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
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
	 * Set the overtimes requested by the responsible
	 */
	public static void setRequestOvertime(Integer year, Integer month, @As(binder=JsonRequestedOvertimeBinder.class) PersonsCompetences body) {
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
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

}
