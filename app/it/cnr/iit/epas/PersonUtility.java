package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import manager.PersonDayManager;
import manager.PersonManager;
import models.Absence;
import models.AbsenceType;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.Stamping;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonChildrenDao;
import dao.PersonDao;

public class PersonUtility {

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



