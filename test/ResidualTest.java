import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import models.Contract;
import models.Person;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.hibernate.mapping.Array;
import org.joda.time.LocalDate;
import org.junit.Test;

import play.Logger;
import play.db.jpa.JPAPlugin;
import play.test.UnitTest;

public class ResidualTest extends UnitTest {
	
    @Test
    public void residualLucchesi() {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	Person person = Person.find("bySurname", "Lucchesi").first();
    	assertEquals(Double.valueOf(146), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday
     	PersonUtility.fixPersonSituation(person.id, 2013, 1, person);
    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = person.getMonthContracts(month, year);
    	for(Contract contract : monthContracts)
		{
			contract.buildContractYearRecap();
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, dateToTest);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = new VacationsRecap(person, 2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	Mese february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 0);
    	assertEquals(february.monteOreAnnoCorrente, 1445);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(26));	   //maturate(tutte) meno usate 27 - 1	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 28-0
    	assertEquals(februaryVacation.permissionUsed, new Integer(2));
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(2));

    }
    
    @Test
    public void residualSanterini() {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurnameAndName", "Santerini", "Paolo").first();
    	assertEquals(Double.valueOf(32), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday
     	PersonUtility.fixPersonSituation(person.id, 2013, 1, person);
    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = person.getMonthContracts(month, year);
    	for(Contract contract : monthContracts)
		{
			contract.buildContractYearRecap();
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, dateToTest);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = new VacationsRecap(person, 2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	Mese february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 3207);
    	assertEquals(february.monteOreAnnoCorrente, 2453);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(28));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed, new Integer(0));
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
    
    @Test
    public void residualMartinelli() {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurnameAndName", "Martinelli", "Maurizio").first();
    	assertEquals(Double.valueOf(25), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday
     	PersonUtility.fixPersonSituation(person.id, 2013, 1, person);
    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = person.getMonthContracts(month, year);
    	for(Contract contract : monthContracts)
		{
			contract.buildContractYearRecap();
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, dateToTest);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = new VacationsRecap(person, 2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	Mese february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 28764);
    	assertEquals(february.monteOreAnnoCorrente, 2166);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(25));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed, new Integer(0));
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
    
    
    /* TODO prima vedere se funzionano per i casi semplici 
    @Test
    public void residualLami() {
    	int month = 12;
    	int year = 2013;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.findById(131l);
    	assertEquals(Double.valueOf(131), Double.valueOf(person.id));
    	
    	
    	//Ricalcolo tutti i personday
    	PersonUtility.fixPersonSituation(person.id, 2013, 1, person);
    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = person.getMonthContracts(month, year);
    	for(Contract contract : monthContracts)
		{
			contract.buildContractYearRecap();
		}

    	//Costruisco la situazione residuale di febbraio 2014
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
    	
    	
    	JPAPlugin.closeTx(false);
    	assertEquals(contractMonths.size(),2);
    	Mese february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 0);
    	assertEquals(february.monteOreAnnoCorrente, 1445);
	
    }
    */

    
	


}
