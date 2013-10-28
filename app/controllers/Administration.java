package controllers;

import it.cnr.iit.epas.FromMysqlToPostgres;


import java.sql.SQLException;
import java.util.List;

import org.joda.time.LocalDate;

import controllers.shib.Shibboleth;

import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;
import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

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
    
    
	public static void test(){
		PersonMonth pm = new PersonMonth(Person.em().getReference(Person.class, 140L), 2012,6);
		long n = pm.getMaximumCoupleOfStampings();
		render(n);
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
	 * metodo da lanciare per ricalcolare tutti i valori nei personday dall'inizio dell'anno alla fine dello stesso.
	 * Questo metodo viene lanciato attraverso una chiamata da browser. 
	 */
	public static void updatePersonDaysValue(){
		
		List<Person> personList = Person.findAll();
		LocalDate date = new LocalDate();
		for(Person p : personList){
			for(int month = 1; month <=12; month++){
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
						p, new LocalDate(date.getYear(), month, 1), new LocalDate(date.getYear(), month, 1).dayOfMonth().withMaximumValue()).fetch();
				for(PersonDay pd : pdList){
					pd.populatePersonDay();
				}
			}			
		}		
	}
	
	public static void updateVacationPeriodRelation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		List<Person> personList = Person.getActivePersons(new LocalDate());
		for(Person p : personList){
			Logger.debug("Cerco i contratti per %s %s", p.name, p.surname);
			List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", p).fetch();
			for(Contract con : contractList){
				Logger.debug("Sto analizzando il contratto %s", con.toString());
				Logger.debug("Inizio a creare i periodi di ferie per %s", con.person);
				if(con.expireContract == null){
					VacationPeriod first = new VacationPeriod();
					first.beginFrom = con.beginContract;
					first.endTo = con.beginContract.plusYears(3).minusDays(1);
					first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
					first.contract = con;
					first.save();
					VacationPeriod second = new VacationPeriod();
					second.beginFrom = con.beginContract.plusYears(3);
					second.endTo = null;
					second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
					second.contract =con;
					second.save();
				}
				else{
					if(con.expireContract.isAfter(con.beginContract.plusYears(3).minusDays(1))){
						VacationPeriod first = new VacationPeriod();
						first.beginFrom = con.beginContract;
						first.endTo = con.beginContract.plusYears(3).minusDays(1);
						first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
						first.contract = con;
						first.save();
						VacationPeriod second = new VacationPeriod();
						second.beginFrom = con.beginContract.plusYears(3);
						second.endTo = con.expireContract;
						second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
						second.contract =con;
						second.save();
					}
					else{
						VacationPeriod first = new VacationPeriod();
						first.beginFrom = con.beginContract;
						first.endTo = con.expireContract;
						first.contract = con;
						first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
						first.save();
					}
				}
				
			}
		}
		
	}

    
}
