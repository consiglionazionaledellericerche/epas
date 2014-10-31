package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.ExportToYaml;
import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PersonUtility;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import manager.ContractYearRecapManager;
import models.AbsenceType;
import models.Contract;
import models.ContractYearRecap;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import controllers.Resecure.NoCheck;
import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;


@With( {Secure.class, RequestInit.class} )
public class Administration extends Controller {
	
	
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
	//TODO permessi

	public static void utilities(){
		List<Person> pdList = Person.getActivePersonsInDay(new LocalDate(), Security.getOfficeAllowed(), false);
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

	@NoCheck
	public static void fixPersonSituation(Long personId, int year, int month){	
	//TODO permessi
		PersonUtility.fixPersonSituation(personId, year, month, Security.getUser().get(), false);

	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void createOvertimeFile(int year) throws IOException{
		Logger.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void personalResidualSituation()
	{
		
		List<Person> listPerson = Person.getActivePersonsInDay(new LocalDate(), Security.getOfficeAllowed(), false);
		List<Mese> listMese = new ArrayList<Mese>();
		for(Person person : listPerson)
		{
			LocalDate today = new LocalDate().minusMonths(1);
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(person.getCurrentContract(), today.getYear(), null);
			Mese mese = c.getMese(today.getYear(), today.getMonthOfYear());
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
	
	
	/**
	 * Questo metodo e' da lanciare nel caso di procedura di importazione che preleva i dati dal 2013-01-01, 
	 * (in cui anche initUse è 2013-01-01)
	 * Computazioni integrative da compiere:
	 * 1) Inserire nell'oggetto InitializationTime le ferie effettuate nell'anno precedente
	 * 2) Costruire per ogni contratto attivo alla data 2013-01-01 l'oggetto sourceData
	 * 3) Costruire per ogni contratto di cui si dispone di sufficiente informazione i riepiloghi annuali
	 * 
	 */
	public static void mysqlIntegration()
	{
		
		JPAPlugin.startTx(false);
		//Distruggere quello che c'è prima (adesso in fase di sviluppo)
		List<Contract> allContract = Contract.findAll();
		for(Contract contract : allContract)
		{
			contract.sourceDate = null;
			contract.save();
			for(ContractYearRecap yearRecap : contract.recapPeriods)
			{
				yearRecap.delete();
			}
			contract.recapPeriods = new ArrayList<ContractYearRecap>();
			contract.save();
		}
		JPAPlugin.closeTx(false);
		
		//1) Rimodellare il contenuto di InitializationTime (con ferie e residuo)
		LocalDate mySqlImportation = new LocalDate(2013,1,1);
		JPAPlugin.startTx(false);
		List<Person> personList = Person.findAll();
		for(Person person : personList)
		{
			Logger.debug("%s %s", person.name, person.surname);
			if(person.name.equals("Admin"))
				continue;
			//TODO epasclocks
			
			if(person.initializationTimes==null || person.initializationTimes.size()==0)
				continue;

			InitializationTime mysqlInitPerson = person.initializationTimes.get(0);			
			Contract contract = person.getContract(mySqlImportation);
			if(contract==null)
				continue;
	
			//AGGIORNAMENTO RISPETTO ALLA PROCEDURA DI IMPORTAZIONE
			DateInterval year2012 = new DateInterval(new LocalDate(2012,1,1), new LocalDate(2012,12,31));
			AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
			mysqlInitPerson.vacationCurrentYearUsed = VacationsRecap.getVacationDays(year2012, contract, ab32).size();
			mysqlInitPerson.save();
	
			//2) Costruire per ogni contratto attivo alla data 2013-01-01 l'oggetto sourceData
			contract.sourceDate = mySqlImportation.minusDays(1);
			contract.sourceRemainingMinutesLastYear = 0;
			contract.sourceRemainingMinutesCurrentYear = mysqlInitPerson.residualMinutesPastYear;
			contract.sourcePermissionUsed = 0;
			contract.sourceVacationCurrentYearUsed = mysqlInitPerson.vacationCurrentYearUsed;
			contract.sourceVacationLastYearUsed = 0;
			contract.sourceRecoveryDayUsed = 0;
			contract.save();
		}
		JPAPlugin.closeTx(false);
		
		
		JPAPlugin.startTx(false);
		//3) Costruire per ogni contratto di cui si dispone di sufficiente informazione i riepiloghi annuali
		for(Person person : personList)
		{
			if(person.name.equals("Admin"))
				continue;
			//TODO epasclocks
			List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", person).fetch();
			for(Contract contract : contractList)
			{
				try
				{
					ContractYearRecapManager.buildContractYearRecap(contract);
				}
				catch(Exception e)
				{
					Logger.debug("Eccezione per il contratto %s", contract.id);
				}
			}
		}
		JPAPlugin.closeTx(false);

		
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
