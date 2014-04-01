package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.hibernate.envers.reader.FirstLevelCache;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

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
import play.Play;

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
	public static void insertTrainingHours(Long personId, int month, int year){
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();

		Person person = Person.findById(personId);
		render(person, month, year, max);
	}

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void modifyTrainingHours(Long personMonthSituationId){
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthSituationId);
		int year = pm.year;
		int month = pm.month;
		Person person = pm.person;
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
//		List<PersonMonthRecap> pmList = PersonMonthRecap.find("select pm from PersonMonthRecap pm where pm.person = ? and pm.year = ? and pm.month = ?", 
//				person, year, month).fetch();
		render(person, pm, max, year, month);
	}

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void saveTrainingHours(@Valid int begin, @Valid int end, @Valid Integer value, Long personId, int month, int year){
		if (validation.hasErrors()) {
			flash.error("Ci sono errori");
			Application.indexAdmin();
			return;
		}

		Person person = Person.findById(personId);
		Logger.debug("nome e cognome: %s %s", person.name, person.surname);
		LocalDate beginDate = new LocalDate(year, month, begin);
		LocalDate endDate = new LocalDate(year, month, end);
		if(begin > end){
			flash.error("La data di inizio del periodo di formazione non può essere successiva a quella di fine");
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}
		if(value == null || value < 0 || value > 24*beginDate.dayOfMonth().withMaximumValue().getDayOfMonth()){
			flash.error("Non sono valide le ore di formazione negative o testuali.");
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}

		List<PersonMonthRecap> pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.person = ? and pm.year = ? " +
				"and pm.month = ? and (? between pm.fromDate and pm.toDate or ? between pm.fromDate and pm.toDate)", 
				person, year, month, beginDate, endDate).fetch();
		if(pmList != null && pmList.size() > 0){
			flash.error("Esiste un periodo di ore di formazione che contiene uno o entrambi i giorni specificati.");
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}
		
		PersonMonthRecap pm = new PersonMonthRecap(person, year, month);
		pm.hoursApproved = false;
		pm.trainingHours = value;
		pm.fromDate = beginDate;
		pm.toDate = endDate;
		pm.save();
		flash.success("Salvate %d ore di formazione ", value);
		PersonMonths.trainingHours(personId, pm.year, pm.month);
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void updateTrainingHours(@Valid int begin, @Valid int end, @Valid Integer value, Long personId, int month, int year, Long personMonthId){
		if (validation.hasErrors()) {
			flash.error("Ci sono errori");
			Application.indexAdmin();
			return;
		}
		Person person = Person.findById(personId);
		LocalDate beginDate = new LocalDate(year, month, begin);
		LocalDate endDate = new LocalDate(year, month, end);
		if(begin > end){
			flash.error("La data di inizio del periodo di formazione non può essere successiva a quella di fine");
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}
		if(value == null || value < 0 || value > 24*beginDate.dayOfMonth().withMaximumValue().getDayOfMonth()){
			flash.error("Non sono valide le ore di formazione negative o testuali.");
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthId);
		if(pm != null){
			pm.hoursApproved = false;
			pm.trainingHours = value;
			pm.fromDate = beginDate;
			pm.toDate = endDate;
			pm.save();
		}
		else{
			flash.error("Non ci sono ore di formazione per %s %s in questo mese da modificare!!!", person.name, person.surname);
			PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
		}
		flash.success("Aggiornate ore di formazione per %s %s", person.name, person.surname);
		PersonMonths.trainingHours(personId, beginDate.getYear(), beginDate.getMonthOfYear());
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void deleteTrainingHours(Long personMonthRecapId, int year, int month){
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthRecapId);
		if(pm == null)
		{
			flash.error("Ore di formazioni inesistenti. Operazione annullata.");
			Stampings.stampings(year, month);
		}
		render(pm, year, month);
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void deleteTrainingHoursConfirmed(Long personMonthRecapId, int year, int month){
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthRecapId);
		if(pm == null)
		{
			flash.error("Ore di formazioni inesistenti. Operazione annullata.");
			Stampings.stampings(year, month);
		}
		pm.delete();
		flash.error("Ore di formazione eliminate con successo.");
		Stampings.stampings(year, month);
	}
}
