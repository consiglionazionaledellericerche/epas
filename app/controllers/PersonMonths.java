package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

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
		Logger.debug("Ore di formazione per %s %s dell'anno %d", person.name, person.surname, year);
		Map<Integer, List<PersonMonthRecap>> pmMap = new HashMap<Integer, List<PersonMonthRecap>>();
		List<PersonMonthRecap> pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.year = ? and pm.person = ?", year, person).fetch();
		Logger.debug("Lista di ore di formazione: %s", pmList);
		
		List<PersonMonthRecap> list = null;
		for(int i=1; i< 13; i++){
			PersonMonthRecap pm = new PersonMonthRecap(person, year, i);
			List<PersonMonthRecap> listina = new ArrayList<PersonMonthRecap>();
			pm.trainingHours = 0;
			pm.hoursApproved = false;
			
			listina.add(new PersonMonthRecap(person,year,i));
			pmMap.put(pm.month, listina);
		}
		for(PersonMonthRecap pm : pmList){
			if(!pmMap.containsKey(pm.month)){
				list = new ArrayList<PersonMonthRecap>();
				list.add(pm);
				pmMap.put(pm.month, list);
			}
			else{
				list = pmMap.get(pm.month);
				list.add(pm);
				pmMap.put(pm.month, list);
			}
		}
		
		render(person, year, pmMap, month);

	}


	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void insertTrainingHours(Long personId){
		Person person = Person.findById(personId);
		render(person);
	}

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void saveTrainingHours(@Valid String begin, @Valid String end, @Valid Integer value, Long personId){
		Person person = Person.findById(personId);
		Logger.debug("Inizio: %s", begin);
		Logger.debug("Fine: %s", end);
		Logger.debug("Valore: %d", value);
		Logger.debug("Persona: %s %s", person.name, person.surname);
		if((begin == null || end == null) || (begin.equals("") || end.equals(""))){
			flash.error("Le date devono essere valorizzate");
			Application.indexAdmin();
		}
		LocalDate from = new LocalDate(begin);
		LocalDate to = new LocalDate(end);
		if(from.isAfter(to)){
			flash.error("La data di inizio del periodo di formazione non può essere successiva a quella di fine");
			PersonMonths.trainingHours(personId, from.getYear(), from.getMonthOfYear());
		}
		if(value == null || value < 0){
			flash.error("Non sono valide le ore di formazione negative o testuali.");
			PersonMonths.trainingHours(personId, from.getYear(), from.getMonthOfYear());
		}
		PersonMonthRecap pm = new PersonMonthRecap(person, from.getYear(), from.getMonthOfYear());
		pm.hoursApproved = false;
		pm.trainingHours = value;
		pm.fromDate = from;
		pm.toDate = to;
		pm.save();
		flash.success("Salvate %d ore di formazione ", value);
		PersonMonths.trainingHours(personId, pm.year, pm.month);
	}
}
