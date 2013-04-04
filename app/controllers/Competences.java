package controllers;

import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.TotalOvertime;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

@With( {Secure.class, NavigationMenu.class} )
public class Competences extends Controller{

	/* corrisponde alla voce di menu selezionata */
	//	private final static ActionMenuItem actionMenuItem = ActionMenuItem.competences;
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void competences(Long personId, int year, int month) {
		Person person = null;
		if(personId != null)
			person = Person.findById(personId); //Security.getPerson();
		else
			person = Security.getPerson();
		
		Logger.trace("Year: {}, month: {}", year, month);

		String anno = params.get("year");
		String mese= params.get("month");

		if(anno==null || mese==null){

			LocalDate now = new LocalDate();
			PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());

			render(personMonth);
		}
		else{
			Logger.info("Sono dentro il ramo else della creazione del month recap");
			PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, year, month);

			render(personMonth);

		}

	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void showCompetences(Integer year, Integer month){

		Table<Person, String, Integer> tablePersonCompetences =  HashBasedTable.create();
		List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, 1));
		for(Person p : activePersons){
			List<Competence> competenceInMonth = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ?" +
					"and comp.month = ?", p, year, month).fetch();
			Logger.debug("Dimensione competencenInMonth: %d", competenceInMonth.size());
			tablePersonCompetences.put(p, "Totale", competenceInMonth.size());
			for(Competence comp : competenceInMonth){
				Integer value = tablePersonCompetences.row(p).get(comp.competenceCode.description);
				Logger.debug("Per la persona %s il codice %s vale: %s", p, comp.competenceCode.description, value);
				if(value == null){
					Logger.debug("Non ci sono competenze per %s %s relativamente a %s. Quindi inserisco 0 in tabella", p.surname, p.name, comp.competenceCode.description);
					
					tablePersonCompetences.row(p).put(comp.competenceCode.description, 0);
				}
				else{
					tablePersonCompetences.row(p).put(comp.competenceCode.description, value);
					Logger.debug("Inserisco in tabella nuova competenza per %s con codice %s", p, comp.competenceCode.description);

				}
			}
		}
		int numberOfDifferentCompetenceType = tablePersonCompetences.columnKeySet().size();
		render(tablePersonCompetences, year, month, numberOfDifferentCompetenceType);

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
			code.inactive = params.get("inattivo", Boolean.class);
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
		List<TotalOvertime> total = TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ?", year).fetch();
		Logger.debug("la lista di monte ore per l'anno %s è %s", year, total);
		render(total, year, month);
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
		if(numeroOre.startsWith("+")){

			total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
		}
		total.save();
		flash.success(String.format("Aggiornato monte ore per l'anno %s", data.getYear()));
		Application.indexAdmin();
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtime(int year, int month){
		Map<Person, List<Object>> mapPersonFeatures = new HashMap<Person, List<Object>>();
		List<Object> lista = null;
		LocalDate beginMonth = new LocalDate(year, month, 1);
		List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, 1));
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
			//Logger.debug("Sto per caricare il valore degli straordinari per %s %s con id %s", p.surname, p.name, p.id);
			CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
			//Logger.debug("Il codice per la ricerca è: %s", code.code);

			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? " +
					"and comp.year = ? and comp.month = ? and comp.competenceCode.code = ?", 
					p, year, month, code.code).first();
			//Logger.debug("La competenza è: %s", comp);
			if(comp != null)
				overtime = comp.valueApproved;
			else
				overtime = 0;
			lista = new ArrayList<Object>();
			lista.add(daysAtWork);  //posizione 0 della lista
			lista.add(timeAtWork);  //posizione 1 della lista
			lista.add(difference);  //posizione 2 della lista
			lista.add(difference-(overtime*60)); //posizione 3 della lista
			lista.add(overtime);    //posizione 4 della lista
			lista.add(recoveryDays);//posizione 5 della lista
			mapPersonFeatures.put(p, lista);

		}

		render(mapPersonFeatures, year, month);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void updateOldOvertime(Long personId){
		LocalDate date = new LocalDate().minusMonths(1);
		Logger.debug("La data è: ", date);
		Person person = Person.findById(personId);
		int weekDayAvailability;
		int holidaysAvailability;
		int daylightWorkingDaysOvertime;
		CompetenceCode cmpCode1 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
		CompetenceCode cmpCode2 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
		CompetenceCode cmpCode3 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
		Logger.debug("Anno e mese: %s %s", date.getYear(), date.getMonthOfYear());
		Competence comp1 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode1).first();
		Competence comp2 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode2).first();
		Competence comp3 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode3).first();
		if(comp1 != null)
			weekDayAvailability = comp1.valueApproved;
		else
			weekDayAvailability = 0;
		if(comp2 != null)
			holidaysAvailability = comp2.valueApproved;
		else
			holidaysAvailability = 0;
		if(comp3 != null)
			daylightWorkingDaysOvertime = comp3.valueApproved;
		else
			daylightWorkingDaysOvertime = 0;
		int progressive = 0;
		progressive = PersonUtility.getPositiveDaysForOvertime(PersonMonth.getInstance(person, date.getYear(), date.getMonthOfYear()));
		//		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
		//				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
		//		if(lastPreviousPersonDayInMonth != null)
		progressive = progressive /60;
		render(weekDayAvailability,holidaysAvailability,daylightWorkingDaysOvertime, progressive, date, person);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void saveOldOvertime(){
		long personId = params.get("person.id", Long.class);
		Person person = Person.findById(personId);
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		int overtime = params.get("straordinari", Integer.class);
		int progressive = params.get("progressive", Integer.class);
		if(overtime > progressive){
			flash.error(String.format("Impossibile assegnare ore di straordinario."));
			Persons.list();
		}
		else{
			if(PersonUtility.canTakeOvertime(person, year, month)){
				Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? and comp.year = ?", 
						person, month, year).first();
				if(comp == null){
					comp = new Competence();
					comp.month = month;
					comp.year = year;
					comp.competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
					comp.valueApproved = overtime;

				}
				else{
					comp.valueApproved = progressive;
				}
				comp.save();
				flash.success(String.format("Inserite %s ore di straordinario per %s %s il %s/%s", overtime, person.name, person.surname, month, year));
				Application.indexAdmin();

			}
			else{
				flash.error(String.format("Impossibile assegnare ore di straordinario causa residuo mese precedente insufficiente a coprire " +
						"le ore in negativo fatte in alcuni giorni di questo mese"));
				Application.indexAdmin();
			}
		}

	}

}
