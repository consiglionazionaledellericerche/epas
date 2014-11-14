/**
 * 
 */
package controllers;

import helpers.ModelQuery;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.ConfGeneral;
import models.Office;
import models.Permission;
import models.Person;
import models.enumerate.ConfigurationFields;
import models.query.QPermission;
import models.query.QRole;
import models.query.QUsersRolesOffices;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import controllers.Resecure.NoCheck;
import dao.PersonDao;
import play.Logger;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

/**
 * @author cristian
 *
 */
public class RequestInit extends Controller {
	
	//@Inject
	//static SecurityRules rules;
	
	public static class ItemsPermitted {
		
		public boolean viewPerson = false;
		public boolean viewPersonDay = false;
		public boolean viewOffice = false;
		public boolean viewCompetence = false;
		public boolean editCompetence = false;
		public boolean uploadSituation = false;
		public boolean viewWorkingTimeType = false;
		public boolean editWorkingTimeType = false;
		public boolean viewAbsenceType = false;
		public boolean editAbsenceType = false;
		public boolean viewCompetenceCode = false;
		public boolean editCompetenceCode = false;
		
		
		public ItemsPermitted() {
			
			final QUsersRolesOffices quro = QUsersRolesOffices.usersRolesOffices;
			final QRole qr = QRole.role;
			final QPermission qp = QPermission.permission;
						
			final JPQLQuery query = ModelQuery.queryFactory().from(qp)
					.leftJoin(qp.roles, qr).fetch()
					.leftJoin(qr.usersRolesOffices, quro).fetch()
					.distinct();
					
			final BooleanBuilder condition = new BooleanBuilder();
			if(Security.getUser().isPresent())
				condition.and(quro.user.eq(Security.getUser().get()));
			else{
				Logger.info("Si tenta di accedere a una risorsa senza essere correttamente loggati");
				
				flash.error("Bisogna autenticarsi prima di accedere a una risorsa");
				try {
					Secure.login();
				} catch (Throwable e) {
					
					Application.index();
				}
			}
			
			query.where(condition);
			
			List<Permission> pList = ModelQuery.simpleResults(query, qp).list();
			

			for(Permission p : pList) {
				
				if(p.description.equals("viewPerson"))
					this.viewPerson = true;
				
				if(p.description.equals("viewPersonDay"))
					this.viewPersonDay = true;
				
				if(p.description.equals("viewOffice"))
					this.viewOffice = true;
				
				if(p.description.equals("viewCompetence"))
					this.viewCompetence = true;
				
				if(p.description.equals("editCompetence"))
					this.editCompetence = true;
				
				if(p.description.equals("uploadSituation"))
					this.uploadSituation = true;
				
				if(p.description.equals("viewWorkingTimeType"))
					this.viewWorkingTimeType = true;
				
				if(p.description.equals("editWorkingTimeType"))
					this.editWorkingTimeType = true;
				
				if(p.description.equals("viewCompetenceCode"))
					this.viewCompetenceCode = true;
				
				if(p.description.equals("editCompetenceCode"))
					this.editCompetenceCode = true;
				
				if(p.description.equals("viewAbsenceType"))
					this.viewAbsenceType = true;
				
				if(p.description.equals("editAbsenceType"))
					this.editAbsenceType = true;
				
				
				
			}
						

		}
		
		public boolean isDropDownVisible() {
			
			return viewPerson || viewPersonDay || viewCompetence || uploadSituation;
		}
		
		public boolean isDropDown2Visible() {
			
			return viewOffice || viewWorkingTimeType || viewAbsenceType;
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
			List<Office> officeList = Security.getOfficeAllowed();
			if(officeList.size() > 0) {
			List<Person> persons = PersonDao.list(Optional.fromNullable(name), 
					Sets.newHashSet(Security.getOfficeAllowed()), false, beginMonth, endMonth, true).list();
			renderArgs.put("navPersons", persons);
			}
		} 
		else {

			List<Office> allOffices = Office.findAll();
			if (allOffices!=null && !allOffices.isEmpty()){
			List<Person> persons = PersonDao.list(Optional.fromNullable(name), 
					Sets.newHashSet(allOffices), false, beginMonth, endMonth, true).list();
			renderArgs.put("navPersons", persons);
			}
		}

		// day lenght (provvisorio)
		try {
			
			Integer dayLenght = new LocalDate(year, month, day).dayOfMonth().withMaximumValue().getDayOfMonth();
			renderArgs.put("dayLenght", dayLenght);
		}
		catch (Exception e) {
			
		}		
		
		/**
		 *  years per la gestione dinamica degli anni(provvisorio)
		 */
		List<Integer> years = Lists.newArrayList();
		Integer actualYear = new LocalDate().getYear();
		ConfGeneral yearBegin = ConfGeneral.find("Select c from ConfGeneral c where c.field = ? ", 
				ConfigurationFields.InitUseProgram.description).first();
		Integer yearBeginProgram = new Integer(yearBegin.fieldValue.substring(0, 4));
		Logger.debug("yearBeginProgram = %s", yearBeginProgram);

		while(yearBeginProgram <= actualYear+1){
			years.add(yearBeginProgram);
			Logger.debug("Aggiunto %s alla lista", yearBeginProgram);
			yearBeginProgram++;
		}

		renderArgs.put("navYears", years);
		
		
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

