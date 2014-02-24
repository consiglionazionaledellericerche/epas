package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
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
import models.ConfGeneral;
import models.Contract;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonthRecap;
import models.WorkingTimeType;
import models.exports.PersonsList;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;
import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import procedure.evolutions.Evolutions;


//@With(Shibboleth.class)

@With( {Secure.class, NavigationMenu.class} )
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
    
    public static void importAttCodes() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
    	FromMysqlToPostgres.importCodesAtt();
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
		List<Person> pdList = Person.getActivePersonsInDay(new LocalDate(), false);
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
		
		PersonUtility.fixPersonSituation(personId, year, month, Security.getPerson());
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void createOvertimeFile(int year) throws IOException{
		Logger.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);
		
	}
	
	
	public static void buildYaml()
	{
		//general
		ExportToYaml.buildAbsenceTypesAndQualifications("conf/absenceTypesAndQualifications.yml");
		
		ExportToYaml.buildCompetenceCodes("conf/competenceCodes.yml");
		
		ExportToYaml.buildVacationCodes("conf/vacationCodes.yml");
		
		
		//person
		/*
		Person person = Person.findById(146l);
		ExportToYaml.buildPerson(person, "test/dataTest/persons/lucchesi.yml");
		
		//test stampings
		ExportToYaml.buildPersonMonth(person, 2013,  9, "test/dataTest/stampings/lucchesiStampingsSettembre2013.yml");
		ExportToYaml.buildPersonMonth(person, 2013, 10, "test/dataTest/stampings/lucchesiStampingsOttobre2013.yml");
		
		//test vacations
		ExportToYaml.buildYearlyAbsences(person, 2012, "test/dataTest/absences/lucchesiAbsences2012.yml");
		ExportToYaml.buildYearlyAbsences(person, 2013, "test/dataTest/absences/lucchesiAbsences2013.yml");
		*/
		
	}
	
	public static void importStampings() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		FromMysqlToPostgres.importStamping();
		renderText("E' fatta");
	}
	
	
	public static void prove()
	{
		
		LocalDate initUse = ConfGeneral.getConfGeneral().initUseProgram;
		Person personLogged = Security.getPerson();
		
		//Prendo tutte le persone che hanno almeno un contratto attivo dal momento di initUse a oggi
		List<Person> personList = Person.getActivePersonsSpeedyInPeriod(initUse, new LocalDate(), personLogged, false);
		
		for(Person person : personList)
		{
			if(person.id!=131)
				continue;
			
			
			Logger.debug("Processo %s %s (%s di %s)", person.name, person.surname, personList.indexOf(person), personList.size());
			
			InitializationTime initPerson = null; ;	//TODO relazione e' 1:1
			if(person.initializationTimes!=null && person.initializationTimes.size()>0)
				initPerson = person.initializationTimes.get(0);
			List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", person).fetch();
			for(Contract c : contractList)
			{
				c.setRecapPeriods(initUse, initPerson);
			}
						
		}
		
		render(personList);
		
	}

	public static void logvariecose()
	{
		LocalDate initUse = ConfGeneral.getConfGeneral().initUseProgram;
		Person personLogged = Security.getPerson();
		
		//Prendo tutte le persone che hanno almeno un contratto attivo dal momento di initUse a oggi
		List<Person> personList = Person.getActivePersonsSpeedyInPeriod(initUse, new LocalDate(), personLogged, false);
		
		for(Person person : personList)
		{
			try
			{
				//2013
				/*
				CalcoloSituazioneAnnualePersona csap2013 = new CalcoloSituazioneAnnualePersona(person, 2013, new LocalDate());
				//2014
				CalcoloSituazioneAnnualePersona csap2014 = new CalcoloSituazioneAnnualePersona(person, 2014, new LocalDate());
				*/
			}
			catch(Exception e)
			{
				Logger.debug("ECCEZIONEEEE per la person %s %s ", person.name, person.surname);
			}
		}
		
	}
	
	/**
	 * Successivamente la procedura di importazione sono rimaste alcune computazioni da effettuare.
	 * Occorre completare la valorizzazione di initializationTime contenente i dati pre importazione (quelli fino a 2012/12/31)
	 */
	public static void mysqlIntegration()
	{
		LocalDate mysqlInitTime = new LocalDate(2013,1,1);
		List<Person> personList = Person.getActivePersonsinYear(mysqlInitTime.getYear(), false);
		for(Person person : personList)
		{
			
			if(person.initializationTimes==null || person.initializationTimes.size()==0)
			{
				Logger.debug("%s %s : no initialization time 2013-01-01", person.name, person.surname);
				continue;
			}
			InitializationTime initPerson = person.initializationTimes.get(0);	//TODO relazione e' 1:1	
			
			//ferie anno corrente fatte nel 2012 imputate al contratto attivo alla data
			Contract contract = person.getContract(mysqlInitTime);
			if(contract==null)
			{
				Logger.debug("%s %s : no active contract 2013-01-01", person.name, person.surname);
				initPerson.vacationLastYearUsed = null;
				initPerson.save();
				continue;
			}
			
			
			DateInterval year2012 = new DateInterval(new LocalDate(2012,1,1), new LocalDate(2012,12,31));
			AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
			initPerson.vacationLastYearUsed = VacationsRecap.getVacationDays(year2012, contract, ab32).size();
			Logger.debug("%s %s : %s", person.name, person.surname, initPerson.vacationLastYearUsed);
			initPerson.save();
						
		}
	}

    
}
