package it.cnr.iit.epas;

import java.util.List;

import javax.persistence.FetchType;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Transactional;

import models.Absence;
import models.AbsenceType;
import models.Configuration;
import models.Contract;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
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
		Logger.debug("Chiamata la funzione getResidual per %s %s alla data %s", person.name, person.surname, date);
		person = Person.findById(person.id);
		int residual = 0;
		if(person.qualification == null){
			/**
			 * questa persona non ha qualifica...come bisogna agire in questo caso? di norma dovrebbe essere un collaboratore...quindi non inquadrato
			 * come contratto cnr...
			 */
			residual = 0;
		}
		else{
			if(person.qualification.qualification == 1 || person.qualification.qualification == 2 || person.qualification.qualification == 3){
				if(person.getCurrentContract().beginContract != null && person.getCurrentContract().beginContract.isAfter(date)){
					residual = 0;
				}
				else{
					PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? and pm.year = ? order by pm.month desc",
							person, date.getMonthOfYear(), date.getYear()).first();
					if(pm != null)
						residual = pm.totalRemainingMinutes;
					else
						/**
						 * TODO: controllare se questa dicitura è corretta...
						 */
						residual = 0;
				}

			}
			else{
				/**
				 * in questo caso ritorna il residuo totale del mese precedente comprendente anche i residui derivanti dall'anno precedente
				 */
				if(date.getMonthOfYear() < Configuration.getCurrentConfiguration().monthExpireRecoveryDaysFourNine ){
					PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? " +
							"and pm.year = ? order by pm.month desc", person, date.getMonthOfYear(), date.getYear()).first();			
					if(pm != null)		
						residual = residual + pm.totalRemainingMinutes;
					else
						residual = 0;

					//					PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ? and py.year = ?", 
					//							person, date.getYear()-1).first();
					//					if(py != null)
					//						residual = residual + py.remainingMinutes;
					//					else
					//						residual = 0;
				}
				else{
					/**
					 * qui siamo nel caso in cui il mese è successivo a quello impostato in configurazione entro il quale poter usare le ore dell'
					 * anno precedente...
					 */
					List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ?", 
							person, date.getYear()).fetch();
					for(PersonMonth personMonth : pm){
						residual = residual+personMonth.progressiveAtEndOfMonthInMinutes;
					}
				}
			}
		}

		return residual;

	}


	/**
	 * questa funzione all'apparenza oscura calcola nel mese passato come parametro, quanti sono stati i giorni in cui la persona ha fatto ore/minuti
	 * in più rispetto al proprio orario di lavoro. Questa somma mi servirà per stabilire se in quel mese quella persona potrà beneficiare o meno
	 * di straordinari
	 * @return la somma delle differenze positive dei giorni del mese
	 */
	public static int getPositiveDaysForOvertime(PersonMonth personMonth){
		int positiveDifference = 0;
		LocalDate date = new LocalDate(personMonth.year, personMonth.month, 1);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				personMonth.person, date, date.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.difference > 0)
				positiveDifference = positiveDifference + pd.difference;
		}


		return positiveDifference;
	}

	public int getOvertimeAvailable(PersonMonth personMonth) {
		int positiveDaysForOvertime = getPositiveDaysForOvertime(personMonth);
		if (positiveDaysForOvertime <= 0) {
			return 0;
		}
		//TODO calcolare il tempo disponibile sottraendo al positiveDaysForOvertime le eventuali ore da recuperare
		//dall'anno precedente, dal mese precedente e dal mese corrente
		return 0;
	}

	public static boolean canTakeOvertime(Person person, int year, int month){
		boolean canOrNot = false;
		int positiveDifference = 0;
		int negativeDifference = 0;
		LocalDate date = new LocalDate(year, month, 1);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, date, date.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.difference > 0)
				positiveDifference = positiveDifference + pd.difference;
			else
				negativeDifference = negativeDifference + pd.difference;
		}
		if(positiveDifference > -negativeDifference)
			canOrNot = true;
		else{
			/**
			 * per "bilanciare" i residui negativi del mese, si va a vedere se esistono residui positivi dal mese precedente o dall'anno precedente
			 */
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
					person, year, month-1).first();

			if(pm != null){
				if(pm.totalRemainingMinutes > -negativeDifference)
					canOrNot = true;
				else 
					canOrNot = false;
			}
			else
				canOrNot = false;


		}
		return canOrNot;

	}

	/**
	 * metodo per stabilire se una persona può ancora prendere o meno giorni di permesso causa malattia del figlio
	 */
	public static boolean canTakePermissionIllnessChild(Person person, LocalDate date, AbsenceType abt){
		/**
		 * controllo che la persona abbia un figlio in età per poter usufruire del congedo
		 */
		List<PersonChildren> persChildList = PersonChildren.find("Select pc from PersonChildren pc where pc.person = ? order by pc.bornDate", 
				person).fetch();
		int code = new Integer(abt.code).intValue();
		PersonChildren child = null;
		switch(code){
		case 12:
			child = persChildList.get(0);
			if(child.bornDate.isAfter(date.minusYears(3))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(3), date, abt).fetch();
				if(existingAbsence.size() < 30)
					return true;
				else
					return false;
			}
			break;
			
		
		case 122:
			child = persChildList.get(1);
			if(child.bornDate.isAfter(date.minusYears(3))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(3), date, abt).fetch();
				if(existingAbsence.size() < 30)
					return true;
				else
					return false;
			}
			break;
			
		case 123:
			child = persChildList.get(2);
			if(child.bornDate.isAfter(date.minusYears(3))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(3), date, abt).fetch();
				if(existingAbsence.size() < 30)
					return true;
				else
					return false;
			}
			break;
			
		case 13:
			child = persChildList.get(0);
			if(child.bornDate.isAfter(date.minusYears(8))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(8), date, abt).fetch();
				if(existingAbsence.size() < 5)
					return true;
				else
					return false;
			}	
			break;
			
		case 132:
			child = persChildList.get(1);
			if(child.bornDate.isAfter(date.minusYears(8))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(8), date, abt).fetch();
				if(existingAbsence.size() < 5)
					return true;
				else
					return false;
			}	
			break;
			
		case 133:
			child = persChildList.get(2);
			if(child.bornDate.isAfter(date.minusYears(8))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(8), date, abt).fetch();
				if(existingAbsence.size() < 5)
					return true;
				else
					return false;
			}				
			break;
			
		case 134:
			child = persChildList.get(3);
			if(child.bornDate.isAfter(date.minusYears(8))){
				List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date between ? and ?" +
						"and a.absenceType = ?", person, date.minusYears(8), date, abt).fetch();
				if(existingAbsence.size() < 5)
					return true;
				else
					return false;
			}			
			break;
			
			default:
				throw new IllegalArgumentException(String.format("Il codice %s che si tenta di verificare non è compreso nella lista di quelli " +
						"previsti per la retribuzione dei giorni di malattia dei figli.", code));
		}
		return false;
	}
}




