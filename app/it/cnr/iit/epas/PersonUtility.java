package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.FetchType;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

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
import models.StampModificationType;
import models.StampProfile;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.JustifiedTimeAtWork;

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


	public static void checkExitStampNextDay(PersonDay pd){
		LocalDateTime beginDate = new LocalDateTime(pd.date.getYear(),pd.date.getMonthOfYear(),pd.date.getDayOfMonth(),0,0,0);
		LocalDateTime endDate = new LocalDateTime(pd.date.getYear(),pd.date.getMonthOfYear(),pd.date.getDayOfMonth(),23,59,59);

		Configuration config = Configuration.getCurrentConfiguration();
		PersonDay pdPastDay = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", pd.person, pd.date.minusDays(1)).first();
		StampProfile stampProfile = pd.getStampProfile();
		if(pdPastDay != null){			
			//lista delle timbrature del giorno precedente
			List<Stamping> reloadedStampingYesterday = Stamping.find("Select st from Stamping st where st.personDay = ? and " +
					"st.date between ? and ? order by st.date", pdPastDay, beginDate.minusDays(1), endDate.minusDays(1)).fetch();
			//controllo che la lista di timbrature ne contenga una sola e che sia una timbratura di ingresso
			if(reloadedStampingYesterday.size() == 1 && reloadedStampingYesterday.get(0).way == WayType.in){
				Logger.debug("Sono nel caso in cui ci sia una sola timbratura ed è di ingresso nel giorno precedente nel giorno %s", 
						pdPastDay.date);
				if(stampProfile == null || !stampProfile.fixedWorkingTime){
					for(Stamping st : pd.stampings){

						if(st != null){
							//controllo nelle timbrature del giorno attuale se la prima che incontro è una timbratura di uscita sulla base
							//del confronto con il massimo orario impostato in configurazione per considerarla timbratura di uscita relativa
							//al giorno precedente
							if(st.way == WayType.out && config.hourMaxToCalculateWorkTime > st.date.getHourOfDay()){
								Logger.debug("Esiste una timbratura di uscita come prima timbratura del giorno %s", pd.date);
								//in caso esista quella timbratura di uscita come prima timbratura del giorno attuale, creo una nuova timbratura
								// di uscita e la inserisco nella lista delle timbrature relative al personDay del giorno precedente.
								//E svolgo i calcoli su tempo di lavoro, differenza e progressivo
								Stamping correctStamp = new Stamping();
								Logger.debug("Aggiungo una nuova timbratura di uscita al giorno precedente alla mezzanotte ");
								correctStamp.date = new LocalDateTime(pdPastDay.date.getYear(), pdPastDay.date.getMonthOfYear(), pdPastDay.date.getDayOfMonth(), 23, 59);
								correctStamp.way = WayType.out;
								correctStamp.markedByAdmin = false;
								correctStamp.stampModificationType = StampModificationType.findById(4l);
								correctStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
								correctStamp.personDay = pdPastDay;
								correctStamp.save();
								Logger.debug("Aggiunta nuova timbratura %s con valore %s", correctStamp, correctStamp.date);
								Logger.debug("Devo rifare i calcoli in funzione di questa timbratura aggiunta");

								pdPastDay.timeAtWork = pd.toMinute(correctStamp.date) - pd.toMinute(reloadedStampingYesterday.get(0).date);
								pdPastDay.merge();
								pdPastDay.difference = pdPastDay.timeAtWork - pd.getWorkingTimeTypeDay().workingTime;
								pdPastDay.merge();
								PersonDay pdPast = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date <= ? order by pd.date desc", pd.person, pd.date.minusDays(2)).first();
								if(pdPast != null)
									pdPastDay.progressive = pdPast.progressive + pdPastDay.difference;

								else
									pdPastDay.progressive = pdPastDay.difference;
								pdPastDay.merge();
								Logger.debug("Fatti i calcoli, ora aggiungo una timbratura di ingresso alla mezzanotte del giorno %s", pd.date);
								//a questo punto devo aggiungere una timbratura di ingresso prima della prima timbratura di uscita che è anche
								//la prima timbratura del giorno attuale
								Stamping newEntranceStamp = new Stamping();
								newEntranceStamp.date = new LocalDateTime(pd.date.getYear(), pd.date.getMonthOfYear(), pd.date.getDayOfMonth(),0,0);
								newEntranceStamp.way = WayType.in;
								newEntranceStamp.markedByAdmin = false;
								newEntranceStamp.stampModificationType = StampModificationType.findById(4l);
								newEntranceStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
								newEntranceStamp.personDay = pd;
								newEntranceStamp.save();
								Logger.debug("Aggiunta la timbratura %s con valore %s", newEntranceStamp, newEntranceStamp.date);

								pd.save();
							}
						}
					}
				}

			}
		}
	}

	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return il numero di giorni in cui una persona è stata a lavoro in un giorno festivo in un certo intervallo temporale
	 */
	public static int workDayInHoliday(Person person, LocalDate begin, LocalDate end){
		int workDayInHoliday = 0;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, begin, end).fetch();
		for(PersonDay pd : pdList){
			if(pd.date.getDayOfWeek() == DateTimeConstants.SATURDAY || pd.date.getDayOfWeek() == DateTimeConstants.SUNDAY){
				workDayInHoliday++;
			}
		}
		return workDayInHoliday;
	}

	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return il numero di giorni lavorativi in cui una persona è stata effettivamente a lavoro in un certo intervallo temporale
	 */
	public static int workDayInWorkingDay(Person person, LocalDate begin, LocalDate end){
		int workDayInWorkingDay = 0;

		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, begin, end).fetch();


		for(PersonDay pd : pdList){
			if(pd.stampings != null)
				workDayInWorkingDay = workDayInWorkingDay + 1;
		}

		return workDayInWorkingDay;
	}

	/**
	 * 
	 * @param pdList
	 * @return il numero di giorni di assenza giustificata presenti nella lista di personDay passata come parametro
	 */
	public static List<PersonDay> getJustifiedAbsences(List<PersonDay> pdList){
		List<PersonDay> justifiedAbsencesPersonDay = new ArrayList<PersonDay>();
		for(PersonDay pd : pdList){

			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.justifiedTimeAtWork.minutesJustified == null)
				justifiedAbsencesPersonDay.add(pd);

		}
		return justifiedAbsencesPersonDay;
	}

	/**
	 * 
	 * @param pdList
	 * @return il numero di giorni in cui ci sono assenze non giustificate (niente assenze o timbrature per il personDay)
	 */
	public static List<PersonDay> getNotJustifiedAbsences(List<PersonDay> pdList){
		List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
		for(PersonDay pd : pdList){
			if((pd.stampings.size() == 0 && pd.absences.size() == 0) || (pd.stampings.size() == 1))
				notJustifiedAbsences.add(pd);
		}
		return notJustifiedAbsences;
	}
	
	/**
	 * 
	 * @return false se l'id passato alla funzione non trova tra le persone presenti in anagrafica, una che avesse nella vecchia applicazione un id
	 * uguale a quello che la sequence postgres genera automaticamente all'inserimento di una nuova persona in anagrafica.
	 * In particolare viene controllato il campo oldId presente per ciascuna persona e si verifica che non esista un valore uguale a quello che la 
	 * sequence postgres ha generato
	 */
	public static boolean isIdPresentInOldSoftware(Long id){
		Person person = Person.find("Select p from Person p where p.oldId = ?", id).first();
		if(person == null)
			return false;
		else
			return true;
		
	}

}




