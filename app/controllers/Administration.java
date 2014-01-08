package controllers;

import it.cnr.iit.epas.ExportToYaml;
import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PersonUtility;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.yaml.snakeyaml.Yaml;

import controllers.shib.Shibboleth;
import models.AbsenceType;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonth;
import models.WorkingTimeType;
import models.exports.PersonsList;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import procedure.evolutions.Evolutions;


//@With(Shibboleth.class)
public class Administration extends Controller {
	
	
    public static void index() {
        render();
    }
        
    
    public static void importOreStraordinario() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
    	
    	FromMysqlToPostgres.importOreStraordinario();
    	renderText("Importati tutti i dati relativi ai monte ore straordinari");
    }
    
    public static void addPermissionToAll(){
    	
    	FromMysqlToPostgres.addPermissiontoAll();
    	renderText("Aggiunto permesso in sola lettura per tutti gli utenti");
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    
    	final int NUMERO_PERSONE_DA_IMPORTARE = 0;
    	
    	final int ANNO_DA_CUI_INIZIARE_IMPORTAZIONE = 2007;
    	
    	int absenceTypes = FromMysqlToPostgres.importAbsenceTypes();
    	//FromMysqlToPostgres.createAbsenceTypeToQualificationRelations();
    	int workingTimeTypes = FromMysqlToPostgres.importWorkingTimeTypes();
    	
    	FromMysqlToPostgres.importAll(NUMERO_PERSONE_DA_IMPORTARE, ANNO_DA_CUI_INIZIARE_IMPORTAZIONE);
      	renderText(
        		String.format("Importate dalla vecchia applicazione %d tipi di assenza con i relativi gruppi e %d tipi di orari di lavoro.\n" +
        			"Importate %d persone con i relativi dati (contratti, dati personali, assenze, timbrature, ...", 
        			absenceTypes, workingTimeTypes, NUMERO_PERSONE_DA_IMPORTARE));
    }
    
    
//	public static void test(){
//		PersonMonth pm = new PersonMonth(Person.em().getReference(Person.class, 140L), 2012,6);
//		long n = pm.getMaximumCoupleOfStampings();
//		render(n);
//	}
//	
	public static void upgradePerson(){
		FromMysqlToPostgres.upgradePerson();
		renderText("Modificati i permessi per l'utente");
	}
	
	public static void updateCompetence() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		//Person person = Person.find("bySurnameAndName", "Lucchesi", "Cristian").first();
	//	FromMysqlToPostgres.updateCompetence();
		renderText("Aggiunti gli straordinari diurni feriali alle persone nella tabella competenze");
	}
	
	public static void updatePersonDay(){
		FromMysqlToPostgres.checkFixedWorkingTime();
		renderText("Aggiornati i person day delle persone con timbratura fissa");
	}
	
	/**
	 * @deprecated Use {@link Evolutions#updateVacationPeriodRelation()} instead
	 */
	public static void updateVacationPeriodRelation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Evolutions.updateVacationPeriodRelation();
	}

	
	public static void checkNewRelation() throws ClassNotFoundException, SQLException{
		Evolutions.updateWorkingTimeTypeRelation();
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void utilities(){
		List<Person> pdList = Person.getActivePersons(new LocalDate());
		render(pdList);
	}
	
	
	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 * 
	 * 
	 */	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void fixPersonSituation(Long personId, int year, int month){
		
		PersonUtility.fixPersonSituation(personId, year, month);
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void createOvertimeFile(int year) throws IOException{
		Logger.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);
		
	}
	
	
//	public static void updateCauseInTrouble()
//	{
//		List<PersonDayInTrouble> tList = PersonDayInTrouble.findAll();
//		for(PersonDayInTrouble t : tList)
//		{
//			if(t.personDay.isFixedTimeAtWork())
//			{
//				t.cause = "timbratura disaccoppiata persona fixed";
//				t.save();
//			}
//		}
//	}

	public static void buildYaml()
	{
		//general
		ExportToYaml.buildAbsences("test/dataTest/general/absences.yml");
		ExportToYaml.buildCompetenceCodes("test/dataTest/general/competenceCodes.yml");
		
		//person
		Person person = Person.findById(146l);
		ExportToYaml.buildPerson(person, "test/dataTest/persons/lucchesi.yml");
		
		//test stampings
		ExportToYaml.buildPersonMonth(person, 2013,  9, "test/dataTest/stampings/lucchesiStampingsSettembre2013.yml");
		ExportToYaml.buildPersonMonth(person, 2013, 10, "test/dataTest/stampings/lucchesiStampingsOttobre2013.yml");
		
		//test vacations
		ExportToYaml.buildYearlyAbsences(person, 2012, "test/dataTest/absences/lucchesiAbsences2012.yml");
		ExportToYaml.buildYearlyAbsences(person, 2013, "test/dataTest/absences/lucchesiAbsences2013.yml");
		
	}

    
}
