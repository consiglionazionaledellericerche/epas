package controllers;

import it.cnr.iit.epas.ExportToYaml;
import it.cnr.iit.epas.FromMysqlToPostgres;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.ConsistencyManager;
import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.User;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;

import controllers.Resecure.NoCheck;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;


@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static ConsistencyManager consistencyManager;
	
	@Inject
	static PersonResidualYearRecapFactory yearFactory;
	
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
	
	
	public static void updatePersonDay(){
		FromMysqlToPostgres.checkFixedWorkingTime();
		renderText("Aggiornati i person day delle persone con timbratura fissa");
	}
	
	@NoCheck
	public static void utilities(){

		final List<Person> personList = PersonDao.list( 
				Optional.<String>absent(), officeDao.getOfficeAllowed(Optional.<User>absent()), 
				false, LocalDate.now(), LocalDate.now(), true)
				.list();
		
		render(personList);
	}
	
	
	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 */
	@NoCheck
	public static void fixPersonSituation(Long personId, int year, int month){	
		//TODO permessi
		consistencyManager.fixPersonSituation(personId, year, month, Security.getUser().get(), false);

	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void createOvertimeFile(int year) throws IOException{
		Logger.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void personalResidualSituation()
	{
		
		List<Person> listPerson = PersonDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Optional.<User>absent()), false, LocalDate.now(), LocalDate.now(), true).list();
		List<PersonResidualMonthRecap> listMese = new ArrayList<PersonResidualMonthRecap>();
		for(Person person : listPerson)
		{
			LocalDate today = new LocalDate().minusMonths(1);
			PersonResidualYearRecap c = 
					yearFactory.create(ContractDao.getCurrentContract(person), today.getYear(), null);
			PersonResidualMonthRecap mese = c.getMese(today.getMonthOfYear());
			listMese.add(mese);
		}
		render(listMese);
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
	
	public static void killclock()
	{
		Person person = Person.find("byName", "epas").first();
		
		
		//destroy person day in trouble
		List<PersonDay> pdList = PersonDay.find("select pd from PersonDay pd where pd.person = ?", person).fetch();
		for(PersonDay pd : pdList)
		{
			while(pd.troubles.size()>0)
			{
				PersonDayInTrouble pdt = pd.troubles.get(0);
				pd.troubles.remove(pdt);
				pdt.delete();
				pd.save();
			}
		}
		
		//destroy person day
		while(pdList.size()>0)
		{
			PersonDay pd = pdList.get(0);
			pdList.remove(pd);
			pd.delete();
		}
		
		//destroy contracts
		while(person.contracts.size()>0)
		{
			Contract c = person.contracts.get(0);
			person.contracts.remove(c);
			c.delete();
			person.save();
		}
		
		//destroy contact_data
//		if(person.contactData!=null)
//			person.contactData.delete();
		
		//destroy locations
//		if(person.location!=null)
//			person.location.delete();
//		
		person.save();
		
		renderText(person.name);
	}
   
}
