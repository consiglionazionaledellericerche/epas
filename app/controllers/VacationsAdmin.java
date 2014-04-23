package controllers;

import helpers.ModelQuery.SimpleResults;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import dao.PersonDao;
import models.ConfYear;
import models.Contract;
import models.Office;
import models.Person;
import models.VacationCode;
import models.rendering.VacationsRecap;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Scope.RenderArgs;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class VacationsAdmin extends Controller{

	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void list(Integer year, String name, Integer page){
		
		if(page==null)
			page = 0;
		
		LocalDate date = new LocalDate();
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), false, date, date);
		
		List<Person> personList = simpleResults.paginated(page).getResults();
		
		List<VacationsRecap> vacationsList = new ArrayList<VacationsRecap>();
		
		List<Person> personsWithVacationsProblems = new ArrayList<Person>();

		for(Person person: personList)
		{
			person.refresh();
			Logger.info("%s", person.surname);
			VacationsRecap vr = null;
			try {
				vr = new VacationsRecap(person, year, person.getCurrentContract(), new LocalDate(), true);
				vacationsList.add(vr);
			}
			catch(IllegalStateException e){
				personsWithVacationsProblems.add(person);
			}
		}
		
		//ConfYear conf = ConfYear.getConfYear(year);
		Office office = Security.getUser().person.office;
		Integer monthExpiryVacationPastYear = ConfYear.getFieldValue("month_expiry_vacation_past_year", year, office);
		Integer dayExpiryVacationPastYear = ConfYear.getFieldValue("day_expiry_vacation_past_year", year, office);
		LocalDate expireDate = LocalDate.now().withMonthOfYear(monthExpiryVacationPastYear).withDayOfMonth(dayExpiryVacationPastYear);
		
		boolean isVacationLastYearExpired = VacationsRecap.isVacationsLastYearExpired(year, expireDate);
		render(vacationsList, isVacationLastYearExpired, personsWithVacationsProblems, year, simpleResults);
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void vacationsCurrentYear(Long personId, Integer anno){
		
		Person person = Person.findById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}

    	//Costruzione oggetto di riepilogo per la persona
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
		    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
    	renderTemplate("Vacations/vacationsCurrentYear.html", vacationsRecap);
	}
	

	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void vacationsLastYear(Long personId, Integer anno){
		
		Person person = Person.findById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
    	
    	//Costruzione oggetto di riepilogo per la persona
    	Contract contract = person.getCurrentContract();
    	
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
    	renderTemplate("Vacations/vacationsLastYear.html", vacationsRecap);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void permissionCurrentYear(Long personId, Integer anno){
		
		Person person = Person.findById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
		
    	//Costruzione oggetto di riepilogo per la persona
		Contract contract = person.getCurrentContract();
		
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
    	renderTemplate("Vacations/permissionCurrentYear.html", vacationsRecap);
	}

	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void manageVacationCode(){
		
		List<VacationCode> vacationCodeList = VacationCode.findAll();
		renderArgs.put("year", session.get("yearSelected"));
		render(vacationCodeList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void edit(Long vacationCodeId){
		VacationCode vc = VacationCode.findById(vacationCodeId);
		render(vc);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void save(){
		VacationCode vacationCode = new VacationCode();
		vacationCode.description = params.get("nome");
		vacationCode.vacationDays = params.get("giorniFerie", Integer.class);
		vacationCode.permissionDays = params.get("giorniPermesso", Integer.class);
		VacationCode vc = VacationCode.find("Select vc from VacationCode vc where vc.description = ?", params.get("nome")).first();
		if(vc == null){
			vacationCode.save();
			flash.success(String.format("Inserito nuovo piano ferie con nome %s", vacationCode.description));
			VacationsAdmin.manageVacationCode();
		}
		else{
			flash.error(String.format("Esiste già un piano ferie con nome: %s. Cambiare il nome.", params.get("nome")));
			VacationsAdmin.manageVacationCode();
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void update(){
		Long vacationCodeId = params.get("vacationCodeId", Long.class);
		VacationCode code = VacationCode.findById(vacationCodeId);
		code.description = params.get("nome");
		code.vacationDays = params.get("giorniFerie", Integer.class);
		code.permissionDays = params.get("giorniPermesso", Integer.class);
		code.save();
		flash.success("Aggiornato valore del piano ferie %s", code.description);
		VacationsAdmin.manageVacationCode();
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void insertVacationCode(){
		VacationCode vacationCode = new VacationCode();
		render(vacationCode);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void discard(){
		manageVacationCode();
	}
	
	
}
