package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.PersonUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TotalOvertime;
import models.User;
import models.rendering.PersonMonthCompetenceRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import controllers.Resecure.NoCheck;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;

@With( {Resecure.class, RequestInit.class} )
public class Competences extends Controller{

	@Inject
	static SecurityRules rules;
	
	public static void competences(int year, int month) {

		//controllo dei parametri
		Optional<User> user = Security.getUser();
		if( ! user.isPresent() || user.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}

		PersonMonthCompetenceRecap personMonthCompetenceRecap = new PersonMonthCompetenceRecap(user.get().person, month, year);

		Person person = user.get().person;
		render(personMonthCompetenceRecap, person, year, month);

	}


	public static void showCompetences(Integer year, Integer month, Long officeId, String name, String codice, Integer page){

		//TODO selezionare gli uffici con viewCompetence
		List<Office> offices = Security.getOfficeAllowed();

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.get(0).id;
		}

		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		if(office == null) {

			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(office);


		//rules.checkIfPermitted("");
		if(page==null)
			page = 0;
				
		List<CompetenceCode> activeCompetenceCodes = PersonUtility.activeCompetence();
		
		CompetenceCode competenceCode = null;
		if(codice==null || codice=="")
		{
			competenceCode = activeCompetenceCodes.get(0);	//per adesso assumiamo che almeno una attiva ci sia (esempio S1)
		}
		else
		{
			for(CompetenceCode compCode : activeCompetenceCodes)
			{
				if(compCode.code.equals(codice))
					competenceCode = compCode;
			}
		}

		
		SimpleResults<Person> simpleResults = PersonDao.listForCompetence(competenceCode, Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());

		List<Person> activePersons = simpleResults.paginated(page).getResults();
		
		if(activePersons == null)
			activePersons = new ArrayList<Person>();
		
		//Redirect in caso di mese futuro
		LocalDate today = new LocalDate();
		if(today.getYear()==year && month>today.getMonthOfYear())
		{
			flash.error("Impossibile accedere a situazione futura, redirect automatico a mese attuale");
			month = today.getMonthOfYear();
		}

		
		for(Person p : activePersons){
			for(CompetenceCode c : p.competenceCode){
				Competence comp = CompetenceDao.getCompetence(p, year, month, c);
//				Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ?" +
//						"and comp.competenceCode = ?", p, month, year, c).first();
				if(comp == null){
					comp = new Competence(p, c, year, month);
					comp.valueApproved = 0;
					comp.save();
				}
					
			}
		}
		List<String> code = Lists.newArrayList();
		code.add("S1");
		code.add("S2");
		code.add("S3");
		List<Competence> competenceList = CompetenceDao.getCompetences(year, month, code, office, false);
//		List<Competence> competenceList = 
//				Competence.find("Select comp from Competence comp, CompetenceCode code where comp.year = ? and comp.month = ? " +
//				"and comp.competenceCode = code and code.code in (?,?,?) and comp.person.office = ?", 
//				year, month, "S1", "S2", "S3", office).fetch();
		
		int totaleOreStraordinarioMensile = 0;
		int totaleOreStraordinarioAnnuale = 0;
		int totaleMonteOre = 0;
		
		for(Competence comp : competenceList){
			
			totaleOreStraordinarioMensile = totaleOreStraordinarioMensile + comp.valueApproved;
		}
		
		List<Competence> competenceYearList = CompetenceDao.getCompetences(year, month, code, office, true);
//		List<Competence> competenceYearList = 
//				Competence.find("Select comp from Competence comp, CompetenceCode code where comp.year = ? and comp.month <= ? " +
//				"and comp.competenceCode = code and code.code in (?,?,?) and comp.person.office = ?", 
//				year, month, "S1", "S2", "S3", office).fetch();
		
		for(Competence comp : competenceYearList){
			
			totaleOreStraordinarioAnnuale = totaleOreStraordinarioAnnuale + comp.valueApproved;
		}
		
		List<TotalOvertime> total = CompetenceDao.getTotalOvertime(year, office);
		//List<TotalOvertime> total = TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ? and tot.office = ?", year, office).fetch();
		
		for(TotalOvertime tot : total){
			
			totaleMonteOre = totaleMonteOre+tot.numberOfHours;
		}
		
		render(year, month, office, offices, activePersons, totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, 
				totaleMonteOre, simpleResults, name, codice, activeCompetenceCodes, competenceCode);

	}
	

	public static void updateCompetence(long pk, String name, Integer value){
		final Competence competence = CompetenceDao.getCompetenceById(pk);
		//final Competence competence = Competence.findById(pk);
		
		notFoundIfNull(competence);
		if (validation.hasErrors()) {
			error(Messages.get(Joiner.on(",").join(validation.errors())));
		}
		rules.checkIfPermitted(competence.person.office);
		
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
	

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	@NoCheck
	public static void manageCompetenceCode(){
		rules.checkIfPermitted();
		List<CompetenceCode> compCodeList = CompetenceCodeDao.getAllCompetenceCode();
		//List<CompetenceCode> compCodeList = CompetenceCode.findAll();
		render(compCodeList);
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void insertCompetenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = new CompetenceCode();
		render(code);
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void edit(Long competenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = CompetenceCodeDao.getCompetenceCodeById(competenceCodeId);
		//CompetenceCode code = CompetenceCode.findById(competenceCodeId);
		render(code);
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void save(Long competenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		if(competenceCodeId == null){
			CompetenceCode code = new CompetenceCode();
			code.code = params.get("codice");
			code.codeToPresence = params.get("codiceAttPres");
			code.description = params.get("descrizione");
			//code.inactive = params.get("inattivo", Boolean.class) != null ? params.get("inattivo", Boolean.class) : false;
			
			CompetenceCode codeControl = CompetenceCodeDao.getCompetenceCodeByCode(params.get("codice"));
//			CompetenceCode codeControl = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", 
//					params.get("codice")).first();
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
			CompetenceCode code = CompetenceCodeDao.getCompetenceCodeById(competenceCodeId);
			//CompetenceCode code = CompetenceCode.findById(competenceCodeId);
			code.code = params.get("codice");
			code.codeToPresence = params.get("codiceAttPres");
			code.description = params.get("descrizione");
		//	code.inactive = params.get("inattivo", Boolean.class);
			code.save();
			flash.success(String.format("Codice %s aggiornato con successo", code.code));
			Application.indexAdmin();
		}
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void discard(){
		manageCompetenceCode();
	}

	
	
	public static void totalOvertimeHours(int year, Long officeId){
	
		//TODO selezionare gli uffici con viewCompetence
		List<Office> offices = Security.getOfficeAllowed();
		
		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.get(0).id;
		}
		
		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		if(office == null) {
			
			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			Application.indexAdmin();
		}
		
		rules.checkIfPermitted(office);
		
		List<TotalOvertime> totalList = CompetenceDao.getTotalOvertime(year, office);
//		List<TotalOvertime> totalList = 
//				TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ? and tot.office = ?",
//						year, office).fetch();

		int totale = 0;
		for(TotalOvertime tot : totalList) {
			
			totale = totale + tot.numberOfHours;
		}
		
		render(totalList, totale, year, office, offices);
	}

	public static void saveOvertime(Integer year, String numeroOre, Long officeId){

		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		if(office == null) {
			
			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			Application.indexAdmin();
		}
		
		rules.checkIfPermitted(office);
		
		TotalOvertime total = new TotalOvertime();
		LocalDate data = new LocalDate();
		total.date = data;
		total.year = data.getYear();
		total.office = office;

		try {
			if(numeroOre.startsWith("-")) {

				total.numberOfHours = - new Integer(numeroOre.substring(1, numeroOre.length()));
			}
			else if(numeroOre.startsWith("+")) {

				total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
			}
			else {
				
				flash.error("Inserire il segno (+) o (-) davanti al numero di ore da aggiungere (sottrarre)");
				Competences.totalOvertimeHours(year, officeId);
			}
		}
		catch (Exception e) {

			flash.error("Inserire il segno (+) o (-) davanti al numero di ore da aggiungere (sottrarre)");
			Competences.totalOvertimeHours(year, officeId);
		}
		
		total.save();
		flash.success(String.format("Aggiornato monte ore per l'anno %s", data.getYear()));
		Competences.totalOvertimeHours(year, officeId);
	}

	public static void overtime(int year, int month, Long officeId, String name, Integer page){
		
		//TODO selezionare gli uffici con viewCompetence
		List<Office> offices = Security.getOfficeAllowed();

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.get(0).id;
		}

		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		if(office == null) {

			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(office);
		
		if(page == null)
			page = 0;
		
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
		
		CompetenceCode code = CompetenceCodeDao.getCompetenceCodeByCode("S1");
		//CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
		SimpleResults<Person> simpleResults = PersonDao.listForCompetence(code, Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());

		List<Person> activePersons = simpleResults.paginated(page).getResults();
		
	
		for(Person p : activePersons){
			Integer daysAtWork = 0;
			Integer recoveryDays = 0;
			Integer timeAtWork = 0;
			Integer difference = 0;
			Integer overtime = 0;
			
			List<PersonDay> personDayList = PersonDayDao.getPersonDayInPeriod(p, beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()));
//			List<PersonDay> personDayList = PersonDay.find("Select pd from PersonDay pd where pd.date between ? and ? and pd.person = ?", 
//					beginMonth, beginMonth.dayOfMonth().withMaximumValue(), p).fetch();
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
	//		CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
			
			Competence comp = CompetenceDao.getCompetence(p, year, month, code);
//			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? " +
//					"and comp.year = ? and comp.month = ? and comp.competenceCode.code = ?", 
//					p, year, month, code.code).first();
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
			render(tableFeature, year, month, simpleResults, name, office, offices);
		else{
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			render(tableFeature,yearParams,monthParams,simpleResults, name, office, offices );
		}

	}

	/**
	 * funzione che ritorna la tabella contenente le competenze associate a ciascuna persona
	 */
	public static void enabledCompetences(Long officeId, String name){

		//TODO selezionare gli uffici con viewCompetence
		List<Office> offices = Security.getOfficeAllowed();

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.get(0).id;
		}

		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		if(office == null) {

			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(office);

		LocalDate date = new LocalDate();
		
		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, date, date.dayOfMonth().withMaximumValue(), true);
		
		//List<Person> personList = simpleResults.paginated(page).getResults();
		List<Person> personList = simpleResults.list();
		
		ImmutableTable.Builder<Person, String, Boolean> builder = ImmutableTable.builder();
		Table<Person, String, Boolean> tableRecapCompetence = null;
		
		List<CompetenceCode> allCodeList = CompetenceCodeDao.getAllCompetenceCode();
		//List<CompetenceCode> allCodeList = CompetenceCode.findAll();
		List<CompetenceCode> codeList = new ArrayList<CompetenceCode>();
		for(CompetenceCode compCode : allCodeList) {
			
			if( compCode.persons.size() > 0 )
				codeList.add(compCode);
			
		}
			
		for(Person p : personList) {

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
		render(tableRecapCompetence, month, year, office, offices, simpleResults, name);
	}

	/**
	 * 
	 * @param personId render della situazione delle competenze per la persona nella form updatePersonCompetence
	 */
	public static void updatePersonCompetence(Long personId){
		
		if(personId == null){
			
			flash.error("Persona inesistente");
			Application.indexAdmin();
		}
		
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		rules.checkIfPermitted(person.office);
		render(person);
	}

	/**
	 *  salva la nuova configurazione di competenze per la persona
	 */
	public static void saveNewCompetenceConfiguration(){
		long personId = params.get("personId", Long.class);
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente in anagrafica");
			Application.indexAdmin();
			
		}
		rules.checkIfPermitted(person.office);
		
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

		if(overtimeWorkDay.equals("true") && !person.isOvertimeInWorkDayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario diurno nei giorni lavorativi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni lavorativi").first();
			person.competenceCode.add(c);
		}
		else if(overtimeWorkDay.equals("false") && person.isOvertimeInWorkDayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario diurno nei giorni lavorativi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni lavorativi").first();
			person.competenceCode.remove(c);
		}

		if(nightlyOvertime.equals("true") && !person.isOvertimeInHolidayOrNightlyInWorkDayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi").first();
			person.competenceCode.add(c);
		}
		else if(nightlyOvertime.equals("false") && person.isOvertimeInHolidayOrNightlyInWorkDayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi").first();
			person.competenceCode.remove(c);
		}

		if(nightlyHolidayOvertime.equals("true") && !person.isOvertimeInNightlyHolidayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario notturno nei giorni festivi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario notturno nei giorni festivi").first();
			person.competenceCode.add(c);
		}
		else if(nightlyHolidayOvertime.equals("false") && person.isOvertimeInNightlyHolidayAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Straordinario notturno nei giorni festivi");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Straordinario notturno nei giorni festivi").first();
			person.competenceCode.remove(c);
		}

		if(workdayReperibility.equals("true") && !person.isWorkDayReperibilityAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Reper.ta' Feriale");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Feriale").first();
			person.competenceCode.add(c);
		}
		else if(workdayReperibility.equals("false") && person.isWorkDayReperibilityAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Reper.ta' Feriale");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Feriale").first();
			person.competenceCode.remove(c);
		}

		if(holidayReperebility.equals("true") && !person.isHolidayReperibilityAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Reper.ta' Festiva");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Festiva").first();
			person.competenceCode.add(c);
		}
		else if(holidayReperebility.equals("false") && person.isHolidayReperibilityAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Reper.ta' Festiva");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Reper.ta' Festiva").first();
			person.competenceCode.remove(c);
		}

		if(ordinaryShift.equals("true") && !person.isOrdinaryShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno ordinario");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno ordinario").first();
			person.competenceCode.add(c);
		}
		else if(ordinaryShift.equals("false") && person.isOrdinaryShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno ordinario");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno ordinario").first();
			person.competenceCode.remove(c);
		}

		if(holidayShift.equals("true") && !person.isHolidayShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno festivo");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno festivo").first();
			person.competenceCode.add(c);
		}
		else if(holidayShift.equals("false") && person.isHolidayShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno festivo");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno festivo").first();
			person.competenceCode.remove(c);
		}

		if(nightlyShift.equals("true") && !person.isNightlyShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno notturno");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno notturno").first();
			person.competenceCode.add(c);
		}
		else if(nightlyShift.equals("false") && person.isNightlyShiftAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Turno notturno");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Turno notturno").first();
			person.competenceCode.remove(c);
		}

		if(hardship.equals("true") && !person.isHardshipAllowance()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.tà Sede Disagiata");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.tà Sede Disagiata").first();
			person.competenceCode.add(c);
		}
		else if(hardship.equals("false") && person.isHardshipAllowance()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.tà Sede Disagiata");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.tà Sede Disagiata").first();
			person.competenceCode.remove(c);
		}

		if(handleValues.equals("true") && !person.isHandleValuesAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Maneggio Valori");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Maneggio Valori").first();
			person.competenceCode.add(c);
		}
		else if(handleValues.equals("false") && person.isHandleValuesAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Maneggio Valori");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Maneggio Valori").first();
			person.competenceCode.remove(c);
		}

		if(task.equals("true") && !person.isTaskAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' mansione L.397/71");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71").first();
			person.competenceCode.add(c);
		}
		else if(task.equals("false") && person.isTaskAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' mansione L.397/71");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71").first();
			person.competenceCode.remove(c);
		}

		if(taskIncreased.equals("true") && !person.isTaskAllowanceIncreasedAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' mansione L.397/71 Magg.");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71 Magg.").first();
			person.competenceCode.add(c);
		}
		else if(taskIncreased.equals("false") && person.isTaskAllowanceIncreasedAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' mansione L.397/71 Magg.");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' mansione L.397/71 Magg.").first();
			person.competenceCode.remove(c);
		}

		if(boats.equals("true") && !person.isBoatsAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Natanti");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Natanti").first();
			person.competenceCode.add(c);
		}
		else if(boats.equals("false") && person.isBoatsAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Natanti");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Natanti").first();
			person.competenceCode.remove(c);
		}

		if(riskOne.equals("true") && !person.isRiskDegreeOneAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.1 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.1 DPR.146").first();
			person.competenceCode.add(c);
		}
		else if(riskOne.equals("false") && person.isRiskDegreeOneAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.1 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.1 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskTwo.equals("true") && !person.isRiskDegreeTwoAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.2 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.2 DPR.146").first();
			person.competenceCode.add(c);
		}
		else if(riskTwo.equals("false") && person.isRiskDegreeTwoAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.2 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.2 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskThree.equals("true") && !person.isRiskDegreeThreeAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.3 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.3 DPR.146").first();
			person.competenceCode.add(c);
		}
		else if(riskThree.equals("false") && person.isRiskDegreeThreeAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.3 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.3 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskFour.equals("true") && !person.isRiskDegreeFourAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.4 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.4 DPR.146").first();
			person.competenceCode.add(c);
		}
		else if(riskFour.equals("false") && person.isRiskDegreeFourAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.4 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.4 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskFive.equals("true") && !person.isRiskDegreeFiveAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.5 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.5 DPR.146").first();
			person.competenceCode.add(c);
		}
		else if(riskFive.equals("false") && person.isRiskDegreeFiveAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio GR.5 DPR.146");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio GR.5 DPR.146").first();
			person.competenceCode.remove(c);
		}

		if(riskDiving.equals("true") && !person.isRiskDivingAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio Subacquei");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio Subacquei").first();
			person.competenceCode.add(c);
		}
		else if(riskDiving.equals("false") && person.isRiskDivingAllowanceAvailable()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByDescription("Ind.ta' Rischio Subacquei");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.description = ? ", "Ind.ta' Rischio Subacquei").first();
			person.competenceCode.remove(c);
		}


		if(ionicRadiance1.equals("true") && !person.isIonicRadianceRiskCom1Available()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("205");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "205").first();
			person.competenceCode.add(c);
		}
		else if(ionicRadiance1.equals("false") && person.isIonicRadianceRiskCom1Available()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("205");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "205").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance3.equals("true") && !person.isIonicRadianceRiskCom3Available()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("206");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "206").first();
			person.competenceCode.add(c);
		}
		else if(ionicRadiance3.equals("false") && person.isIonicRadianceRiskCom3Available()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("206");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "206").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance1bis.equals("true") && !person.isIonicRadianceRiskCom1AvailableBis()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("303");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "303").first();
			person.competenceCode.add(c);
		}
		else if(ionicRadiance1bis.equals("false") && person.isIonicRadianceRiskCom1AvailableBis()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("303");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "303").first();
			person.competenceCode.remove(c);
		}

		if(ionicRadiance3bis.equals("true") && !person.isIonicRadianceRiskCom3AvailableBis()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("304");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "304").first();
			person.competenceCode.add(c);
		}
		else if(ionicRadiance3bis.equals("false") && person.isIonicRadianceRiskCom3AvailableBis()){
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeByCode("304");
			//CompetenceCode c = CompetenceCode.find("Select c from CompetenceCode c where c.code = ? ", "304").first();
			person.competenceCode.remove(c);
		}
		person.save();
		flash.success(String.format("Aggiornate con successo le competenze per %s %s", person.name, person.surname));
		Competences.enabledCompetences( person.office.id, null);

	}
	
	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void exportCompetences(){
		rules.checkIfPermitted("");
		render();
	}
	
	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void getOvertimeInYear(int year) throws IOException{
		
		rules.checkIfPermitted("");
		List<Person> personList = Person.getActivePersonsinYear(year, Security.getOfficeAllowed(), true);
		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Cognome Nome,Totale straordinari"+' '+year);
		out.newLine();
		List<CompetenceCode> codeList = Lists.newArrayList();
		codeList.add(CompetenceCodeDao.getCompetenceCodeByCode("S1"));
		for(Person p : personList){
			Long totale = CompetenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.<Integer>absent(), Optional.fromNullable(p), codeList).longValue();
//			Long totale = Competence.find("Select sum(comp.valueApproved) from Competence comp, CompetenceCode code " +
//					"where comp.person = ?" +
//					"and comp.year = ? " +
//					"and comp.competenceCode = code " +
//					"and code.code = ? ", p, year, "S1").first();			
			Logger.debug("Totale per %s %s vale %d", p.name, p.surname, totale);
			out.write(p.surname+' '+p.name+',');
			if(totale != null)			
				out.append(totale.toString());
			else
				out.append("0");
			out.newLine();
		}
		out.close();
		renderBinary(inputStream, "straordinari"+year+".csv");
	}
	

}
