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

import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.yaml.snakeyaml.Yaml;

import controllers.shib.Shibboleth;
import models.AbsenceType;
import models.ConfGeneral;
import models.Contract;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonthRecap;
import models.WorkingTimeType;
import models.exports.PersonsList;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.Logger;
import play.Play;
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
    
    public static void importAttCodes() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
    	FromMysqlToPostgres.importCodesAtt();
    }
    
    
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
		List<Person> pdList = Person.getActivePersonsInDay(new LocalDate(), Security.getPerson().getOfficeAllowed(), false);
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
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void personalResidualSituation()
	{
		List<Person> listPerson = Person.getActivePersonsInDay(new LocalDate(), Security.getPerson().getOfficeAllowed(), false);
		List<Mese> listMese = new ArrayList<Mese>();
		for(Person person : listPerson)
		{
			LocalDate today = new LocalDate().minusMonths(1);
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(person, today.getYear(), null);
			Mese mese = c.getMese(today.getYear(), today.getMonthOfYear());
			listMese.add(mese);
		}
		render(listMese);
	} 
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void troublesLog()
	{
		Person personLogged = Person.find("byUsername", "admin").first();	
		List<Person> personList = Person.getActivePersonsInDay(LocalDate.now(), personLogged.getOfficeAllowed(), false);
		for(Person person : personList)
		{
			String message = "";
			
			DateInterval troubleInterval = new DateInterval(ConfGeneral.getConfGeneral().initUseProgram, LocalDate.now());
			troubleInterval = DateUtility.intervalIntersection(troubleInterval, person.getCurrentContract().getContractDateInterval());
			
			//TODO quando sarà entrata in fuzione l'implementazione init use prendere tutti i person day da quando la persona ha dati in db
			
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
					person, troubleInterval.getBegin(), troubleInterval.getEnd()).fetch();
			List<PersonDayInTrouble> troubles = new ArrayList<PersonDayInTrouble>();
			for(PersonDay pd : pdList)
			{
				for(PersonDayInTrouble trouble : pd.troubles)
				{
					if(!trouble.fixed)
						troubles.add(trouble);
				}
			}
			for(PersonDayInTrouble trouble : troubles)
			{
				if(trouble.cause.equals("timbratura disaccoppiata persona fixed"))
					continue;
				
				message = message + person.name +" "+ person.surname +" "+ trouble.personDay.date +" "+ trouble.cause +"\n";
				Logger.debug("%s %s %s %s", person.name, person.surname, trouble.personDay.date, trouble.cause);
			}
		}	
		/*
		
		SimpleEmail email = new SimpleEmail();
		if(person.contactData != null && (!person.contactData.email.trim().isEmpty())){
			Logger.debug("L'indirizzo a cui inviare la mail è: %s", person.contactData.email);
			email.addTo(person.contactData.email);
		}
			
		else
			email.addTo(person.name+"."+person.surname+"@"+"iit.cnr.it");
		email.setHostName(Play.configuration.getProperty("mail.smtp.host"));
		Integer port = new Integer(Play.configuration.getProperty("mail.smtp.port"));
		email.setSmtpPort(port.intValue());
		email.setAuthentication(Play.configuration.getProperty("mail.smtp.user"), Play.configuration.getProperty("mail.smtp.pass"));
		
		email.setFrom(Play.configuration.getProperty("mail.from.alias"));
//		if(p != null)
//			email.addCc(p.contactData.email);
		email.setSubject("controllo giorni del mese");
		email.setMsg("Salve, controllare i giorni: "+daysInTrouble+ " per "+person.name+' '+person.surname);
		email.send();
		
		*/
		
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

    
}
