package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.reader.FirstLevelCache;
import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonMonthRecap;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.mvc.Controller;
import play.mvc.With;
import play.Logger;

@With( {Secure.class, NavigationMenu.class} )
public class PersonMonths extends Controller{

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void hourRecap(Long personId,int year){
		
		if(year > new LocalDate().getYear()){
			flash.error("Richiesto riepilogo orario di un anno futuro. Impossibile soddisfare la richiesta");
			renderTemplate("Application/indexAdmin.html");
		}
		Person person = null;
		if(personId != null)
			person = Person.findById(personId);
		else
			person = Security.getPerson();
	
		Contract contract = person.getCurrentContract();
		CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(contract, year, null);
		render(csap, person, year);	
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void trainingHours(Long personId, int year, int month){
		Person person = Person.findById(personId);
		Logger.debug("Ore di formazione per %s %s nel mese %d dell'anno %d", person.name, person.surname, month, year);
		List<PersonMonthRecap> pmList = null;
		pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.year = ? and pm.person = ?", year, person).fetch();
		
		if(pmList == null || pmList.size() == 0){
			Logger.debug("Lista nulla, la popolo");
			pmList = new ArrayList<PersonMonthRecap>();
			for(int i=1; i< 13; i++){
				PersonMonthRecap pm = new PersonMonthRecap(person, year, i);
				pm.trainingHours = 0;
				pm.hoursApproved = false;
				pm.save();
				pmList.add(new PersonMonthRecap(person,year,i));
				Logger.debug("Aggiunto elemento alla lista dei person month recap con valore del mese %d", i);
			}
		}
		render(person, year, month, pmList);
		
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void insertTrainingHours(long pk, Integer value){
		
		
		PersonMonthRecap pm = PersonMonthRecap.findById(pk);
//		if(pm == null)
//			pm = new PersonMonthRecap(person, year, month);
		Logger.debug("Recuperato person month recap con valori: %s %s %s", pm.person, pm.year, pm.month);
		pm.trainingHours = value;
		pm.hoursApproved = false;
		pm.save();
		flash.success("Aggiornato il valore per le ore di formazione");
		Stampings.stampings(pm.year, pm.month);
	}
}
