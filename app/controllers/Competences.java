package controllers;

import java.util.ArrayList;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Competence;
import models.CompetenceCode;
import models.MonthRecap;
import models.Person;
import models.TotalOvertime;

import org.joda.time.LocalDate;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Competences extends Controller{

	/* corrisponde alla voce di menu selezionata */
//	private final static ActionMenuItem actionMenuItem = ActionMenuItem.competences;
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	private static void show(Person person) {
//		String menuItem = actionMenuItem.toString();
		
    	String anno = params.get("year");
    	Logger.info("Anno: "+anno.toString());
    	String mese= params.get("month");
    	Logger.info("Mese: "+mese.toString());
    	if(anno==null || mese==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap/*, menuItem*/);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.info("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap/*, menuItem*/);
    	}
    	
    }
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void show() {
    	show(Security.getPerson());
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
					Logger.debug("Inserisco in tabella nuova assenza per %s con codice %s", p, comp.competenceCode.description);
					tablePersonCompetences.row(p).put(comp.competenceCode.description, 1);
				}
				else{
					tablePersonCompetences.row(p).put(comp.competenceCode.description, value+1);
					Logger.debug("Incremento il numero di giorni per l'assenza %s di %s al valore %s", comp.competenceCode.description, p, value+1);
					
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
	public static void totalOvertimeHours(int year){
		List<TotalOvertime> total = TotalOvertime.find("Select tot from TotalOvertime tot where tot.year = ?", year).fetch();
		render(total);
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void saveOvertime(int year){
		TotalOvertime total = new TotalOvertime();
		total.date = new LocalDate();
		total.year = new LocalDate().getYear();
		String numeroOre = params.get("numeroOre");
		if(numeroOre.startsWith("-")){
			total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length())) * (-1);
		}
		if(numeroOre.startsWith("+")){
			total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
		}
		total.save();
		flash.success(String.format("Aggiornato monte ore per l'anno", year));
		Application.indexAdmin();
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtime(){
		
	}
	
}
