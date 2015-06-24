/**
 * 
 */
package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import models.ConfGeneral;
import models.Office;
import models.Permission;
import models.Person;
import models.Qualification;
import models.User;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;
import dao.ConfGeneralDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.UsersRolesOfficesDao;

/**
 * @author cristian
 *
 */
public class RequestInit extends Controller {

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static ConfGeneralDao confGeneralDao;
	@Inject
	private static UsersRolesOfficesDao uroDao;
	@Inject
	private static QualificationDao qualificationDao;
	/**
	 * Oggetto che modella i permessi abilitati per l'user
	 * TODO: esportare questa classe in un nuovo file che modella la view.
	 * @param user
	 */
	public static class ItemsPermitted {

		public boolean isEmployee = false;

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

		public ItemsPermitted(Optional<User> user) {

			if(!user.isPresent())
				return;

			List<Permission> pList = uroDao.getUserPermission(user.get());

			for(Permission p : pList) {

				if(p.description.equals("employee"))
					this.isEmployee = true;

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

		/**
		 * Se l'user può vedere il menu del Employee.
		 * 
		 * @return
		 */
		public boolean isEmployeeVisible() {
			return isEmployee;
		}

		/**
		 * Se l'user ha i permessi per vedere Amministrazione.
		 * 
		 * @return
		 */
		public boolean isAdministrationVisible() {

			return viewPerson || viewPersonDay || viewCompetence || uploadSituation;
		}

		/**
		 * Se l'user ha i permessi per vedere Configurazione.
		 * @return
		 */
		public boolean isConfigurationVisible() {

			return viewOffice || viewWorkingTimeType || viewAbsenceType;
		}
		
	}

	/**
	 * Contiene i dati di sessione raccolti per il template.
	 * 
	 * @author alessandro
	 *
	 */
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

	/**
	 * Metodi usabili nel template.
	 * @author alessandro
	 *
	 */
	public static class TemplateUtility {

		///////////////////////////////////////////////////////////////////////////7
		//Convertitori mese

		public String monthName(String month) {

			return DateUtility.getName(Integer.parseInt(month));
		}

		public String monthName(Integer month) {

			return DateUtility.getName(month);
		}

		public String monthNameByString(String month){
			if(month != null)
				return DateUtility.getName(Integer.parseInt(month));
			else
				return null;
		}

		public boolean checkTemplate(String profile) {

			return false;
		}



		///////////////////////////////////////////////////////////////////////////7
		//Navigazione menu (next/previous month)

		public int computeNextMonth(int month){
			if(month==12)
				return 1;

			return month + 1;
		}

		public int computeNextYear(int month, int year){
			if(month==12)
				return year + 1;

			return year;
		}

		public int computePreviousMonth(int month){
			if(month==1)
				return 12;

			return month - 1;
		}

		public int computePreviousYear(int month, int year){
			if(month==1)
				return year - 1;

			return year;
		}

		///////////////////////////////////////////////////////////////////////////7
		//Liste di utilità per i template

		public List<Qualification> getAllQualifications() {
			return qualificationDao.findAll();
		}

		public Set<Office> getAllOfficesAllowed() {
			return officeDao.getOfficeAllowed(Security.getUser().get());
		}
	}

	@Before (priority = 1)
	static void injectUtility() {
		TemplateUtility templateUtility = new TemplateUtility();
		renderArgs.put("templateUtility", templateUtility);
	}


	@Before (priority = 1)
	@NoCheck
	static void injectMenu() { 

		Optional<User> user = Security.getUser();

		ItemsPermitted ip = new ItemsPermitted(user);
		renderArgs.put("ip", ip);
		
		if(!user.isPresent()) {
			return;
		}
		
		if(user.get().person != null) {
			renderArgs.put("isPersonInCharge", user.get().person.isPersonInCharge);
		}
			
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
		else if( user.get().person != null ){

			session.put("personSelected", user.get().person.id);
		}
		else {

			session.put("personSelected", 1);
		}

		renderArgs.put("currentData", new CurrentData(year, month, day, 
				Long.valueOf(session.get("personSelected"))));

		if(user.get().person != null) {
			
			Set<Office> officeList = officeDao.getOfficeAllowed(user.get());
			if(!officeList.isEmpty()) {
				List<Person> persons = personDao
						.getActivePersonInMonth(officeList, new YearMonth(year, month)); 	
//				List<PersonLite> persons = personDao
//						.liteList(officeList, year, month);
				renderArgs.put("navPersons", persons);
			}
		}  else {

			List<Office> allOffices = officeDao.getAllOffices();
			if (allOffices!=null && !allOffices.isEmpty()) {
				List<Person> persons = personDao.getActivePersonInMonth(
						Sets.newHashSet(allOffices), new YearMonth(year, month));
//				List<PersonLite> persons = personDao
//						.liteList(Sets.newHashSet(allOffices), year, month);
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
		 *  //FIXME la lista degli anni andrebbe presa in funzione della persona selezionata 
		 *  // e della action richiesta, non in funzione del primo office allowed (??).
		 */
		List<Integer> years = Lists.newArrayList();
		Integer actualYear = new LocalDate().getYear();

		Optional<ConfGeneral> yearInitUseProgram = confGeneralDao.getByFieldName(Parameter.INIT_USE_PROGRAM.description,
				officeDao.getOfficeAllowed(user.get()).iterator().next());

		Integer yearBeginProgram;
		if(yearInitUseProgram.isPresent()){
			yearBeginProgram = new Integer(yearInitUseProgram.get().fieldValue.substring(0, 4));
		}
		else{
			yearBeginProgram = new LocalDate().getYear();
		}

		Logger.trace("injectMenu -> yearBeginProgram = %s", yearBeginProgram);

		while(yearBeginProgram <= actualYear+1){
			years.add(yearBeginProgram);
			Logger.trace("injectMenu -> aggiunto %s alla lista", yearBeginProgram);
			yearBeginProgram++;
		}

		renderArgs.put("navYears", years);


	}

	private static String computeActionSelected(String action) {


		if( action.startsWith("Stampings.")) {

			if(action.equals("Stampings.stampings")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "Stampings.stampings";
			}

			if(action.equals("Stampings.personStamping")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Stampings.personStamping";
			}

			if(action.equals("Stampings.missingStamping")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Stampings.missingStamping";
			}
			
			if(action.equals("Stampings.holidaySituation")) {
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Stampings.holidaySituation";
			}

			if(action.equals("Stampings.dailyPresence")) {

				renderArgs.put("switchDay", true);
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Stampings.dailyPresence";
			}
			
			if(action.equals("Stampings.dailyPresenceForPersonInCharge")) {

				renderArgs.put("switchDay", true);
				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				return "Stampings.dailyPresenceForPersonInCharge";
			}
			
			if(action.equals("Stampings.mealTicketSituation")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Stampings.mealTicketSituation";
			}

		}

		if( action.startsWith("PersonMonths.")) {

			if(action.equals("PersonMonths.trainingHours")) {


				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "PersonMonths.trainingHours";
			}

			if(action.equals("PersonMonths.hourRecap")) {

				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "PersonMonths.hourRecap";
			}
		}

		if( action.startsWith("Vacations.")) {

			if(action.equals("Vacations.show")) {

				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "Vacations.show";
			}
		}

		if( action.startsWith("Persons.")) {

			if(action.equals("Persons.changePassword")) {

				renderArgs.put("dropDown", "dropDownEmployee");
				return "Persons.changePassword";
			}
			if(action.equals("Persons.list")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "Persons.list";
			}

			if(action.equals("Persons.edit")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "Persons.edit";
			}
		}

		if(action.startsWith("Absences.")) {

			if(action.equals("Absences.absences")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "Absences.absences";
			}

			if(action.equals("Absences.manageAttachmentsPerCode")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Absences.manageAttachmentsPerCode";
			}

			if(action.equals("Absences.manageAttachmentsPerPerson")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Absences.manageAttachmentsPerPerson";
			}

			if(action.equals("Absences.absenceInPeriod")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "Absences.absenceInPeriod";
			}
		}

		if(action.startsWith("YearlyAbsences.")) {

			if(action.equals("YearlyAbsences.absencesPerPerson")) {

				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "YearlyAbsences.absencesPerPerson";
			}

			if(action.equals("YearlyAbsences.showGeneralMonthlyAbsences")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "YearlyAbsences.showGeneralMonthlyAbsences";
			}

			if(action.equals("YearlyAbsences.yearlyAbsences")) {

				renderArgs.put("switchYear",  true);
				renderArgs.put("switchPerson", true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "YearlyAbsences.yearlyAbsences";
			}
		}

		if(action.startsWith("Competences.")) {

			if(action.equals("Competences.competences")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownEmployee");
				return "Competences.competences";
			}

			if(action.equals("Competences.showCompetences")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Competences.showCompetences";
			}
			
			if(action.equals("Competences.monthlyOvertime")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				return "Competences.monthlyOvertime";
			}

			if(action.equals("Competences.totalOvertimeHours")) {

				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "Competences.totalOvertimeHours";
			}

			if(action.equals("Competences.enabledCompetences")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "Competences.enabledCompetences";
			}

			if(action.equals("Competences.exportCompetences")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "Competences.exportCompetences";
			}

		}

		if(action.startsWith("MonthRecaps.")) {

			if(action.equals("MonthRecaps.show")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "MonthRecaps.show";
			}
			
			if(action.equals("MonthRecaps.showRecaps")) {

				renderArgs.put("switchMonth",  true);
				renderArgs.put("switchYear",  true);
				renderArgs.put("dropDown", "dropDownAdministration");
				return "MonthRecaps.showRecaps";
			}
		}

		if(action.startsWith("UploadSituation.")) {

			if(action.equals("UploadSituation.show")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "UploadSituation.show";
			}

			if(action.equals("UploadSituation.loginAttestati")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "UploadSituation.loginAttestati";
			}

			if(action.equals("UploadSituation.processAttestati")) {

				renderArgs.put("dropDown", "dropDownAdministration");
				return "UploadSituation.processAttestati";
			}
		}

		if(action.startsWith("WorkingTimes.")) {

			if(action.equals("WorkingTimes.manageWorkingTime")) {

				renderArgs.put("dropDown", "dropDownConfiguration");
				return "WorkingTimes.manageWorkingTime";
			}
		}



		return session.get("actionSelected");
	}

}

