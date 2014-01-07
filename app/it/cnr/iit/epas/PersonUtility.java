package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.FetchType;
import javax.persistence.Query;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Transactional;
import models.Absence;
import models.AbsenceType;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonth;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.StampProfile;
import models.Stamping;
import models.VacationPeriod;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

public class PersonUtility {

//	/**
//	 * @param actualMonth, actualYear
//	 * @return la somma dei residui mensili passati fino a questo momento; nel caso di dipendenti con qualifica da 4 a 9 
//	 * se siamo in un mese prima di aprile i residui da calcolare sono su quello relativo all'anno precedente + i residui mensili fino a 
//	 * quel mese; se siamo in un mese dopo aprile, invece, i residui da considerare sono solo quelli da aprile fino a quel momento.
//	 * Nel caso invece la qualifica del dipendente sia da 1 a 3, i residui sono sempre validi e non terminano al 31/3
//	 * 
//	 * !!!IMPORTANTE!!! nel momento in cui si cambia la qualifica (da 4-9 a 1-3), viene cambiato anche il contratto. 
//	 * 
//	 */
//
//	public static int getResidual(Person person, LocalDate date){
//		Logger.debug("Chiamata la funzione getResidual per %s %s alla data %s", person.name, person.surname, date);
//		person = Person.findById(person.id);
//		int residual = 0;
//		if(person.qualification == null){
//			/**
//			 * questa persona non ha qualifica...come bisogna agire in questo caso? di norma dovrebbe essere un collaboratore...quindi non inquadrato
//			 * come contratto cnr...
//			 */
//			residual = 0;
//		}
//		else{
//			if(person.qualification.qualification == 1 || person.qualification.qualification == 2 || person.qualification.qualification == 3){
//				if(person.getCurrentContract().beginContract != null && person.getCurrentContract().beginContract.isAfter(date)){
//					residual = 0;
//				}
//				else{
//					PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? and pm.year = ? order by pm.month desc",
//							person, date.getMonthOfYear(), date.getYear()).first();
//					if(pm != null)
//						residual = pm.totalRemainingMinutes;
//					else
//						/**
//						 * TODO: controllare se questa dicitura è corretta...
//						 */
//						residual = 0;
//				}
//
//			}
//			else{
//				/**
//				 * in questo caso ritorna il residuo totale del mese precedente comprendente anche i residui derivanti dall'anno precedente
//				 */
//				if(date.getMonthOfYear() < Configuration.getCurrentConfiguration().monthExpireRecoveryDaysFourNine ){
//					PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? " +
//							"and pm.year = ? order by pm.month desc", person, date.getMonthOfYear(), date.getYear()).first();			
//					if(pm != null)		
//						residual = residual + pm.totalRemainingMinutes;
//					else
//						residual = 0;
//
//					//					PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ? and py.year = ?", 
//					//							person, date.getYear()-1).first();
//					//					if(py != null)
//					//						residual = residual + py.remainingMinutes;
//					//					else
//					//						residual = 0;
//				}
//				else{
//					/**
//					 * qui siamo nel caso in cui il mese è successivo a quello impostato in configurazione entro il quale poter usare le ore dell'
//					 * anno precedente...
//					 */
//					List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ?", 
//							person, date.getYear()).fetch();
//					for(PersonMonth personMonth : pm){
//						residual = residual+personMonth.progressiveAtEndOfMonthInMinutes;
//					}
//				}
//			}
//		}
//
//		return residual;
//
//	}


	/** TODO usato in Competences.java ma riscritto più volte con nuovi algoritmi, rimuoverlo dopo averlo sostituito
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

//	public int getOvertimeAvailable(PersonMonth personMonth) {
//		int positiveDaysForOvertime = getPositiveDaysForOvertime(personMonth);
//		if (positiveDaysForOvertime <= 0) {
//			return 0;
//		}
//		//TODO calcolare il tempo disponibile sottraendo al positiveDaysForOvertime le eventuali ore da recuperare
//		//dall'anno precedente, dal mese precedente e dal mese corrente
//		return 0;
//	}

	/** TODO usato in Competences.java ma utilizza dati del person month, sostituirlo */
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


	

//	/**
//	 * 
//	 * @param person
//	 * @param begin
//	 * @param end
//	 * @return il numero di giorni in cui una persona è stata a lavoro in un giorno festivo in un certo intervallo temporale
//	 */
//	public static int workDayInHoliday(Person person, LocalDate begin, LocalDate end){
//		int workDayInHoliday = 0;
//		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//				person, begin, end).fetch();
//		for(PersonDay pd : pdList){
//			if(pd.date.getDayOfWeek() == DateTimeConstants.SATURDAY || pd.date.getDayOfWeek() == DateTimeConstants.SUNDAY){
//				workDayInHoliday++;
//			}
//		}
//		return workDayInHoliday;
//	}

//	/**
//	 * 
//	 * @param person
//	 * @param begin
//	 * @param end
//	 * @return il numero di giorni lavorativi in cui una persona è stata effettivamente a lavoro in un certo intervallo temporale
//	 */
//	public static int workDayInWorkingDay(Person person, LocalDate begin, LocalDate end){
//		int workDayInWorkingDay = 0;
//
//		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//				person, begin, end).fetch();
//
//
//		for(PersonDay pd : pdList){
//			if(pd.stampings != null)
//				workDayInWorkingDay = workDayInWorkingDay + 1;
//		}
//
//		return workDayInWorkingDay;
//	}

//	/**
//	 * 
//	 * @param pdList
//	 * @return il numero di giorni di assenza giustificata presenti nella lista di personDay passata come parametro
//	 */
//	public static List<PersonDay> getJustifiedAbsences(List<PersonDay> pdList){
//		List<PersonDay> justifiedAbsencesPersonDay = new ArrayList<PersonDay>();
//		for(PersonDay pd : pdList){
//
//			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.justifiedTimeAtWork.minutesJustified == null)
//				justifiedAbsencesPersonDay.add(pd);
//
//		}
//		return justifiedAbsencesPersonDay;
//	}

//	/**
//	 * 
//	 * @param pdList
//	 * @return il numero di giorni in cui ci sono assenze non giustificate (niente assenze o timbrature per il personDay)
//	 */
//	public static List<PersonDay> getNotJustifiedAbsences(List<PersonDay> pdList){
//		List<PersonDay> notJustifiedAbsences = new ArrayList<PersonDay>();
//		for(PersonDay pd : pdList){
//			if((pd.stampings.size() == 0 && pd.absences.size() == 0) || (pd.stampings.size() == 1))
//				notJustifiedAbsences.add(pd);
//		}
//		return notJustifiedAbsences;
//	}

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
	
	/**
	 * 
	 * @return il codice di assenza da utilizzare da scegliere tra 31,32 e 94 nel caso in cui l'utente amministratore utilizzi il codice "FER" per assegnare un giorno di ferie 
	 * alla persona
	 */
	public static AbsenceType whichVacationCode(Person person, LocalDate actualDate){
		
		
		VacationsRecap vr = new VacationsRecap(person, (short) actualDate.getYear(), actualDate);	
		
		if(vr.vacationDaysLastYearNotYetUsed>0)
			return AbsenceType.find("byCode", "31").first();
		
		if(vr.persmissionNotYetUsed>0)
			return AbsenceType.find("byCode", "94").first();	
		
		
		if(vr.vacationDaysCurrentYearNotYetUsed>0)
			return AbsenceType.find("byCode", "32").first();
		
		return null;
		/*
		Configuration config = Configuration.getCurrentConfiguration();
		AbsenceType vacationFromThisYear = AbsenceType.find("byCode", "32").first();
		AbsenceType vacationFromLastYear = AbsenceType.find("byCode", "31").first();
		
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.personDay.date between :begin and :end " +
				"and abs.absenceType = :type");
		query.setParameter("person", person).setParameter("begin", new LocalDate(year-1,1,1)).setParameter("end", new LocalDate(year-1,12,31)).setParameter("type", vacationFromThisYear);
		List<Absence> absList = query.getResultList();
		Logger.debug("Nell'anno passato %s %s ha usufruito di %d giorni di ferie", person.name, person.surname, absList.size());
		
		query.setParameter("person", person).setParameter("begin", new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue())
			.setParameter("end", new LocalDate()).setParameter("type", vacationFromLastYear);
		List<Absence> absThisYearList = query.getResultList();
		Logger.debug("Quest'anno %s %s ha usufruito di %d giorni di ferie con codice %s relativo alle ferie dell'anno passato", person.name, person.surname, absThisYearList.size(), vacationFromLastYear.code);
		

		VacationPeriod vp = person.getCurrentContract().getCurrentVacationPeriod();
		if((vp.vacationCode.vacationDays > absList.size() + absThisYearList.size()) && 
				(new LocalDate(year, month, day).isBefore(new LocalDate(year, config.monthExpiryVacationPastYear, config.dayExpiryVacationPastYear)))){
			return AbsenceType.find("byCode", "31").first();
		}
		else{
			Logger.debug("%s %s ha finito i giorni di ferie dell'anno passato, passo a controllare se può prendere dei permessi legge...", person.name, person.surname);
		}
		AbsenceType permissionDay = AbsenceType.find("byCode", "94").first();
		
		query.setParameter("begin", new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue())
			.setParameter("end", new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()).setParameter("person", person).setParameter("type", permissionDay);
		List<Absence> absPermissions = query.getResultList();
		Logger.debug("%s %s quest'anno ha usufruito di %d giorni di permesso", person.name, person.surname, absPermissions.size());
		if(vp.vacationCode.permissionDays > absPermissions.size()){
			return permissionDay;
		}
		else{
			Logger.debug("%s %s ha terminato i suoi permessi legge. Controllo se può prendere ferie dell'anno corrente", person.name, person.surname);
			
		}
	
		query.setParameter("begin", new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue())
			.setParameter("end", new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()).setParameter("person", person).setParameter("type", vacationFromThisYear);
		List<Absence> absVacationThisYear = query.getResultList();
		if(absVacationThisYear.size() <= vp.vacationCode.vacationDays)
			return AbsenceType.find("byCode", "32").first();
		else
			return null;
			*/
	}
	
	
	
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return true se in quel giorno quella persona non è in turno nè in reperibilità (metodo chiamato dal controller di inserimento assenza)
	 */
	public static boolean canPersonTakeAbsenceInShiftOrReperibility(Person person, LocalDate date){
		Query queryReperibility = JPA.em().createQuery("Select count(*) from PersonReperibilityDay prd where prd.date = :date and prd.personReperibility.person = :person");
		queryReperibility.setParameter("date", date).setParameter("person", person);
		int prdCount = queryReperibility.getFirstResult();
	//	List<PersonReperibilityDay> prd =  queryReperibility.getResultList();
		if(prdCount != 0)
			return false;
		Query queryShift = JPA.em().createQuery("Select count(*) from PersonShiftDay psd where psd.date = :date and psd.personShift.person = :person");
		queryShift.setParameter("date", date).setParameter("person", person);
		int psdCount = queryShift.getFirstResult();
		if(psdCount != 0)
			return false;
		
		return true;
	}
	
	/**
	 * 
	 * @param date
	 * @return il personDay se la data passata è di un giorno feriale, null altrimenti
	 */
	public static PersonDay createPersonDayFromDate(Person person, LocalDate date){
		if(DateUtility.isHoliday(person,date))
			return null;
		return new PersonDay(person, date);
	}
	
	/**
	 * 
	 * @param name
	 * @param surname
	 * @return una lista di stringhe ottenute concatenando nome e cognome in vari modi per proporre lo username per il 
	 * nuovo dipendente inserito 
	 */
	public static List<String> composeUsername(String name, String surname){
		List<String> usernameList = new ArrayList<String>();
		usernameList.add(name.replace(' ', '_').toLowerCase()+'.'+surname.replace(' ','_').toLowerCase());
		usernameList.add(name.trim().toLowerCase().substring(0,1)+'.'+surname.replace(' ','_').toLowerCase());
		
	
		int blankNamePosition = whichBlankPosition(name);
		int blankSurnamePosition = whichBlankPosition(surname);
		if(blankSurnamePosition > 4 && blankNamePosition == 0){
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
		}
		if(blankNamePosition > 3 && blankSurnamePosition == 0){
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.toLowerCase().replace(' ','_'));
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.toLowerCase());
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.toLowerCase().replace(' ','_'));
		}
		if(blankSurnamePosition < 4 && blankNamePosition == 0){
			usernameList.add(name.toLowerCase()+'.'+surname.trim().toLowerCase());
		}
		if(blankSurnamePosition > 4 && blankNamePosition > 3){
			usernameList.add(name.toLowerCase().replace(' ','_')+'.'+surname.toLowerCase().replace(' ','_'));
			usernameList.add(name.toLowerCase().substring(0, blankNamePosition)+'.'+surname.replace(' ','_').toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.replace(' ','_').toLowerCase());
			usernameList.add(name.replace(' ','_').toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.replace(' ','_').toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.substring(0, blankNamePosition).toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.substring(0, blankSurnamePosition).toLowerCase());
			usernameList.add(name.substring(blankNamePosition+1, name.length()).toLowerCase()+'.'+surname.substring(blankSurnamePosition+1, surname.length()).toLowerCase());
		}
		return usernameList;
	}
	
	/**
	 * 
	 * @param s
	 * @return la posizione in una stringa in cui si trova un eventuale spazio (più cognomi, più nomi...)
	 */
	private static int whichBlankPosition(String s){
		int position = 0;
		for(int i = 0; i < s.length(); i++){
			if(s.charAt(i) == ' ')
				position = i;
		}
		return position;
	}

	/**
	 * 
	 * @param absenceType
	 * @return true se è possibile prendere il codice di assenza in questione in base ai parametri di accumulo, false altrimenti 
	 */
	public static CheckMessage checkAbsenceGroup(AbsenceType absenceType, Person person, LocalDate date) {
		CheckMessage check = null;
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.nothing)){
			check = canTakeAbsenceWithNoAccumulation(absenceType, person, date);
		}
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.noMoreAbsencesAccepted)){
			check = canTakeAbsenceWithNoMoreAbsencesAccepted(absenceType, person, date);
		}
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation)){
			check = canTakeAbsenceWithReplacingCodeAndDecreasing(absenceType, person, date);
		}
		return check;
		
	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se è possibile prendere il codice di assenza passato come parametro dopo aver controllato di non aver ecceduto in quantità
	 * nel periodo di tempo previsto dal tipo di accumulo e, nel caso, lo sostituisce con il codice di rimpiazzamento se arriva al limite 
	 * previsto per quel codice  
	 */
	private static CheckMessage canTakeAbsenceWithReplacingCodeAndDecreasing(
			AbsenceType absenceType, Person person, LocalDate date) {
		
		
		int totalMinutesJustified = 0;
		List<Absence> absList = null;
		//trovo nella storia dei personDay l'ultima occorrenza in ordine temporale del codice di rimpiazzamento relativo al codice di assenza
		//che intendo inserire, di modo da fare i calcoli sulla possibilità di inserire quel codice di assenza da quel giorno in poi.
		Absence absence = Absence.find(
				"Select abs "
				+ "from Absence abs "
				+ "where abs.absenceType = ? and abs.personDay.person = ? " 
				+ "order by abs.personDay.date desc",
				absenceType.absenceTypeGroup.replacingAbsenceType, 
				person).first();
		
		if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.yearly)){
			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and abs.personDay.person = ? and" +
					" abs.personDay.date between ? and ?", 
					absenceType.absenceTypeGroup.label, person, absence.personDay.date, date).fetch();
			for(Absence abs : absList){
				totalMinutesJustified = totalMinutesJustified + abs.absenceType.justifiedTimeAtWork.minutesJustified;
			}
			if(absenceType.absenceTypeGroup.limitInMinute > totalMinutesJustified + absenceType.justifiedTimeAtWork.minutesJustified)
				/**
				 * in questo caso non si è arrivati a raggiungere il limite previsto per quella assenza oraria 
				 */
				return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
						"di rimpiazzamento", null);
			else{
				/**
				 * si è arrivati a raggiungere il limite, a questo punto esistono due possibilità:
				 * raggiunto il limite, si guarda se il codice di sostituzione, nella somma delle proprie occorrenze in ambito annuale, ha 
				 * raggiunto o meno il limite per esso previsto, se sì non si fa prendere il codice di assenza altrimenti si concede
				 */
				int totalReplacingAbsence = 0;
				List<Absence> replacingAbsenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? and " +
						"abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
						person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date, absenceType.absenceTypeGroup.replacingAbsenceType.code).fetch();
				totalReplacingAbsence = replacingAbsenceList.size();
				if(absenceType.absenceTypeGroup.replacingAbsenceType.absenceTypeGroup.limitInMinute < totalReplacingAbsence*absenceType.absenceTypeGroup.limitInMinute){
					return new CheckMessage(false,"Non è possibile prendere ulteriori assenze con questo codice poichè si è superato il limite massimo a livello annuale per il suo codice di rimpiazzamento", null);
				}
				else{
					return new CheckMessage(true, "Si può prendere il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
				}
			}
				
		}
		else{
			return new CheckMessage(true, "Si può prendere il codice di assenza richiesto.", null);	
			
			
		}
		
	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se è possibile prendere il codice d'assenza passato come parametro dopo aver controllato di non aver ecceduto in quantità
	 * nel periodo di tempo previsto dal tipo di accumulo
	 */
	private static CheckMessage canTakeAbsenceWithNoMoreAbsencesAccepted(
			AbsenceType absenceType, Person person, LocalDate date) {
		
		int totalMinutesJustified = 0;
		List<Absence> absList = null;
		//controllo che il tipo di accumulo sia su base mensile cercando nel mese tutte le occorrenze di codici di assenza che hanno
		//lo stesso gruppo identificativo
		if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.monthly)){
			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and " +
					"abs.personDay.person = ? and abs.personDay.date between ? and ?", 
					absenceType.absenceTypeGroup.label, person, date.dayOfMonth().withMinimumValue(), date).fetch();
			Logger.debug("La lista di codici di assenza con gruppo %s contiene %d elementi", absenceType.absenceTypeGroup.label, absList.size());
			for(Absence abs : absList){
				totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
			}
			if(absenceType.absenceTypeGroup.limitInMinute >= totalMinutesJustified+absenceType.justifiedTimeAtWork.minutesJustified)
				return new CheckMessage(true, "E' possibile prendere il codice di assenza", null);
			else
				return new CheckMessage(false, "La quantità usata nell'arco del mese per questo codice ha raggiunto il limite. Non si può usarne un altro.", null);
		}
		//controllo che il tipo di accumulo sia su base annuale cercando nel mese tutte le occorrenze di codici di assenza che hanno
		//lo stesso gruppo identificativo
		else{
			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and abs.personDay.person = ? and" +
					" abs.personDay.date between ? and ?", 
					absenceType.absenceTypeGroup.label, person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date).fetch();
			Logger.debug("List size: %d", absList.size());
			for(Absence abs : absList){
				if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay)
					totalMinutesJustified = person.getCurrentWorkingTimeType().getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;
				else{
					
					totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
				}
					
				
			}
			Logger.debug("TotalMinutesJustified= %d. Minuti giustificati: %d", totalMinutesJustified, absenceType.justifiedTimeAtWork.minutesJustified);
			int quantitaGiustificata;
			if(absenceType.justifiedTimeAtWork != JustifiedTimeAtWork.AllDay)
				quantitaGiustificata = absenceType.justifiedTimeAtWork.minutesJustified;
			else
				quantitaGiustificata = person.getCurrentWorkingTimeType().getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;
			if(absenceType.absenceTypeGroup.limitInMinute >= totalMinutesJustified+quantitaGiustificata)
				return new CheckMessage(true, "E' possibile prendere il codice di assenza", null);
			else
				return new CheckMessage(false, "La quantità usata nell'arco dell'anno per questo codice ha raggiunto il limite. Non si può usarne un altro.", null);
		}
		
	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se si può prendere il codice di assenza passato, considerando che quel codice d'assenza ha un gruppo che non prevede l'accumulo
	 * di valori: in effetti bisognerebbe capire se abbia senso una cosa del genere visto che allora non esistono casi che possano verificare 
	 * la situazione opposta
	 */
	private static CheckMessage canTakeAbsenceWithNoAccumulation(AbsenceType absenceType, Person person, LocalDate date) {
		
		 
		return new CheckMessage(true, "E' possibile prendere il codice d'assenza", null);
	}
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return true se la persona ha sufficiente residuo al giorno precedente per prendere un riposo compensativo
	 */
	public static boolean canTakeCompensatoryRest(Person person, LocalDate date)
	{
		if(date.getDayOfMonth()>1)
			date = date.minusDays(1);
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(person, date.getYear(), date);
		Mese mese = c.getMese(date.getYear(), date.getMonthOfYear());
		Logger.info("monteOreAnnoCorrente=%s ,  monteOreAnnoPassato=%s, workingTime=%s", mese.monteOreAnnoCorrente, mese.monteOreAnnoPassato, mese.person.getWorkingTimeType(date).getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime);
		if(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato > mese.person.getWorkingTimeType(date).getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime)
		{
			Logger.info("decido si");
			return true;
		}
		else
		{
			Logger.info("decido no");
			return false;
		}
	}
	
	/**
	 * Il numero di coppie ingresso/uscita da stampare per il personday
	 * @param pd
	 * @return
	 */
	public static int numberOfInOutInPersonDay(PersonDay pd)
	{
		if(pd == null)
			return 0;
		pd.orderStampings();

		int coupleOfStampings = 0;
		
		String lastWay = null;
		for(Stamping s : pd.stampings)
		{
			if(lastWay==null)
			{
				//trovo out chiudo una coppia
				if(s.way.description.equals("out"))
				{
					coupleOfStampings++;
					lastWay = null;
					continue;
				}
				//trovo in lastWay diventa in
				if(s.way.description.equals("in"))
				{
					lastWay = s.way.description;
					continue;
				}
				
			}
			//lastWay in
			if(lastWay.equals("in"))
			{
				//trovo out chiudo una coppia
				if(s.way.description.equals("out"))
				{
					coupleOfStampings++;
					lastWay = null;
					continue;
				}
				//trovo in chiudo una coppia e lastWay resta in
				if(s.way.description.equals("in"))
				{
					coupleOfStampings++;
					continue;
				}
			}
		}
		//l'ultima stampings e' in chiudo una coppia
		if(lastWay!=null)
			coupleOfStampings++;
		
		return coupleOfStampings;
	}
	
	/**
	 * Calcola il numero massimo di coppie di colonne ingresso/uscita da stampare nell'intero mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public static int getMaximumCoupleOfStampings(Person person, int year, int month){
		
		LocalDate begin = new LocalDate(year, month, 1);
		if(begin.isAfter(new LocalDate()))
			return 0;
		List<PersonDay> pdList = PersonDay.find("Select pd From PersonDay pd where pd.person = ? and pd.date between ? and ?", person,begin,begin.dayOfMonth().withMaximumValue() ).fetch();

		int max = 0;
		for(PersonDay pd : pdList)
		{
			int coupleOfStampings = PersonUtility.numberOfInOutInPersonDay(pd);
			
			if(max<coupleOfStampings)
				max = coupleOfStampings;
		}
		
		return max;
	}
	
	/**
	 * Genera una lista di PersonDay aggiungendo elementi fittizzi per coprire ogni giorno del mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public static List<PersonDay> getTotalPersonDayInMonth(Person person, int year, int month)
	{
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		List<PersonDay> totalDays = new ArrayList<PersonDay>();
		List<PersonDay> workingDays = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
				person, beginMonth, endMonth).fetch();
		
		int currentWorkingDays = 0;
		LocalDate currentDate = beginMonth;
		while(!currentDate.isAfter(endMonth))
		{
			if(currentWorkingDays<workingDays.size() && workingDays.get(currentWorkingDays).date.isEqual(currentDate))
			{
				totalDays.add(workingDays.get(currentWorkingDays));
				currentWorkingDays++;
			}
			else
			{
				PersonDay previusPersonDay = null;
				if(totalDays.size()>0)
					previusPersonDay = totalDays.get(totalDays.size()-1);
				
				PersonDay newPersonDay; 
				//primo giorno del mese festivo 
				if(previusPersonDay==null)
					newPersonDay = new PersonDay(person, new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0, 0);
				//altri giorni festivi
				else
				{
					newPersonDay = new PersonDay(person, new LocalDate(year, month, currentDate.getDayOfMonth()), 0, 0, previusPersonDay.progressive);
				}
					
				totalDays.add(newPersonDay);
				
			}
			currentDate = currentDate.plusDays(1);
		}
		return totalDays;
	}
	
	/**
	 * //TODO utilizzare jpa per prendere direttamente i codici (e migrare ad una lista)
	 * @param days lista di PersonDay
	 * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
	 */
	public static Map<AbsenceType,Integer> getAllAbsenceCodeInMonth(List<PersonDay> personDays){
		int month = personDays.get(0).date.getMonthOfYear();
		int year = personDays.get(0).date.getYear();
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		Person person = personDays.get(0).person;
		
		List<AbsenceType> abtList = AbsenceType.find("Select abt from AbsenceType abt, Absence ab, PersonDay pd where ab.personDay = pd and ab.absenceType = abt and pd.person = ? and pd.date between ? and ?", person, beginMonth, endMonth ).fetch();
		Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();
		int i = 0;
		for(AbsenceType abt : abtList)
		{
			boolean stato = absenceCodeMap.containsKey(abt);
			if(stato==false){
				i=1;
				absenceCodeMap.put(abt,i);            	 
			} else{
				i = absenceCodeMap.get(abt);
				absenceCodeMap.remove(abt);
				absenceCodeMap.put(abt, i+1);
			}
		}
		return absenceCodeMap;
		/*
		if(absenceCodeMap.isEmpty()){
			int i = 0;
			for(PersonDay pd : personDays){
				for (Absence absence : pd.absences) {
					AbsenceType absenceType = absence.absenceType;
					if(absenceType != null){
						boolean stato = absenceCodeMap.containsKey(absenceType);
						if(stato==false){
							i=1;
							absenceCodeMap.put(absenceType,i);            	 
						} else{
							i = absenceCodeMap.get(absenceType);
							absenceCodeMap.remove(absenceType);
							absenceCodeMap.put(absenceType, i+1);
						}
					}            
				}	 
			}       
		}

		return absenceCodeMap;	
		*/
	}
	
	/**
	 * Il numero di buoni pasto usabili all'interno della lista di person day passata come parametro
	 * @return
	 */
	public static int numberOfMealTicketToUse(Person person, int year, int month){
		
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		List<PersonDay> workingDays = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? and pd.isTicketAvailable = ? order by pd.date",
				person, beginMonth, endMonth, true).fetch();
		int number = 0;
		for(PersonDay pd : workingDays)
		{
			if(!pd.isHoliday())
				number++;
		}
		return number;
		//return workingDays.size();
		/*
		int tickets=0;
		for(PersonDay pd : workingDays)
		{
			if(pd.isTicketAvailable==true)
				tickets++;
		}
		return tickets;
		*/
	}
	
	

	/**
	 * Il numero di buoni pasto da restituire all'interno della lista di person day passata come parametro
	 * @return
	 */
	public static int numberOfMealTicketToRender(Person person, int year, int month){
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		List<PersonDay> pdListNoTicket = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? and pd.isTicketAvailable = ? order by pd.date",
				person, beginMonth, endMonth, false).fetch();
		int ticketTorender = pdListNoTicket.size();
		
		//tolgo da ticket da restituire i giorni festivi
		for(PersonDay pd : pdListNoTicket)
			if(pd.isHoliday())
				ticketTorender--;
		
		return ticketTorender;
	}
	
	/**
	 * 
	 * @return il numero di giorni lavorati in sede. Per stabilirlo si controlla che per ogni giorno lavorativo, esista almeno una 
	 * timbratura.
	 */
	public static int basedWorkingDays(List<PersonDay> personDays){
		int basedDays=0;
		for(PersonDay pd : personDays)
		{
			boolean fixed = pd.isFixedTimeAtWork();
			if(pd.isHoliday())
				continue;
			
			if(fixed && !pd.isAllDayAbsences() )
			{
				basedDays++;
			}
			else if(!fixed && pd.stampings.size()>0 && !pd.isAllDayAbsences() )
			{
				basedDays++;
			}
		}
		return basedDays;
	}
	
	/**
	 * Il numero di riposi compensativi utilizzati nell'anno dalla persona
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public static int numberOfCompensatoryRestUntilToday(Person person, int year, int month){
		
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.absenceType.code = :code " +
				"and abs.personDay.date between :begin and :end");
		query.setParameter("person", person)
		.setParameter("code", "91")
		.setParameter("begin", new LocalDate(year,1,1))
		.setParameter("end", new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
		
		return query.getResultList().size();
	}
	
	/**
	 * Aggiorna i person day della persona dei mesi che cadono entro l'intervallo temporale [dateFrom,dateTo]
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 */
	public static void updatePersonDaysIntoInterval(Person person, LocalDate dateFrom, LocalDate dateTo)
	{
		LocalDate monthBegin = new LocalDate(dateFrom.getYear(), dateFrom.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		while(true)
		{
			List<PersonDay> pdList = PersonDay.find(
					"select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date asc",
					person,
					monthBegin,
					monthEnd).fetch();
			for(PersonDay pd : pdList)
			{
				pd.populatePersonDay();
			}
			if(monthEnd.isEqual(dateTo) || monthEnd.isAfter(dateTo))
				return;
			monthBegin = monthBegin.plusMonths(1);
			monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		}

	}

	
	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 */	
	public static void fixPersonSituation(Long personId, int year, int month){
		
		if(personId==-1)
			personId=null;

		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		JPAPlugin.startTx(false);
		if(personId==null)
		{
			List<Person> personList = Person.getActivePersonsInMonth(month, year);
			for(Person person : personList)
			{
				PersonUtility.checkHistoryError(person.id, year, month);
			}
		}
		else
		{
			PersonUtility.checkHistoryError(personId, year, month);
		}
		JPAPlugin.closeTx(false);
		
		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		JPAPlugin.startTx(true);
		List<Person> personList = new ArrayList<Person>();
		if(personId == null)
		{
			personList = Person.findAll();
		}
		else
		{
			Person person = Person.findById(personId);
			personList.add(person);
		}
		JPAPlugin.closeTx(false);
		
		int i = 1;
		
		for(Person p : personList){
			Logger.info("Update person situation %s (%s di %s) dal %s-%s-01 a oggi", p.surname, i++, personList.size(), year, month);
			
			LocalDate actualMonth = new LocalDate(year, month, 1);
			LocalDate endMonth = new LocalDate().withDayOfMonth(1);
			JPAPlugin.startTx(false);
			while(!actualMonth.isAfter(endMonth))
			{
				Logger.info("Mese inizio %s", actualMonth);
				JPAPlugin.closeTx(false);
				JPAPlugin.startTx(false);
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
						p,
						actualMonth, 
						actualMonth.dayOfMonth().withMaximumValue())
						.fetch();
				for(PersonDay pd : pdList){
					if(pd.date.getMonthOfYear()==6)Logger.info("Giorno inizio %s", pd.date);
					
					pd.populatePersonDay();
					if(pd.date.getMonthOfYear()==6)Logger.info("Giorno fine %s", pd.date);
				}
				actualMonth = actualMonth.plusMonths(1);
				JPAPlugin.closeTx(false);
				JPAPlugin.startTx(false);
				Logger.info("Mese fine %s", actualMonth);
				
			}
			JPAPlugin.closeTx(false);
		}
	}
	
	 /**
	 * Verifica per la persona (se attiva) che alla data 
	 * 	(1) in caso di giorno lavorativo il person day esista. 
	 * 		Altrimenti viene creato e persistito un personday vuoto e inserito un record nella tabella PersonDayInTrouble.
	 * 	(2) il person day presenti una situazione di timbrature corretta dal punto di vista logico. 
	 * 		In caso contrario viene inserito un record nella tabella PersonDayInTrouble. Situazioni di timbrature errate si verificano nei casi 
	 *  	(a) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 		(b) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature. 
	 * @param personid la persona da controllare
	 * @param dayToCheck il giorno da controllare
	 */
	private static void checkPersonDay(Long personid, LocalDate dayToCheck)
	{
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);

		Person personToCheck = Person.findById(personid);
		if(!personToCheck.isActive(dayToCheck)) {
			return;
		}

		PersonDay pd = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? AND pd.date = ? ", 
				personToCheck,dayToCheck).first();

		if(pd!=null){
			pd.checkForPersonDayInTrouble(); 
			return;
		}
		else {
			pd = new PersonDay(personToCheck, dayToCheck);
			if(pd.isHoliday()) {
				return;
			}
			pd.create();
			pd.populatePersonDay();
			pd.save();
			pd.checkForPersonDayInTrouble();
			return;
		}
	}
	
	
	

	
	
	/**
	 * A partire dal mese e anno passati al metodo fino al giorno di ieri (yesterday)
	 * controlla la presenza di errori nelle timbrature, inserisce i giorni problematici nella tabella PersonDayInTrouble
	 * e setta a fixed true quelli che in passato avevano problemi e che invece sono stati risolti.
	 * @param personid la persona da controllare
	 * @param year l'anno di partenza
	 * @param month il mese di partenza
	 */
	private static void checkHistoryError(Long personid, int year, int month)
	{
		Person person = Person.findById(personid);
		Logger.info("Check history error %s dal %s-%s-1 a oggi", person.surname, year, month);
		LocalDate date = new LocalDate(year,month,1);
		LocalDate today = new LocalDate();
		while(true)
		{
			PersonUtility.checkPersonDay(personid, date);
			date = date.plusDays(1);
			if(date.isEqual(today))
				break;
		}
	}
	
	/**
	 * 
	 * @return la lista dei codici competenza attivi per le persone nell'anno in corso
	 */
	public static List<CompetenceCode> activeCompetence(){
		List<CompetenceCode> competenceCodeList = new ArrayList<CompetenceCode>();
		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.year = ?", new LocalDate().getYear()).fetch();
		for(Competence comp : competenceList){
			if(!competenceCodeList.contains(comp.competenceCode))
				competenceCodeList.add(comp.competenceCode);
		}
		return competenceCodeList;
	}
	

}




