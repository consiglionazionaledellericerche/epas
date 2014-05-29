package controllers;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;

import models.Contract;
import models.Person;
import models.PersonMonthRecap;
import models.User;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class PersonMonths extends Controller{
	
	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void hourRecap(int year){

		//controllo dei parametri
		User user = Security.getUser().get();
		if( user == null || user.person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}

		if(year > new LocalDate().getYear()){
			flash.error("Richiesto riepilogo orario di un anno futuro. Impossibile soddisfare la richiesta");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Contract contract = user.person.getCurrentContract();
		CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(contract, year, null);
		render(csap, user.person, year);	
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void trainingHours(int year){
		
		if( Security.getUser().get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Person person = Security.getUser().get().person;
		
		List<Integer> mesi = new ArrayList<Integer>();
		for(int i = 1; i < 13; i++){
			mesi.add(i);
		}
		List<PersonMonthRecap> pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.year = ? and pm.person = ?",
				year, person).fetch();
		
		LocalDate today = new LocalDate();
		

		render(person, year, mesi, pmList, today);

	}


	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void insertTrainingHours(int month, int year){
		
		/*
		 		Person person = Security.getUser().person;
				int max = LocalDate.now().dayOfMonth().withMaximumValue().getDayOfMonth();
				int actualMonth = LocalDate.now().getMonthOfYear();
				render(person, actualMonth, year, max);
		 */
		Person person = Security.getUser().get().person;
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();

		
		render(person, month, year, max);
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void modifyTrainingHours(Long personMonthSituationId){
		
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthSituationId);
		int year = pm.year;
		int month = pm.month;
		Person person = pm.person;
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
		render(person, pm, max, year, month);
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void saveTrainingHours(@Valid int begin, @Valid int end, @Valid Integer value, int month, int year){
		if (validation.hasErrors()) {
			flash.error("Ci sono errori");
			Application.indexAdmin();
			return;
		}

		Person person = Security.getUser().get().person;
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Logger.debug("nome e cognome: %s %s", person.name, person.surname);
		LocalDate beginDate = new LocalDate(year, month, begin);
		LocalDate endDate = new LocalDate(year, month, end);
		if(begin > end){
			flash.error("La data di inizio del periodo di formazione non può essere successiva a quella di fine");
			PersonMonths.trainingHours(beginDate.getYear());
		}
		if(value == null || value < 0 || value > 24*(endDate.getDayOfMonth()-beginDate.getDayOfMonth()+1)){
			flash.error("Non sono valide le ore di formazione negative, testuali o che superino la quantità massima di ore nell'intervallo temporale inserito.");
			PersonMonths.trainingHours(beginDate.getYear());
		}

		List<PersonMonthRecap> pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.person = ? and pm.year = ? " +
				"and pm.month = ? and (? between pm.fromDate and pm.toDate or ? between pm.fromDate and pm.toDate)", 
				person, year, month, beginDate, endDate).fetch();
		if(pmList != null && pmList.size() > 0){
			flash.error("Esiste un periodo di ore di formazione che contiene uno o entrambi i giorni specificati.");
			PersonMonths.trainingHours(beginDate.getYear());
		}
		
		/* Si cerca di inserire delle ore di formazione per il mese precedente: se le ore di formazione sono già state inviate insieme
		 * agli attestati, il sistema non permette l'inserimento.
		 * In caso contrario sì
		 */
		List<PersonMonthRecap> list = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.person = ? and pm.month = ? and pm.year = ? and pm.hoursApproved = ?",
				person, month, year, true).fetch();
		if(list.size() > 0){
			flash.error("Impossibile inserire ore di formazione per il mese precedente poichè gli attestati per quel mese sono già stati inviati");
			trainingHours(year);
		}
			
		PersonMonthRecap pm = new PersonMonthRecap(person, year, month);
		pm.hoursApproved = false;
		pm.trainingHours = value;
		pm.fromDate = beginDate;
		pm.toDate = endDate;
		pm.save();
		flash.success("Salvate %d ore di formazione ", value);
		
		PersonMonths.trainingHours(year);
	}
	
	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void updateTrainingHours(@Valid int begin, @Valid int end, @Valid Integer value, int month, int year, Long personMonthId){
		
		if (validation.hasErrors()) {
			flash.error("Ci sono errori");
			Application.indexAdmin();
			return;
		}
		
		LocalDate beginDate = new LocalDate(year, month, begin);
		LocalDate endDate = new LocalDate(year, month, end);
		if(begin > end){
			flash.error("La data di inizio del periodo di formazione non può essere successiva a quella di fine");
			PersonMonths.trainingHours(beginDate.getYear());
		}
		if(value == null || value < 0 || value > 24*beginDate.dayOfMonth().withMaximumValue().getDayOfMonth()){
			flash.error("Non sono valide le ore di formazione negative o testuali.");
			PersonMonths.trainingHours(beginDate.getYear());
		}
		
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthId);
		if(pm == null) {
			
			flash.error("Ore di formazione non trovate. Operazione annullata.");
			PersonMonths.trainingHours(beginDate.getYear());
		}
		
		Person person = Security.getUser().get().person;
		if( person == null || !person.id.equals(pm.person.id)) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		
		

		pm.hoursApproved = false;
		pm.trainingHours = value;
		pm.fromDate = beginDate;
		pm.toDate = endDate;
		pm.save();
		
		flash.success("Aggiornate ore di formazione per %s %s", person.name, person.surname);
		PersonMonths.trainingHours(beginDate.getYear());
	}
	
	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void deleteTrainingHours(Long personId, Long personMonthRecapId){
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthRecapId);
		if(pm == null)
		{
			flash.error("Ore di formazioni inesistenti. Operazione annullata.");
			PersonMonths.trainingHours(LocalDate.now().getYear());
			
		}
		render(pm);
	}
	
	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void deleteTrainingHoursConfirmed( Long personMonthRecapId ){
		
		PersonMonthRecap pm = PersonMonthRecap.findById(personMonthRecapId);
		if(pm == null)
		{
			flash.error("Ore di formazioni inesistenti. Operazione annullata.");
			Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
		}
		
		Person person = Security.getUser().get().person;
		if( person == null || !person.id.equals(pm.person.id)) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		
		pm.delete();
		flash.error("Ore di formazione eliminate con successo.");
		PersonMonths.trainingHours(pm.year);
	}
}
