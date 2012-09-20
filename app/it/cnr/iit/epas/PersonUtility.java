package it.cnr.iit.epas;

import java.util.List;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import models.Configuration;
import models.Contract;
import models.Person;
import models.PersonMonth;
import models.PersonYear;

public class PersonUtility {

	/**
	 * @param actualMonth, actualYear
	 * @return la somma dei residui mensili passati fino a questo momento; nel caso di dipendenti con qualifica da 4 a 9 
	 * se siamo in un mese prima di aprile i residui da calcolare sono su quello relativo all'anno precedente + i residui mensili fino a 
	 * quel mese; se siamo in un mese dopo aprile, invece, i residui da considerare sono solo quelli da aprile fino a quel momento.
	 * Nel caso invece la qualifica del dipendente sia da 1 a 3, i residui sono sempre validi e non terminano al 31/3
	 * 
	 * !!!IMPORTANTE!!! nel momento in cui si cambia la qualifica (da 4-9 a 1-3), viene cambiato anche il contratto. 
	 * 
	 */
	public static int getResidual(Person person, LocalDate date){
		int residual = 0;
		if(person.qualification.qualification == 1 || person.qualification.qualification == 2 || person.qualification.qualification == 3){
			if(person.getCurrentContract().beginContract.isAfter(date)){
				
			}
				
		}
		else{
			if(date.getMonthOfYear() < Configuration.getCurrentConfiguration().monthExpireRecoveryDaysFourNine ){
				List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? " +
						"and pm.year = ?", person, date.getMonthOfYear(), date.getYear()).fetch();			
				
				for(PersonMonth personMonth : pm){
					residual = residual+personMonth.progressiveAtEndOfMonthInMinutes;
				}
				PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ? and py.year = ?", 
						person, date.getYear()-1).first();
				residual = residual + py.remainingHours;
			}
			else{
				List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ?", 
						person, date.getYear()).fetch();
				for(PersonMonth personMonth : pm){
					residual = residual+personMonth.progressiveAtEndOfMonthInMinutes;
				}
			}
		}
		
		return residual;
	}

}
