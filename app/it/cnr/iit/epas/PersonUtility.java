package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;

import manager.ConfGeneralManager;
import manager.ContractManager;
import manager.ContractYearRecapManager;

import manager.PersonDayManager;
import manager.PersonManager;
import manager.WorkingTimeTypeManager;

import models.Absence;
import models.AbsenceType;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonthRecap;
import models.Stamping;
import models.User;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.libs.Mail;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonDayInTroubleDao;

public class PersonUtility {


	/** TODO usato in Competences.java ma riscritto più volte con nuovi algoritmi, rimuoverlo dopo averlo sostituito
	 * questa funzione all'apparenza oscura calcola nel mese passato come parametro, quanti sono stati i giorni in cui la persona ha fatto ore/minuti
	 * in più rispetto al proprio orario di lavoro. Questa somma mi servirà per stabilire se in quel mese quella persona potrà beneficiare o meno
	 * di straordinari
	 * @return la somma delle differenze positive dei giorni del mese
	 */
	public static int getPositiveDaysForOvertime(PersonMonthRecap personMonth){
		int positiveDifference = 0;
		LocalDate date = new LocalDate(personMonth.year, personMonth.month, 1);
		List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(personMonth.person, date, Optional.fromNullable(date.dayOfMonth().withMaximumValue()), false);
//		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//				personMonth.person, date, date.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.difference > 0)
				positiveDifference = positiveDifference + pd.difference;
		}


		return positiveDifference;
	}


	/**
	 * metodo per stabilire se una persona può ancora prendere o meno giorni di permesso causa malattia del figlio
	 */
	public static Boolean canTakePermissionIllnessChild(Person person, LocalDate date, AbsenceType abt){
		/**
		 * controllo che la persona abbia un figlio in età per poter usufruire del congedo
		 */
		List<PersonChildren> persChildList = PersonChildrenDao.getAllPersonChildren(person);
//		List<PersonChildren> persChildList = PersonChildren.find("Select pc from PersonChildren pc where pc.person = ? order by pc.bornDate", 
//				person).fetch();
		int code = new Integer(abt.code).intValue();
		Logger.debug("Person children list size: %d", persChildList.size());
		PersonChildren child = null;
		switch(code){
		case 12:
			if(persChildList.size()<1)
				return null;
			child = persChildList.get(0) != null ? persChildList.get(0) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(3))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(3), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(3), date, abt.code).fetch();
					if(existingAbsence.size() < 30)
						return true;
					else
						return false;
				}
			}
			else{
				return false;
			}

			break;


		case 122:
			if(persChildList.size()<2)
				return null;
			child = persChildList.get(1) != null ? persChildList.get(1) : null;
			Logger.debug("Il riferimento del codice è per %s %s. Nato il %s", child.surname, child.name, child.bornDate);
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(3))){
					Logger.debug("La data di nascita del figlio è inferiore ai 3 anni necessari per prendere il codice");
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(3), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(3), date, abt.code).fetch();
					Logger.debug("Il dipendente ha già preso %d giorni con codice %s", existingAbsence.size(), abt.code);
					if(existingAbsence.size() < 30)
						return true;
					else
						return false;
				}
			}

			break;

		case 123:
			if(persChildList.size()<3)	//TODO implementare un sistema di ritorno messaggio al chiamante (esempio ritorno un oggetto Message con esito booleano e una stringa stato)
				return null;
			child = persChildList.get(2) != null ? persChildList.get(2) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(3))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(3), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(3), date, abt.code).fetch();
					if(existingAbsence.size() < 30)
						return true;
					else
						return false;
				}
			}
			else{
				return false;
			}

			break;

		case 13:
			if(persChildList.size()<1)
				return null;
			child = persChildList.get(0) != null ? persChildList.get(0) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(8))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(8), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(8), date, abt.code).fetch();
					if(existingAbsence.size() < 5)
						return true;
					else
						return false;
				}	
			}

			else{
				return false;
			}
			break;

		case 132:
			if(persChildList.size()<2)
				return null;
			child = persChildList.get(1) != null ? persChildList.get(1) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(8))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(8), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(8), date, abt.code).fetch();
					if(existingAbsence.size() < 5)
						return true;
					else
						return false;
				}	
			}
			else{
				return false;
			}

			break;

		case 133:
			if(persChildList.size()<3)
				return null;
			child = persChildList.get(2) != null ? persChildList.get(2) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(8))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(8), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(8), date, abt.code).fetch();
					if(existingAbsence.size() < 5)
						return true;
					else
						return false;
				}	
			}
			else{
				return false;
			}
			break;

		case 134:
			if(persChildList.size()<4)
				return null;
			child = persChildList.get(3) != null ? persChildList.get(3) : null;
			if(child != null){
				if(child.bornDate.isAfter(date.minusYears(8))){
					List<Absence> existingAbsence = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(abt.code), date.minusYears(8), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> existingAbsence = Absence.find("Select a from Absence a where a.personDay.person = ? and a.personDay.date between ? and ?" +
//							"and a.absenceType.code = ?", person, date.minusYears(8), date, abt.code).fetch();
					if(existingAbsence.size() < 5)
						return true;
					else
						return false;
				}	
			}
			else{
				return false;
			}

			break;

		default:
			throw new IllegalArgumentException(String.format("Il codice %s che si tenta di verificare non è compreso nella lista di quelli " +
					"previsti per la retribuzione dei giorni di malattia dei figli.", code));
		}
		return false;
	}


	/**
	 * 
	 * @return false se l'id passato alla funzione non trova tra le persone presenti in anagrafica, una che avesse nella vecchia applicazione un id
	 * uguale a quello che la sequence postgres genera automaticamente all'inserimento di una nuova persona in anagrafica.
	 * In particolare viene controllato il campo oldId presente per ciascuna persona e si verifica che non esista un valore uguale a quello che la 
	 * sequence postgres ha generato
	 */
	public static boolean isIdPresentInOldSoftware(Long id){
		Person person = PersonDao.getPersonByOldID(id);
		//Person person = Person.find("Select p from Person p where p.oldId = ?", id).first();
		if(person == null)
			return false;
		else
			return true;

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
		//if(person.isHoliday(date))
		if(PersonManager.isHoliday(person, date))
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
		Absence absence = AbsenceDao.getLastOccurenceAbsenceInPeriod(absenceType, person, Optional.fromNullable(new LocalDate(date.getYear(),1,1)), date);
//		Absence absence = Absence.find(
//				"Select abs "
//						+ "from Absence abs "
//						+ "where abs.absenceType = ? and abs.personDay.person = ? "
//						+ "and abs.personDay.date between ? and ? "
//						+ "order by abs.personDay.date desc",
//						absenceType.absenceTypeGroup.replacingAbsenceType, 
//						person, new LocalDate(date.getYear(),1,1), date).first();
		if(absence != null){

			int minutesExcess = minutesExcessPreviousAbsenceType(absenceType, person, date);

			if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.yearly)){
				absList = AbsenceDao.getReplacingAbsenceOccurrenceListInPeriod(absenceType, person, absence.personDay.date, date);
//				absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and abs.personDay.person = ? and" +
//						" abs.personDay.date > ? and abs.personDay.date <= ?", 
//						absenceType.absenceTypeGroup.label, person, absence.personDay.date, date).fetch();
				for(Absence abs : absList){
					totalMinutesJustified = totalMinutesJustified + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				}
				if(absenceType.absenceTypeGroup.limitInMinute > totalMinutesJustified + absenceType.justifiedTimeAtWork.minutesJustified + minutesExcess)
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
					List<Absence> replacingAbsenceList = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(absenceType.absenceTypeGroup.replacingAbsenceType.code), 
							date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date, Optional.<JustifiedTimeAtWork>absent(), false, false);
//					List<Absence> replacingAbsenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? and " +
//							"abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
//							person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date, absenceType.absenceTypeGroup.replacingAbsenceType.code).fetch();
					totalReplacingAbsence = replacingAbsenceList.size();
					if(absenceType.absenceTypeGroup.replacingAbsenceType.absenceTypeGroup.limitInMinute < totalReplacingAbsence*absenceType.absenceTypeGroup.limitInMinute){
						return new CheckMessage(false,"Non è possibile prendere ulteriori assenze con questo codice poichè si è superato il limite massimo a livello annuale per il suo codice di rimpiazzamento", null);
					}
					else{
						return new CheckMessage(true, "Si può prendere il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
					}
				}

			}
			else if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.always)){

				absList = AbsenceDao.getReplacingAbsenceOccurrenceListInPeriod(absenceType, person, absence.personDay.date, date);
//				absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and abs.personDay.person = ? and" +
//						" abs.personDay.date between ? and ?", 
//						absenceType.absenceTypeGroup.label, person, absence.personDay.date, date).fetch();
				for(Absence abs : absList){
					if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay)
						//totalMinutesJustified = person.getCurrentWorkingTimeType().getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;
						totalMinutesJustified = WorkingTimeTypeManager.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek(), person.getCurrentWorkingTimeType()).workingTime;
					else{

						totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
					}

				}
				if(absenceType.absenceTypeGroup.limitInMinute > totalMinutesJustified + absenceType.justifiedTimeAtWork.minutesJustified)
					/**
					 * in questo caso non si è arrivati a raggiungere il limite previsto per quella assenza oraria 
					 */
					return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
							"di rimpiazzamento", null);
				else{		
					return new CheckMessage(true, "Si può prendere il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
				}

			}
		}

		else{

			absList = AbsenceDao.getReplacingAbsenceOccurrenceListInPeriod(absenceType, person, new LocalDate(date.getYear(),1,1), date);
//			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and " +
//					"abs.personDay.person = ? and abs.personDay.date between ? and ?", 
//					absenceType.absenceTypeGroup.label, 
//					person, 
//					new LocalDate(date.getYear(),1,1), 
//					date).fetch();

			for(Absence abs : absList){
				totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
			}
			if(totalMinutesJustified + absenceType.justifiedTimeAtWork.minutesJustified > absenceType.absenceTypeGroup.limitInMinute)
				return new CheckMessage(true, "Si può inserire il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
			else
				return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
						"di rimpiazzamento", null);
		}


		return new CheckMessage(true, "Si può prendere il codice di assenza richiesto.", null);	
	}


	/**
	 * 
	 * @param abt
	 * @param person
	 * @param date
	 * @return i minuti in eccesso, se ci sono, relativi all'inserimento del precedente codice di assenza dello stesso tipo 
	 */
	private static int minutesExcessPreviousAbsenceType(AbsenceType abt, Person person, LocalDate date){

		//cerco l'ultima occorrenza del codice di completamento
		Absence absence = AbsenceDao.getLastOccurenceAbsenceInPeriod(abt, person, Optional.<LocalDate>absent(), date);
//		Absence absence = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
//				"and abs.absenceType.absenceTypeGroup.label = ? " +
//				"and abs.personDay.date < ? order by abs.personDay.date desc", person, abt.absenceTypeGroup.label, date).first();
		if(absence == null)
			return 0;

		List<Absence> absList = AbsenceDao.getReplacingAbsenceOccurrenceListInPeriod(abt, person, new LocalDate(date.getYear(),1,1), date);
//		List<Absence> absList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
//				"and abs.personDay.date between ? and ? and abs.absenceType.absenceTypeGroup.label = ?", 
//				person, new LocalDate(date.getYear(),1,1), date, abt.absenceTypeGroup.label).fetch();
		int minutesExcess = 0;
		int minutesJustified = 0;
		for(Absence abs : absList){
			minutesJustified = minutesJustified + abs.absenceType.justifiedTimeAtWork.minutesJustified;
			if(minutesJustified + minutesExcess > abs.absenceType.absenceTypeGroup.limitInMinute ){
				minutesExcess = minutesExcess + minutesJustified - abs.absenceType.absenceTypeGroup.limitInMinute;
				minutesJustified = 0;
			}
		}		

		return minutesExcess;
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
			absList = AbsenceDao.getAllAbsencesWithSameLabel(absenceType, person, date.dayOfMonth().withMinimumValue(), date);

//			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and " +
//					"abs.personDay.person = ? and abs.personDay.date between ? and ?", 
//					absenceType.absenceTypeGroup.label, person, date.dayOfMonth().withMinimumValue(), date).fetch();
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
			absList = AbsenceDao.getReplacingAbsenceOccurrenceListInPeriod(absenceType, person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date);
//			absList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.label = ? and abs.personDay.person = ? and" +
//					" abs.personDay.date between ? and ?", 
//					absenceType.absenceTypeGroup.label, person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date).fetch();
			Logger.debug("List size: %d", absList.size());
			for(Absence abs : absList){
				if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay)
					//totalMinutesJustified = person.getCurrentWorkingTimeType().getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;
					totalMinutesJustified = WorkingTimeTypeManager.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek(), person.getCurrentWorkingTimeType()).workingTime;
				else{

					totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
				}


			}
			Logger.debug("TotalMinutesJustified= %d. Minuti giustificati: %d", totalMinutesJustified, absenceType.justifiedTimeAtWork.minutesJustified);
			int quantitaGiustificata;
			if(absenceType.justifiedTimeAtWork != JustifiedTimeAtWork.AllDay)
				quantitaGiustificata = absenceType.justifiedTimeAtWork.minutesJustified;
			else
				quantitaGiustificata = WorkingTimeTypeManager.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek(), person.getCurrentWorkingTimeType()).workingTime;
				//quantitaGiustificata = person.getCurrentWorkingTimeType().getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;
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
	 * Il numero di coppie ingresso/uscita da stampare per il personday
	 * @param pd
	 * @return
	 */
	public static int numberOfInOutInPersonDay(PersonDay pd)
	{
		if(pd == null)
			return 0;
		PersonDayManager.orderStampings(pd);

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
		List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(person, begin, Optional.fromNullable(begin.dayOfMonth().withMaximumValue()), false);
		//List<PersonDay> pdList = PersonDay.find("Select pd From PersonDay pd where pd.person = ? and pd.date between ? and ?", person,begin,begin.dayOfMonth().withMaximumValue() ).fetch();

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
		List<PersonDay> workingDays = PersonDayDao.getPersonDayInPeriod(person, beginMonth, Optional.fromNullable(endMonth), true);
//		List<PersonDay> workingDays = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
//				person, beginMonth, endMonth).fetch();

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

	//	List<AbsenceType> abtList = AbsenceTypeDao.getAbsenceTypeInPeriod(beginMonth, endMonth, person);
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
	}

	/**
	 * Il numero di buoni pasto usabili all'interno della lista di person day passata come parametro
	 * @return
	 */
	public static int numberOfMealTicketToUse(Person person, int year, int month){

		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> workingDays = PersonDayDao.getPersonDayForTicket(person, beginMonth, endMonth, true);
//		List<PersonDay> workingDays = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? and pd.isTicketAvailable = ? order by pd.date",
//				person, beginMonth, endMonth, true).fetch();
		int number = 0;
		for(PersonDay pd : workingDays)
		{
			if(!pd.isHoliday() )
				number++;
		}
		return number;
	}



	/**
	 * Il numero di buoni pasto da restituire all'interno della lista di person day passata come parametro
	 * @return
	 */
	public static int numberOfMealTicketToRender(Person person, int year, int month){
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		List<PersonDay> pdListNoTicket = PersonDayDao.getPersonDayForTicket(person, beginMonth, endMonth, false);
//		List<PersonDay> pdListNoTicket = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? and pd.isTicketAvailable = ? order by pd.date",
//				person, beginMonth, endMonth, false).fetch();
		int ticketTorender = pdListNoTicket.size();

		
		for(PersonDay pd : pdListNoTicket) {
			
			//tolgo da ticket da restituire i giorni festivi e oggi e i giorni futuri
			if(pd.isHoliday() || pd.isToday() ) 
			{
				ticketTorender--;
				continue;
			}
			
			//tolgo da ticket da restituire i giorni futuri in cui non ho assenze
			if(pd.date.isAfter(LocalDate.now()) && pd.absences.isEmpty())
				ticketTorender--;
		}

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

			if(fixed && !PersonDayManager.isAllDayAbsences(pd) )
			{
				basedDays++;
			}
			else if(!fixed && pd.stampings.size()>0 && !PersonDayManager.isAllDayAbsences(pd) )
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
		//TODO questo metodo è delicato. Prendere comunque il numero di riposi nell'anno solare. Ciclare a ritroso sui 
		//Contratti per cercare se esiste un sourceContract
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
	 * 
	 * @deprecated use {@link #PersonUtility.updatePersonDaysFromDate()} instead. 
	 */
	@Deprecated
	public static void updatePersonDaysIntoInterval(Person person, LocalDate dateFrom, LocalDate dateTo)
	{
		LocalDate monthBegin = new LocalDate(dateFrom.getYear(), dateFrom.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		while(true)
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(person, monthBegin, Optional.fromNullable(monthEnd), true);
//			List<PersonDay> pdList = PersonDay.find(
//					"select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date asc",
//					person,
//					monthBegin,
//					monthEnd).fetch();
			for(PersonDay pd : pdList)
			{
				PersonDayManager.populatePersonDay(pd);
			}
//			FIXME c'è realmente bisogno di fare la populate oltre la data di oggi??
			if(monthEnd.isEqual(dateTo) || monthEnd.isAfter(dateTo))
				return;
			monthBegin = monthBegin.plusMonths(1);
			monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		}

	}

	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone attive alla data di ieri
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 * @param userLogged
	 * @throws EmailException 
	 */
	public static void fixPersonSituation(Long personId, int year, int month, User userLogged, boolean sendEmail){

		if(userLogged==null)
			return;

		// (0) Costruisco la lista di persone su cui voglio operare
		List<Person> personList = new ArrayList<Person>();
		if(personId==-1)
			personId=null;
		if(personId==null) {
			
			LocalDate begin = new LocalDate(year, month, 1);
			LocalDate end = new LocalDate().minusDays(1);
			//personList = Person.getActivePersonsSpeedyInPeriod(begin, end, officeAllowed, false);	
			personList = PersonDao.list(Optional.<String>absent(), 
					OfficeDao.getOfficeAllowed(Optional.fromNullable(userLogged)), false, begin, end, true).list();
		}
		else {
			
			//TODO controllare che personLogged abbia i diritti sulla persona
			personList.add(PersonDao.getPersonById(personId));
			//personList.add((Person)Person.findById(personId));
		}
		
		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		JPAPlugin.startTx(false);
		for(Person person : personList) {
				PersonUtility.checkHistoryError(person, year, month);
		}
		JPAPlugin.closeTx(false);

		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		int i = 1;
		for(Person p : personList){
			Logger.info("Update person situation %s (%s di %s) dal %s-%s-01 a oggi", p.surname, i++, personList.size(), year, month);

			LocalDate actualMonth = new LocalDate(year, month, 1);
			LocalDate endMonth = new LocalDate().withDayOfMonth(1);
			JPAPlugin.startTx(false);
			while(!actualMonth.isAfter(endMonth)) {

				List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(p, actualMonth, Optional.fromNullable(actualMonth.dayOfMonth().withMaximumValue()), true);
//				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
//						p, actualMonth, actualMonth.dayOfMonth().withMaximumValue()).fetch();


				for(PersonDay pd : pdList){
					JPAPlugin.closeTx(false);
					JPAPlugin.startTx(false);
					PersonDay pd1 = PersonDayDao.getPersonDayById(pd.id);
					//PersonDay pd1 = PersonDay.findById(pd.id);
					PersonDayManager.populatePersonDay(pd1);
					JPAPlugin.closeTx(false);
					JPAPlugin.startTx(false);
				}

				actualMonth = actualMonth.plusMonths(1);
			}
			JPAPlugin.closeTx(false);
		}
		
		//(3) 
		JPAPlugin.startTx(false);
		i = 1;
		for(Person p : personList) {
			
			JPAPlugin.closeTx(false);	
			JPAPlugin.startTx(false);
			p = PersonDao.getPersonById(p.id);
			
			Logger.info("Update residui %s (%s di %s) dal %s-%s-01 a oggi", p.surname, i++, personList.size(), year, month);
			List<Contract> contractList = ContractDao.getPersonContractList(p);
			//List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", p).fetch();

			for(Contract contract : contractList) {

				ContractYearRecapManager.buildContractYearRecap(contract);
			}
		}
		JPAPlugin.closeTx(false);		
		
		
		//(4) Invio mail per controllo timbrature da farsi solo nei giorni feriali
		if( sendEmail ) {
			
			if( LocalDate.now().getDayOfWeek() != DateTimeConstants.SATURDAY 
					&& LocalDate.now().getDayOfWeek() != DateTimeConstants.SUNDAY){

				JPAPlugin.startTx(false);
				LocalDate begin = new LocalDate().minusMonths(1);
				LocalDate end = new LocalDate().minusDays(1);

				for(Person p : personList){
					
					Logger.debug("Chiamato controllo sul giorni %s %s", begin, end);
					if(p.wantEmail)
						checkPersonDayForSendingEmail(p, begin, end, "timbratura");
					else
						Logger.info("Non verrà inviata la mail a %s %s in quanto il campo di invio mail è false", p.name, p.surname);

				}
				JPAPlugin.closeTx(false);
			}
		}


	}

	/**
	 * metodo che controlla i personday in trouble dei dipendenti che non hanno timbratura fixed e invia mail nel caso in cui esistano
	 * timbrature disaccoppiate
	 * @param p
	 * @param begin
	 * @param end
	 * @throws EmailException
	 */
	private static void checkPersonDayForSendingEmail(Person p, LocalDate begin, LocalDate end, String cause) {

		if(p.surname.equals("Conti") && p.name.equals("Marco")) {
			
			Logger.debug("Trovato Marco Conti, capire cosa fare con la sua situazione...");
			return;
		}
		
		List<PersonDayInTrouble> pdList = PersonDayInTroubleDao.getPersonDayInTroubleInPeriod(p, begin, end, false);
//		List<PersonDayInTrouble> pdList = PersonDayInTrouble.find("Select pd from PersonDayInTrouble pd where pd.personDay.person = ? " +
//				"and pd.personDay.date between ? and ? and pd.fixed = ?", p, begin, end, false).fetch();

		List<LocalDate> dateTroubleStampingList = new ArrayList<LocalDate>();

		for(PersonDayInTrouble pdt : pdList){
			
			//Contract contract = p.getContract(pdt.personDay.date);
			Contract contract = ContractDao.getContract(pdt.personDay.date, pdt.personDay.person);
			if(contract == null) {
				
				Logger.error("Individuato PersonDayInTrouble al di fuori del contratto. Person: %s %s - Data: %s",
						p.surname, p.name, pdt.personDay.date);
				continue;
			}
			
			ContractStampProfile csp = ContractManager.getContractStampProfileFromDate(contract, pdt.personDay.date);
			if(csp.fixedworkingtime == true) {
				continue;
			}
			
			if(pdt.cause.contains(cause) && !pdt.personDay.isHoliday() && pdt.fixed == false) { 
				dateTroubleStampingList.add(pdt.personDay.date);
			}
		}



		boolean flag;
		try {

			flag = sendEmailToPerson(dateTroubleStampingList, p, cause);

		} catch (Exception e) {

			Logger.debug("sendEmailToPerson(dateTroubleStampingList, p, cause): fallito invio email per %s %s", p.name, p.surname); 
			e.printStackTrace();
			return;
		}

		//se ho inviato mail devo andare a settare 'true' i campi emailSent dei personDayInTrouble relativi 
		if(flag){
			for(PersonDayInTrouble pd : pdList){
				pd.emailSent = true;
				pd.save();
			}
		}


	}

	/**
	 * questo metodo viene invocato nell'expandableJob per controllare ogni due giorni la presenza di giorni in cui, per ogni dipendente,
	 * non ci siano nè assenze nè timbrature
	 * @param personId
	 * @param year
	 * @param month
	 * @param userLogged
	 * @throws EmailException 
	 */
	public static void checkNoAbsenceNoStamping(Long personId, int year, int month, User userLogged) throws EmailException{
		List<Person> personList = new ArrayList<Person>();
		LocalDate begin = null;
		LocalDate end = null;

		if(personId==-1)
			personId=null;
		if(personId==null)
		{
			begin = new LocalDate(year, month, 1);
			end = new LocalDate().minusDays(1);
			//personList = Person.getActivePersonsSpeedyInPeriod(begin, end, officeAllowed, false);
			personList = PersonDao.list(Optional.<String>absent(),
					OfficeDao.getOfficeAllowed(Optional.fromNullable(userLogged)), false, begin, end, true).list();
		}
		else
		{
			//TODO controllare che personLogged abbia i diritti sulla persona
			personList.add(PersonDao.getPersonById(personId));
			//personList.add((Person)Person.findById(personId));
		}
		for(Person p : personList){
			Logger.debug("Chiamato controllo sul giorni %s %s", begin, end);
			if(p.wantEmail)
				checkPersonDayForSendingEmail(p, begin, end, "no assenze");
			else
				Logger.info("Non verrà inviata la mail a %s %s in quanto il campo di invio mail è false", p.name, p.surname);

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
	public static void checkPersonDay(Long personid, LocalDate dayToCheck)
	{
		Person personToCheck = PersonDao.getPersonById(personid);
		//Person personToCheck = Person.findById(personid);
		//if(!personToCheck.isActiveInDay(dayToCheck)) {
		if(!PersonManager.isActiveInDay(dayToCheck, personToCheck)){
			return;
		}
		PersonDay personDay = null;
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(personToCheck, dayToCheck);
//		PersonDay pd = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? AND pd.date = ? ", 
//				personToCheck,dayToCheck).first();

		if(pd.isPresent()){
			PersonDayManager.checkForPersonDayInTrouble(pd.get()); 
			return;
		}
		else {
			personDay = new PersonDay(personToCheck, dayToCheck);
			if(personDay.isHoliday()) {
				return;
			}
			personDay.create();
			PersonDayManager.populatePersonDay(personDay);
			personDay.save();
			PersonDayManager.checkForPersonDayInTrouble(personDay);
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
	private static void checkHistoryError(Person person, int year, int month)
	{
		//Person person = Person.findById(personid);
		Logger.info("Check history error %s dal %s-%s-1 a oggi", person.surname, year, month);
		LocalDate date = new LocalDate(year,month,1);
		LocalDate today = new LocalDate();
		
		while(true) {
			
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			
			PersonUtility.checkPersonDay(person.id, date);
			date = date.plusDays(1);
			if(date.isEqual(today))
				break;
			
		}

	}


	/**
	 * invia la mail alla persona specificata in firma con la lista dei giorni in cui ha timbrature disaccoppiate
	 * @param date, person
	 * @throws EmailException 
	 */
	private static boolean sendEmailToPerson(List<LocalDate> dateList, Person person, String cause) throws EmailException{
		if(dateList.size() == 0){
			return false;
		}
		Logger.info("Preparo invio mail per %s %s", person.name, person.surname);
		SimpleEmail simpleEmail = new SimpleEmail();
		try {
			simpleEmail.setFrom("epas@iit.cnr.it");
			//simpleEmail.addReplyTo("segreteria@iit.cnr.it");
			simpleEmail.addReplyTo(ConfGeneralManager.getConfGeneralByField(
							ConfigurationFields.EmailToContact.description, 
							person.office).fieldValue);
		} catch (EmailException e1) {

			e1.printStackTrace();
		}
		try {
			simpleEmail.addTo(person.email);
			//simpleEmail.addTo("dario.tagliaferri@iit.cnr.it");
		} catch (EmailException e) {

			e.printStackTrace();
		}
		List<LocalDate> dateFormat = new ArrayList<LocalDate>();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-YYYY");		
		String date = "";
		for(LocalDate d : dateList){
			if(!DateUtility.isGeneralHoliday(person.office, d)){
				dateFormat.add(d);
				String str = fmt.print(d);
				date = date+str+", ";
			}
		}
		String incipit = "";
		if(dateFormat.size() == 0)
			return false;
		if(dateFormat.size() > 1)
			incipit = "Nei giorni: ";
		if(dateFormat.size() == 1)
			incipit = "Nel giorno: ";	

		simpleEmail.setSubject("ePas Controllo timbrature");
		String message = "";
		if(cause.equals("timbratura")){
			message = "Gentile " +person.name+" "+person.surname+ 
					"\r\n" + incipit+date+ " il sistema ePAS ha rilevato un caso di timbratura disaccoppiata. \r\n " +
					"La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione. \r\n" +
					"Saluti \r\n"+
					"Il team di ePAS";

		}
		if(cause.equals("no assenze")){
			message = "Gentile " +person.name+" "+person.surname+ 
					"\r\n" + incipit+date+ " il sistema ePAS ha rilevato un caso di mancanza di timbrature e di codici di assenza. \r\n " +
					"La preghiamo di contattare l'ufficio del personale per regolarizzare la sua posizione. \r\n" +
					"Saluti \r\n"+
					"Il team di ePAS";
		}

		simpleEmail.setMsg(message);
		
		Mail.send(simpleEmail);

		Logger.info("Inviata mail a %s %s contenente le date da controllare : %s", person.name, person.surname, date);
		return true;

	}

	/**
	 * 
	 * @return la lista dei codici competenza attivi per le persone nell'anno in corso
	 */
	public static List<CompetenceCode> activeCompetence(){
		List<CompetenceCode> competenceCodeList = new ArrayList<CompetenceCode>();
		List<Competence> competenceList = CompetenceDao.getCompetenceInYear(new LocalDate().getYear());
		//List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.year = ? order by comp.competenceCode.code", new LocalDate().getYear()).fetch();
		for(Competence comp : competenceList){
			if(!competenceCodeList.contains(comp.competenceCode))
				competenceCodeList.add(comp.competenceCode);
		}
		return competenceCodeList;
	}

}



