/**
 * 
 */
package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;
import dao.PersonDao;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import security.SecurityRules;

/**
 * @author cristian
 *
 */
public class RequestInit extends Controller {
	
	@Inject
	static SecurityRules rules;
	
	public static class ItemsPermitted {
		
		public boolean viewPerson = false;
		public boolean viewPersonDay = false;
		public boolean viewOffice = false;
		public boolean viewCompetence = false;
		public boolean editCompetence = false;
		public boolean uploadSituation = false;
		public boolean viewWorkingTimeType = false;
		public boolean editWorkingTimeType = false;
		
		public ItemsPermitted() {
			
			if(rules.check(Security.VIEW_PERSON))
				this.viewPerson = true;
			
			if(rules.check(Security.VIEW_PERSON_DAY))
				this.viewPersonDay = true;
			
			if(rules.check(Security.VIEW_OFFICE))
				this.viewOffice = true;
			
			if(rules.check(Security.VIEW_COMPETENCE))
				this.viewCompetence = true;
			
			if(rules.check(Security.EDIT_COMPETENCE))
				this.editCompetence = true;
			
			if(rules.check(Security.UPLOAD_SITUATION))
				this.uploadSituation = true;
			
			if(rules.check(Security.VIEW_WORKING_TIME_TYPE))
				this.viewWorkingTimeType = true;
			
			if(rules.check(Security.EDIT_WORKING_TIME_TYPE))
				this.editWorkingTimeType = true;
			
		}
		
		public boolean isDropDownVisible() {
			
			return viewPerson || viewPersonDay || viewCompetence || uploadSituation;
		}
		
		public boolean isDropDown2Visible() {
			
			return viewOffice || viewWorkingTimeType;
		}
	}
	
	public static class CurrentData {
		public final Integer year;
		public final Integer month;
		public final Integer day;
		public final Long personId;
		
		CurrentData(Integer year, Integer month, Integer day, Long personId) {
			this.year = year;
			this.month = month;
			this.day = day;
			this.personId = personId;
		}
		
		public String getMonthLabel() {
			return Messages.get("Month." + month);
		}
	}

	public static class TemplateUtility {

		public String monthName(String month) {

			return DateUtility.getName(Integer.parseInt(month));
		}
		
		public String monthName(Integer month) {

			return DateUtility.getName(month);
		}
		
		public boolean checkTemplate(String profile) {
			
			return false;
		}
	}

	@Before
	public static void injectUtility() {

		TemplateUtility templateUtility = new TemplateUtility();
		renderArgs.put("templateUtility", templateUtility);

	}


	@Before 
	@NoCheck
	public static void injectMenu() { 
		
		ItemsPermitted ip = new ItemsPermitted();
		renderArgs.put("ip", ip);
	

		session.put("actionSelected", computeActionSelected(Http.Request.current().action));

		// year init /////////////////////////////////////////////////////////////////
		Integer year;
		if ( params.get("year") != null ) {

			year = Integer.valueOf(params.get("year"));
		} 
		else if (session.get("yearSelected") != null ){

			year = Integer.valueOf(session.get("yearSelected"));
		}
		else {

			year = LocalDate.now().getYear();
		}

		session.put("yearSelected", year);
		
		// month init ////////////////////////////////////////////////////////////////
		Integer month;
		if ( params.get("month") != null ) {

			month = Integer.valueOf(params.get("month"));
		} 
		else if ( session.get("monthSelected") != null ){

			month = Integer.valueOf(session.get("monthSelected"));
		}
		else {

			month = LocalDate.now().getMonthOfYear();
		}
		
		session.put("monthSelected", month);
		
		// day init //////////////////////////////////////////////////////////////////
		Integer day;
		if ( params.get("day") != null ) {

			day = Integer.valueOf(params.get("day"));
		} 
		else if ( session.get("daySelected") != null ){

			day = Integer.valueOf(session.get("daySelected"));
		}
		else {

			day = LocalDate.now().getDayOfMonth();
		}

		session.put("daySelected", day);
		
		// person init //////////////////////////////////////////////////////////////
		Integer personId;
		if ( params.get("personId") != null ) {

			personId = Integer.valueOf(params.get("personId"));
			session.put("personSelected", personId);
		} 
		else if ( session.get("personSelected") != null ){

			personId = Integer.valueOf(session.get("personSelected"));
		}
		else if( Security.getUser().get().person != null ){

			session.put("personSelected", Security.getUser().get().person.id);
		}
		else {

			session.put("personSelected", 1);
		}

		renderArgs.put("currentData", new CurrentData(year, month, day, 
				Long.valueOf(session.get("personSelected"))));

		
		LocalDate beginMonth = new LocalDate(year,month,1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		String name = null;
		if(Security.getUser().get().person != null) {
			
			List<Person> persons = PersonDao.list(Optional.fromNullable(name), 
					Sets.newHashSet(Security.getOfficeAllowed()), false, beginMonth, endMonth).list();
			renderArgs.put("navPersons", persons);
		} 
		else {

			List<Office> allOffices = Office.findAll();
			List<Person> persons = PersonDao.list(Optional.fromNullable(name), 
					Sets.newHashSet(allOffices), false, beginMonth, endMonth).list();

			renderArgs.put("navPersons", persons);
		}

		// day lenght (provvisorio)
		try {
			
			Integer dayLenght = new LocalDate(year, month, day).dayOfMonth().withMaximumValue().getDayOfMonth();
			renderArgs.put("dayLenght", dayLenght);
		}
		catch (Exception e) {
			
		}
		 
		
		
		

		/*
		LocalDate now = new LocalDate();
		User userLogged = Security.getUser();
		if(userLogged==null)
		{
			flash.error("Nessun utente risulta loggato");
			Application.index(); 	
		}
		Integer year;
		Integer month;
		Integer day;
		Long personId;
		String method = "";

		if(session.get("dispatched")!= null && session.get("dispatched").equals("true"))
		{
			year = Integer.parseInt(session.get("yearSelected"));
			month = Integer.parseInt(session.get("monthSelected"));
			day = Integer.parseInt(session.get("daySelected"));
			personId = Long.parseLong(session.get("personSelected"));
			method = session.get("methodSelected");

		}
		else
		{
			//Year from routes (otherwise now)
			year = params.get("year") != null ? Integer.valueOf(params.get("year")) : now.getYear(); 
			session.put("yearSelected", year);

			//Month from routes (otherwise now)
			month = params.get("month") != null  ? Integer.valueOf(params.get("month")) : now.getMonthOfYear();
			session.put("monthSelected", month);
			session.put("monthSelectedName", DateUtility.getName(month));

			//Day from routes (otherwise now)
			day = params.get("day") != null ? Integer.valueOf(params.get("day")) : now.getDayOfMonth();
			session.put("daySelected", day);

			//personId from routes (otherwise security)
			if(params.get("personId")!=null)
				personId = Long.parseLong(params.get("personId"));
			else if(userLogged.person != null)
				personId = userLogged.person.id;
			else
				personId = 1l; //admin id

			//personId = params.get("personId") != null ? Long.parseLong(params.get("personId")) : Security.getUser().person.id; 
			session.put("personSelected", personId);

			//Method from Http.Request
			method = getFormAction(Http.Request.current().action);
			session.put("methodSelected", method);

		}

		session.put("dispatched", "false");

		List<Person> persons = null;
		if(userLogged.person != null)
		{
			persons = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		}
		else
		{
			List<Office> allOffices = Office.findAll();
			persons = Person.getActivePersonsInMonth(month, year, allOffices, false);
		}

		ActionMenuItem action;
		if(method != null && !method.equals("")) 
			action = ActionMenuItem.valueOf(method);
		else
			action = ActionMenuItem.stampingsAdmin;

		MainMenu mainMenu = null;
		if(action.getDescription().equals("Riepilogo mensile"))
		{
			mainMenu = new MainMenu(year, month, action);
		}
		if(action.getDescription().equals("Presenza giornaliera"))
		{
			mainMenu = new MainMenu(personId, year, month, day, action, persons);			
		}
		else
		{
			mainMenu = new MainMenu(personId, year, month, action, persons);
		}		

		//Se personId è una persona reale (1 admin, 0 tutti) eseguo il controllo
		if( personId > 1 )
		{
			if( !Security.canUserSeePerson(userLogged, personId) )
			{
				flash.error("Non si può accedere alla funzionalità per la persona con id %d", personId);
				renderArgs.put("mainMenu", mainMenu);
				Application.indexAdmin();
			}
		}
		renderArgs.put("mainMenu", mainMenu);
		 */
	}
	
	private static String computeActionSelected(String action) {
		
		
		if( action.startsWith("Stampings.")) {
			
			if(action.equals("Stampings.stampings")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "Stampings.stampings";
			}
			
			if(action.equals("Stampings.personStamping")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Stampings.personStamping";
			}
			
			if(action.equals("Stampings.missingStamping")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Stampings.missingStamping";
			}
			
			if(action.equals("Stampings.dailyPresence")) {
				
				renderArgs.put("switchDay", true);
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Stampings.dailyPresence";
			}
			
			if(action.equals("Stampings.mealTicketSituation")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Stampings.mealTicketSituation";
			}

		}
		
		if( action.startsWith("PersonMonths.")) {
			
			if(action.equals("PersonMonths.trainingHours")) {
				
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "PersonMonths.trainingHours";
			}
			
			if(action.equals("PersonMonths.hourRecap")) {
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "PersonMonths.hourRecap";
			}
		}
		
		if( action.startsWith("Vacations.")) {
			
			if(action.equals("Vacations.show")) {
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "Vacations.show";
			}
		}
		
		if( action.startsWith("Persons.")) {
			
			if(action.equals("Persons.changePassword")) {
				
				renderArgs.put("dropDown", "dropDown1");
				return "Persons.changePassword";
			}
			if(action.equals("Persons.list")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Persons.list";
			}
			
			if(action.equals("Persons.edit")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Persons.edit";
			}
		}
		
		if(action.startsWith("Absences.")) {
			
			if(action.equals("Absences.absences")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "Absences.absences";
			}
			
			if(action.equals("Absences.manageAttachmentsPerCode")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Absences.manageAttachmentsPerCode";
			}
			
			if(action.equals("Absences.manageAttachmentsPerPerson")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Absences.manageAttachmentsPerPerson";
			}
			
			if(action.equals("Absences.absenceInPeriod")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Absences.absenceInPeriod";
			}
		}
		
		if(action.startsWith("YearlyAbsences.")) {
			
			if(action.equals("YearlyAbsences.absencesPerPerson")) {
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "YearlyAbsences.absencesPerPerson";
			}
			
			if(action.equals("YearlyAbsences.showGeneralMonthlyAbsences")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "YearlyAbsences.showGeneralMonthlyAbsences";
			}
			
			if(action.equals("YearlyAbsences.yearlyAbsences")) {
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "YearlyAbsences.yearlyAbsences";
			}
		}
		
		if(action.startsWith("Competences.")) {
			
			if(action.equals("Competences.competences")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown1");
				return "Competences.competences";
			}
			
			if(action.equals("Competences.showCompetences")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.showCompetences";
			}
			
			if(action.equals("Competences.overtime")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.overtime";
			}
			
			if(action.equals("Competences.totalOvertimeHours")) {
				
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.totalOvertimeHours";
			}
			
			if(action.equals("Competences.enabledCompetences")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.enabledCompetences";
			}
			
			if(action.equals("Competences.exportCompetences")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.exportCompetences";
			}

		}
		
		if(action.startsWith("MonthRecaps.")) {
			
			if(action.equals("MonthRecaps.show")) {
				
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDown2");
				return "MonthRecaps.show";
			}
		}
		
		if(action.startsWith("UploadSituation.")) {
			
			if(action.equals("UploadSituation.show")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "UploadSituation.show";
			}
			
			if(action.equals("UploadSituation.loginAttestati")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "UploadSituation.loginAttestati";
			}
			
			if(action.equals("UploadSituation.processAttestati")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "UploadSituation.processAttestati";
			}
		}
		
		if(action.startsWith("WorkingTimes.")) {
			
			if(action.equals("WorkingTimes.manageWorkingTime")) {
				
				renderArgs.put("dropDown", "dropDown3");
				return "WorkingTimes.manageWorkingTime";
			}
		}
		
		
		
		return session.get("actionSelected");
	}

}

