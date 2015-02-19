package controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import manager.PersonMonthsManager;
import manager.PersonMonthsManager.Insertable;
import manager.recaps.PersonResidualYearRecap;
import models.Contract;
import models.Person;
import models.PersonMonthRecap;
import models.User;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.ContractDao;
import dao.PersonMonthRecapDao;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Resecure.class, RequestInit.class} )
public class PersonMonths extends Controller{
	
	
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
		
		Contract contract = ContractDao.getCurrentContract(user.person);
		PersonResidualYearRecap csap = 
				PersonResidualYearRecap.factory(contract, year, null);
		render(csap, user.person, year);	
	}

	
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
		
		List<PersonMonthRecap> pmList = PersonMonthRecapDao.getPersonMonthRecapInYearOrWithMoreDetails(person, year, Optional.<Integer>absent(), Optional.<Boolean>absent());
		LocalDate today = new LocalDate();

		render(person, year, mesi, pmList, today);

	}


	
	public static void insertTrainingHours(int month, int year){

		Person person = Security.getUser().get().person;
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
		
		render(person, month, year, max);
	}
	
	
	
	public static void insertTrainingHoursPreviousMonth(){
		
		Person person = Security.getUser().get().person;
		LocalDate date = new LocalDate();
		int month = 0;
		int year = 0;
		int max = 0;
		if(date.getMonthOfYear() == 1){
			date = date.minusMonths(1);
			month = date.getMonthOfYear();
			year = date.getYear();
			max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
			render(person, month, year, max);
		}
		max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
		month = date.minusMonths(1).getMonthOfYear();
		year = date.getYear();
		
		render(person, month, year, max);
	}

	
	public static void modifyTrainingHours(Long personMonthSituationId){
		
		PersonMonthRecap pm = PersonMonthRecapDao.getPersonMonthRecapById(personMonthSituationId);
		
		int year = pm.year;
		int month = pm.month;
		Person person = pm.person;
		LocalDate date = new LocalDate(year, month, 1);
		int max = date.dayOfMonth().withMaximumValue().getDayOfMonth();
		render(person, pm, max, year, month);
	}

	
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
		Insertable rr = PersonMonthsManager.checkIfInsertable(begin, end, value, beginDate, endDate);
		if(rr.getResult() == false){
			flash.error(rr.getMessage());
			PersonMonths.trainingHours(beginDate.getYear());
		}			
		rr = PersonMonthsManager.checkIfPeriodAlreadyExists(person, year, month, beginDate, endDate);
		if(rr.getResult() == false){
			flash.error(rr.getMessage());
			PersonMonths.trainingHours(beginDate.getYear());
		}		
		
		/* Si cerca di inserire delle ore di formazione per il mese precedente: se le ore di formazione sono già state inviate insieme
		 * agli attestati, il sistema non permette l'inserimento.
		 * In caso contrario sì
		 */
		rr = PersonMonthsManager.checkIfAlreadySend(person, year, month);
		if(rr.getResult() == false){
			flash.error(rr.getMessage());
			trainingHours(year);
		}		
			
		PersonMonthRecap pm = new PersonMonthRecap(person, year, month);
		PersonMonthsManager.saveTrainingHours(pm, false, value, beginDate, endDate);
		flash.success("Salvate %d ore di formazione ", value);
		
		PersonMonths.trainingHours(year);
	}
	
	
	public static void updateTrainingHours(@Valid int begin, @Valid int end, @Valid Integer value, int month, int year, Long personMonthId){
		
		if (validation.hasErrors()) {
			flash.error("Ci sono errori");
			Application.indexAdmin();
			return;
		}
		
		LocalDate beginDate = new LocalDate(year, month, begin);
		LocalDate endDate = new LocalDate(year, month, end);
		
		Insertable rr = PersonMonthsManager.checkIfInsertable(begin, end, value, beginDate, endDate);
		if(rr.getResult() == false){
			flash.error(rr.getMessage());
			PersonMonths.trainingHours(beginDate.getYear());
		}	
		PersonMonthRecap pm = PersonMonthRecapDao.getPersonMonthRecapById(personMonthId);	
		rr = PersonMonthsManager.checkIfExist(pm);
		if(rr.getResult() == false){
			flash.error(rr.getMessage());
			PersonMonths.trainingHours(beginDate.getYear());
		}
		
		Person person = Security.getUser().get().person;
		if( person == null || !person.id.equals(pm.person.id)) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		PersonMonthsManager.saveTrainingHours(pm, false, value, beginDate, endDate);
		
		flash.success("Aggiornate ore di formazione per %s %s", person.name, person.surname);
		PersonMonths.trainingHours(beginDate.getYear());
	}
	
	
	public static void deleteTrainingHours(Long personId, Long personMonthRecapId){
		PersonMonthRecap pm = PersonMonthRecapDao.getPersonMonthRecapById(personMonthRecapId);
		if(pm == null)
		{
			flash.error("Ore di formazioni inesistenti. Operazione annullata.");
			PersonMonths.trainingHours(LocalDate.now().getYear());
			
		}
		render(pm);
	}
	
	
	public static void deleteTrainingHoursConfirmed( Long personMonthRecapId ){
		
		PersonMonthRecap pm = PersonMonthRecapDao.getPersonMonthRecapById(personMonthRecapId);
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
