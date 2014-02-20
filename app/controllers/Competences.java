package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Query;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Permission;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.TotalOvertime;
import models.rendering.PersonMonthCompetenceRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.IsTrue;
import play.data.validation.Min;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

@With( {Secure.class, NavigationMenu.class} )
public class Competences extends Controller{

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void competences(Long personId, int year, int month) {
		Person person = null;
		if(personId != null)
			person = Person.findById(personId); //Security.getPerson();
		else
			person = Security.getPerson();

		PersonMonthCompetenceRecap personMonthCompetenceRecap = new PersonMonthCompetenceRecap(person, month, year);
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		
		render(personMonthCompetenceRecap, person, month_capitalized);

	}


	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void showCompetences(Integer year, Integer month){

		
		List<Person> activePersons = null;
		if((year == null || month == null) || (year == 0 || month == 0)){
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			activePersons = Person.getTechnicianForCompetences(new LocalDate(yearParams, monthParams,1));
		}
		else{
			activePersons = Person.getTechnicianForCompetences(new LocalDate(year, month, 1));
		}

		List<CompetenceCode> competenceCodes = PersonUtility.activeCompetence();
		for(Person p : activePersons){
			for(CompetenceCode c : p.competenceCode){
				Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ?" +
						"and comp.competenceCode = ?", p, month, year, c).first();
				if(comp == null){
					comp = new Competence(p, c, year, month);
					comp.valueApproved = 0;
					comp.save();
				}
					
			}
			
		}
		
		List<Competence> competenceList = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.year = ? and comp.month = ? " +
				"and comp.competenceCode = code and code.code in (?,?,?)", 
				year, month, "S1", "S2", "S3").fetch();
		int totaleOreStraordinarioMensile = 0;
		int totaleOreStraordinarioAnnuale = 0;
		int totaleMonteOre = 0;
		for(Competence comp : competenceList){
			totaleOreStraordinarioMensile = totaleOreStraordinarioMensile + comp.valueApproved;
		}
		List<Competence> competenceYearList = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.year = ? and comp.month <= ? " +
				"and comp.competenceCode = code and code.code in (?,?,?)", 
				year, month, "S1", "S2", "S3").fetch();
		for(Competence comp : competenceYearList){
			totaleOreStraordinarioAnnuale = totaleOreStraordinarioAnnuale + comp.valueApproved;
		}
		List<TotalOvertime> total = TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ?", year).fetch();
		for(TotalOvertime tot : total){
			totaleMonteOre = totaleMonteOre+tot.numberOfHours;
		}
		
		render(year, month, activePersons, competenceCodes, totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, totaleMonteOre);

	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void updateCompetence(long pk, String name, Integer value){
		final Competence competence = Competence.findById(pk);
		notFoundIfNull(competence);
		if (validation.hasErrors()) {
			error(Messages.get(Joiner.on(",").join(validation.errors())));
		}
		Logger.info("Anno competenza: %s Mese competenza: %s", competence.year, competence.month);
		Logger.info("value approved before = %s", competence.valueApproved);
		competence.valueApproved = value;
		Logger.info("saved id=%s (person=%s) code=%s (value=%s)", competence.id, competence.person, 
				competence.competenceCode.code, competence.valueApproved);
		competence.save();
		renderText("ok");
	}
	
	public static void prova(){
		render();
	}
	

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void manageCompetenceCode(){
		List<CompetenceCode> compCodeList = CompetenceCode.findAll();
		render(compCodeList);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void insertCompetenceCode(){
		CompetenceCode code = new CompetenceCode();
		render(code);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void edit(Long competenceCodeId){
		CompetenceCode code = CompetenceCode.findById(competenceCodeId);
		render(code);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void save(Long competenceCodeId){
		if(competenceCodeId == null){
			CompetenceCode code = new CompetenceCode();
			code.code = params.get("codice");
			code.codeToPresence = params.get("codiceAttPres");
			code.description = params.get("descrizione");
			code.inactive = params.get("inattivo", Boolean.class) != null ? params.get("inattivo", Boolean.class) : false;
			CompetenceCode codeControl = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", 
					params.get("codice")).first();
			if(codeControl == null){
				code.save();
				flash.success(String.format("Codice %s aggiunto con successo", code.code));
				Application.indexAdmin();
			}
			else{
				flash.error(String.format("Il codice competenza %s è già presente nel database. Cambiare nome al codice.", params.get("codice")));
				Application.indexAdmin();
			}

		}
		else{
			CompetenceCode code = CompetenceCode.findById(competenceCodeId);
			code.code = params.get("codice");
			code.codeToPresence = params.get("codiceAttPres");
			code.description = params.get("descrizione");
			code.inactive = params.get("inattivo", Boolean.class);
			code.save();
			flash.success(String.format("Codice %s aggiornato con successo", code.code));
			Application.indexAdmin();
		}
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void discard(){
		manageCompetenceCode();
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void totalOvertimeHours(int year, int month){
		List<TotalOvertime> totalList = TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ?", year).fetch();

		int totale = 0;
		for(TotalOvertime tot : totalList) {
			totale = totale+tot.numberOfHours;
		}

		Logger.debug("la lista di monte ore per l'anno %s è %s", year, totalList);
		render(totalList, totale, year, month);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void saveOvertime(int year, int month){
		TotalOvertime total = new TotalOvertime();
		LocalDate data = new LocalDate();
		total.date = data;
		total.year = data.getYear();

		String numeroOre = params.get("numeroOre");
		if(numeroOre.startsWith("-")){

			total.numberOfHours = - new Integer(numeroOre.substring(1, numeroOre.length()));

		}
		else if(numeroOre.startsWith("+")){

			total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
		}
		else {
			flash.error(String.format("Format unexpected"));
			Competences.totalOvertimeHours(year, month);
		}
		total.save();
		flash.success(String.format("Aggiornato monte ore per l'anno %s", data.getYear()));
		Competences.totalOvertimeHours(year, month);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtime(int year, int month){
		
		ImmutableTable.Builder<Person, String, Integer> builder = ImmutableTable.builder();
		Table<Person, String, Integer> tableFeature = null;
		LocalDate beginMonth = null;
		if(year == 0 && month == 0){
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			beginMonth = new LocalDate(yearParams, monthParams, 1);
		}
		else{
			beginMonth = new LocalDate(year, month, 1);
		}
		
		List<Person> activePersons = Person.getTechnicianForCompetences(new LocalDate(year, month, 1));
		for(Person p : activePersons){
			Integer daysAtWork = 0;
			Integer recoveryDays = 0;
			Integer timeAtWork = 0;
			Integer difference = 0;
			Integer overtime = 0;
			List<PersonDay> personDayList = PersonDay.find("Select pd from PersonDay pd where pd.date between ? and ? and pd.person = ?", 
					beginMonth, beginMonth.dayOfMonth().withMaximumValue(), p).fetch();
			for(PersonDay pd : personDayList){
				if(pd.stampings.size()>0)
					daysAtWork = daysAtWork +1;
				timeAtWork = timeAtWork + pd.timeAtWork;
				difference = difference +pd.difference;
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals("94"))
						recoveryDays = recoveryDays+1;
				}

			}
			CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
			
			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? " +
					"and comp.year = ? and comp.month = ? and comp.competenceCode.code = ?", 
					p, year, month, code.code).first();
			if(comp != null)
				overtime = comp.valueApproved;
			else
				overtime = 0;
			builder.put(p, "Giorni di Presenza", daysAtWork);
			builder.put(p, "Tempo Lavorato (HH:MM)", timeAtWork);
			builder.put(p, "Tempo di lavoro in eccesso (HH:MM)", difference);
			builder.put(p, "Residuo - rip. compensativi", difference-(recoveryDays*60));
			builder.put(p, "Residuo netto", difference-(overtime*60));
			builder.put(p, "Ore straordinario pagate", overtime);
			builder.put(p, "Riposi compens.", recoveryDays);
						
		}
		tableFeature = builder.build();
		if(year != 0 && month != 0)
			render(tableFeature, year, month);
		else{
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			render(tableFeature,yearParams,monthParams );
		}

	}

//	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
//	public static void updateOldOvertime(Long personId){
//		LocalDate date = new LocalDate().minusMonths(1);
//		Logger.debug("La data è: ", date);
//		Person person = Person.findById(personId);
//		int weekDayAvailability;
//		int holidaysAvailability;
//		int daylightWorkingDaysOvertime;
//		CompetenceCode cmpCode1 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
//		CompetenceCode cmpCode2 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
//		CompetenceCode cmpCode3 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
//		Logger.debug("Anno e mese: %s %s", date.getYear(), date.getMonthOfYear());
//		Competence comp1 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
//				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode1).first();
//		Competence comp2 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
//				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode2).first();
//		Competence comp3 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
//				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode3).first();
//		if(comp1 != null)
//			weekDayAvailability = comp1.valueApproved;
//		else
//			weekDayAvailability = 0;
//		if(comp2 != null)
//			holidaysAvailability = comp2.valueApproved;
//		else
//			holidaysAvailability = 0;
//		if(comp3 != null)
//			daylightWorkingDaysOvertime = comp3.valueApproved;
//		else
//			daylightWorkingDaysOvertime = 0;
//		int progressive = 0;
//		progressive = PersonUtility.getPositiveDaysForOvertime(PersonMonth.getInstance(person, date.getYear(), date.getMonthOfYear()));
//		//		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
//		//				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
//		//		if(lastPreviousPersonDayInMonth != null)
//		progressive = progressive /60;
//		render(weekDayAvailability,holidaysAvailability,daylightWorkingDaysOvertime, progressive, date, person);
//	}
//
//	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
//	public static void saveOldOvertime(){
//		long personId = params.get("person.id", Long.class);
//		Person person = Person.findById(personId);
//		int year = params.get("year", Integer.class);
//		int month = params.get("month", Integer.class);
//		int overtime = params.get("straordinari", Integer.class);
//		int progressive = params.get("progressive", Integer.class);
//		if(overtime > progressive){
//			flash.error(String.format("Impossibile assegnare ore di straordinario."));
//			render("@save");
//		}
//		else{
//			if(PersonUtility.canTakeOvertime(person, year, month)){
//				Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ?", 
//						person, month, year).first();
//				if(comp == null){
//					comp = new Competence();
//					comp.month = month;
//					comp.person = person;
//					comp.year = year;
//					comp.competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
//					comp.valueApproved = overtime;
//
//				}
//				else{
//					comp.valueApproved = overtime;
//				}
//				comp.save();
//				flash.success(String.format("Inserite %s ore di straordinario per %s %s il %s/%s", overtime, person.name, person.surname, month, year));
//				render("@save");
//
//			}
//			else{
//				flash.error(String.format("Impossibile assegnare ore di straordinario causa residuo mese precedente insufficiente a coprire " +
//						"le ore in negativo fatte in alcuni giorni di questo mese"));
//				render("@save");
//			}
//		}
//
//	}

	/**
	 * funzione che ritorna la tabella contenente le competenze associate a ciascuna persona
	 */
	public static void recapCompetences(){
		LocalDate date = new LocalDate();
		List<Person> personList = Person.getTechnicianForCompetences(date);
		ImmutableTable.Builder<Person, String, Boolean> builder = ImmutableTable.builder();
		Table<Person, String, Boolean> tableRecapCompetence = null;
		List<CompetenceCode> codeList = CompetenceCode.findAll();
		for(Person p : personList){

			for(CompetenceCode comp : codeList){
				if(p.competenceCode.contains(comp)){
					builder.put(p, comp.description+'\n'+comp.code, true);
				}
				else{
					builder.put(p, comp.description+'\n'+comp.code, false);
				}

			}

		}
		tableRecapCompetence = builder.build();
		int month = date.getMonthOfYear();
		int year = date.getYear();
		render(tableRecapCompetence, month, year);
	}

	/**
	 * 
	 * @param personId render della situazione delle competenze per la persona nella form updatePersonCompetence
	 */
	public static void updatePersonCompetence(Long personId){
		if(personId != null){
			Person person = Person.findById(personId);
			render(person);
		}
	}

	/**
	 *  salva la nuova configurazione di competenze per la persona
	 */
	public static void saveNewCompetenceConfiguration(){
		long personId = params.get("personId", Long.class);
		Person person = Person.findById(personId);
		String overtimeWorkDay = params.get("overtimeWorkDay");
		String nightlyOvertime = params.get("nightlyOvertime");
		String nightlyHolidayOvertime = params.get("nightlyHolidayOvertime");
		String workdayReperibility = params.get("workdayReperibility");
		String holidayReperebility = params.get("holidayReperebility");
		String ordinaryShift = params.get("ordinaryShift");
		String holidayShift = params.get("holidayShift");
		String nightlyShift = params.get("nightlyShift");
		String hardship = params.get("hardship");
		String handleValues = params.get("handleValues");
		String task = params.get("task");
		String taskIncreased = params.get("taskIncreased");
		String boats = params.get("boats");
		String riskOne = params.get("riskOne");
		String riskTwo = params.get("riskTwo");
		String riskThree = params.get("riskThree");
		String riskFour = params.get("riskFour");
		String riskFive = params.get("riskFive");
		String riskDiving = params.get("riskDiving");
		String ionicRadiance1 = params.get("ionicRadiance1");
		String ionicRadiance3 = params.get("ionicRadiance3");
		String ionicRadiance1bis = params.get("ionicRadiance1bis");
		String ionicRadiance3bis = params.get("ionicRadiance3bis");

		if(overtimeWorkDay.equals("true") && person.isOvertimeInWorkDayAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni lavorativi").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni lavorativi").first();
			person.competenceCode.remove(c);
		}

		if(nightlyOvertime.equals("true") && !person.isOvertimeInHolidayOrNightlyInWorkDayAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi").first();
			person.competenceCode.remove(c);
		}

		if(nightlyHolidayOvertime.equals("true") && !person.isOvertimeInNightlyHolidayAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario notturno nei giorni festivi").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario notturno nei giorni festivi").first();
			person.competenceCode.remove(c);
		}

		if(workdayReperibility.equals("true") && !person.isWorkDayReperibilityAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Feriale").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Feriale").first();
			person.competenceCode.remove(c);
		}

		if(holidayReperebility.equals("true") && !person.isHolidayReperibilityAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Festiva").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Festiva").first();
			person.competenceCode.remove(c);
		}

		if(ordinaryShift.equals("true") && !person.isOrdinaryShiftAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno ordinario").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno ordinario").first();
			person.competenceCode.remove(c);
		}

		if(holidayShift.equals("true") && !person.isHolidayShiftAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno festivo").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno festivo").first();
			person.competenceCode.remove(c);
		}

		if(nightlyShift.equals("true") && !person.isNightlyShiftAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno notturno").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno notturno").first();
			person.competenceCode.remove(c);
		}

		if(hardship.equals("true") && !person.isHardshipAllowance()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.tà Sede Disagiata").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.tà Sede Disagiata").first();
			person.competenceCode.remove(c);
		}

		if(handleValues.equals("true") && !person.isHandleValuesAllowanceAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Maneggio Valori").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Maneggio Valori").first();
			person.competenceCode.remove(c);
		}

		if(task.equals("true") && !person.isTaskAllowanceAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71").first();
			person.competenceCode.remove(c);
		}

		if(taskIncreased.equals("true") && !person.isTaskAllowanceIncreasedAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71 Magg.").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71 Magg.").first();
			person.competenceCode.remove(c);
		}

		if(boats.equals("true") && !person.isBoatsAllowanceAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Natanti").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Natanti").first();
			person.competenceCode.remove(c);
		}

		if(riskOne.equals("true") && !person.isRiskDegreeOneAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.1 DPR.146").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.1 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskTwo.equals("true") && !person.isRiskDegreeTwoAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.2 DPR.146").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.2 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskThree.equals("true") && !person.isRiskDegreeThreeAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.3 DPR.146").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.3 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskFour.equals("true") && !person.isRiskDegreeFourAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.4 DPR.146").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.4 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskFive.equals("true") && !person.isRiskDegreeFiveAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.5 DPR.146").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.5 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskDiving.equals("true") && !person.isRiskDivingAllowanceAvailable()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio Subacquei").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio Subacquei").first();
			person.competenceCode.remove(c);
		}


		if(ionicRadiance1.equals("true") && !person.isIonicRadianceRiskCom1Available()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "205").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "205").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance3.equals("true") && !person.isIonicRadianceRiskCom3Available()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "206").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "206").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance1bis.equals("true") && !person.isIonicRadianceRiskCom1AvailableBis()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "303").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "303").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance3bis.equals("true") && !person.isIonicRadianceRiskCom3AvailableBis()){
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "304").first();
			person.competenceCode.add(c);
		}
		else{
			CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "304").first();
			person.competenceCode.remove(c);
		}
		person.save();
		flash.success(String.format("Aggiornate con successo le competenze per %s %s", person.name, person.surname));
		Competences.recapCompetences();

	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void exportCompetences(){
		render();
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void getOvertimeInYear(int year) throws IOException{
		
		List<Person> personList = Person.getActivePersonsinYear(year, true);
		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Cognome Nome,Totale straordinari"+' '+year);
		out.newLine();
		for(Person p : personList){
			Long totale = Competence.find("Select sum(comp.valueApproved) from Competence comp, CompetenceCode code " +
					"where comp.person = ?" +
					"and comp.year = ? " +
					"and comp.competenceCode = code " +
					"and code.code = ? ", p, year, "S1").first();			
			Logger.debug("Totale per %s %s vale %d", p.name, p.surname, totale);
			out.write(p.surname+' '+p.name+',');
						
			out.append(totale.toString());
			out.newLine();
		}
		out.close();
		renderBinary(inputStream, "straordinari"+year+".csv");
	}
	

}
