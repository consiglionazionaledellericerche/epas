package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.Query;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
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
import models.PersonDayInTrouble;
import models.PersonMonth;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.PersonYear;
import models.StampModificationType;
import models.StampProfile;
import models.Stamping;
import models.Stamping.WayType;
import models.VacationPeriod;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
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
		Logger.debug("Chiamata la checkExitStampNextDay per %s %s in data %s", pd.person.name, pd.person.surname, pd.date);
		Configuration config = Configuration.getCurrentConfiguration();
		//PersonDay pdPastDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
		//		"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", pd.person, pd.date.dayOfMonth().withMinimumValue(), pd.date).first();

		PersonDay pdPastDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
				"and pd.date < ? ORDER by pd.date DESC", pd.person, pd.date).first();

		StampProfile stampProfile = pd.getStampProfile();
		if(pdPastDay != null)
		{			
			//lista delle timbrature del giorno precedente ordinate in modo decrescente per vedere se l'ultima del giorno è una timbratura di ingresso
			//List<Stamping> reloadedStampingYesterday = Stamping.find("Select st from Stamping st where st.personDay = ? order by st.date desc", pdPastDay).fetch();
			Query query = JPA.em().createQuery("Select st from Stamping st where st.personDay = :pd order by st.date asc");
			query.setParameter("pd", pdPastDay);
			List<Stamping> reloadedStampingYesterday = query.getResultList();
			//	List<Stamping> reloadedStampingYesterday = new ArrayList<Stamping>(pdPastDay.stampings);
			int size = reloadedStampingYesterday.size();
			if(reloadedStampingYesterday.size() > 0 && reloadedStampingYesterday.get(size-1).way == WayType.in)
			{
				Logger.debug("Sono nel caso in cui ci sia una timbratura finale di ingresso nel giorno precedente nel giorno %s", pdPastDay.date);
				if(stampProfile == null || !stampProfile.fixedWorkingTime)
				{
					//List<Stamping> s = Stamping.find("Select s from Stamping s where s.personDay = ? order by s.date asc", pd).fetch();
					pd.orderStampings();
					if(pd.stampings.size() > 0 && pd.stampings.get(0).way == WayType.out && config.hourMaxToCalculateWorkTime > pd.stampings.get(0).date.getHourOfDay())
					{

						//controllo nelle timbrature del giorno attuale se la prima che incontro è una timbratura di uscita sulla base
						//del confronto con il massimo orario impostato in configurazione per considerarla timbratura di uscita relativa
						//al giorno precedente

						Logger.trace("Esiste una timbratura di uscita come prima timbratura del giorno %s", pd.date);
						//in caso esista quella timbratura di uscita come prima timbratura del giorno attuale, creo una nuova timbratura
						// di uscita e la inserisco nella lista delle timbrature relative al personDay del giorno precedente.
						//E svolgo i calcoli su tempo di lavoro, differenza e progressivo
						Stamping correctStamp = new Stamping();
						Logger.trace("Aggiungo una nuova timbratura di uscita al giorno precedente alla mezzanotte ");
						correctStamp.date = new LocalDateTime(pdPastDay.date.getYear(), pdPastDay.date.getMonthOfYear(), pdPastDay.date.getDayOfMonth(), 23, 59);
						correctStamp.way = WayType.out;
						correctStamp.markedByAdmin = false;
						//correctStamp.considerForCounting = true;
						correctStamp.stampModificationType = StampModificationType.findById(4l);
						correctStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
						correctStamp.personDay = pdPastDay;
						correctStamp.save();
						pdPastDay.stampings.add(correctStamp);
						pdPastDay.save();
						Logger.trace("Aggiunta nuova timbratura %s con valore %s", correctStamp, correctStamp.date);
						Logger.trace("Devo rifare i calcoli in funzione di questa timbratura aggiunta");


						pdPastDay.populatePersonDay();
						Logger.trace("Fatti i calcoli, ora aggiungo una timbratura di ingresso alla mezzanotte del giorno %s", pd.date);
						//a questo punto devo aggiungere una timbratura di ingresso prima della prima timbratura di uscita che è anche
						//la prima timbratura del giorno attuale
						Stamping newEntranceStamp = new Stamping();
						newEntranceStamp.date = new LocalDateTime(pd.date.getYear(), pd.date.getMonthOfYear(), pd.date.getDayOfMonth(),0,0);
						newEntranceStamp.way = WayType.in;
						newEntranceStamp.markedByAdmin = false;
						//newEntranceStamp.considerForCounting = true;
						newEntranceStamp.stampModificationType = StampModificationType.findById(4l);
						newEntranceStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
						newEntranceStamp.personDay = pd;
						newEntranceStamp.save();
						Logger.trace("Aggiunta la timbratura %s con valore %s", newEntranceStamp, newEntranceStamp.date);
						pd.stampings.add(newEntranceStamp);
						pd.save();
						//pd.populatePersonDay();


					}
					else
					{
						Logger.trace("La prima timbratura del giorno per %s per %s %s non è di uscita", pd.date, pd.person.name, pd.person.surname);
					}
				}
				else
				{
					Logger.trace("Non faccio i calcoli per l'uscita perchè c'è il tempo di lavoro giustificato per %s %s in data %s", 
							pd.person.name, pd.person.surname, pd.date);
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
	
	/**
	 * 
	 * @return il codice di assenza da utilizzare da scegliere tra 31,32 e 94 nel caso in cui l'utente amministratore utilizzi il codice "FER" per assegnare un giorno di ferie 
	 * alla persona
	 */
	public static AbsenceType whichVacationCode(Person person, Integer year, Integer month, Integer day){
		
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
		Logger.debug("Quest'anno %s %s ha usufruito di %d giorni di ferie", person.name, person.surname, absThisYearList.size());
		
		//FIXME: alcuni non hanno vacation period (abraham)
//		VacationPeriod vp = VacationPeriod.find("Select vp from VacationPeriod vp where vp.person = ? and ((vp.beginFrom <= ? and vp.endTo >= ?) " +
//				"or (vp.endTo = null)) order by vp.beginFrom desc", person, new LocalDate(year-1,1,1), new LocalDate(year-1,12,31)).first();
		VacationPeriod vp = person.getCurrentContract().getCurrentVacationPeriod();
		if((vp.vacationCode.vacationDays > absList.size() + absThisYearList.size()) && 
				(new LocalDate(year, month, day).isBefore(new LocalDate(year, config.monthExpiryVacationPastYear, config.dayExpiryVacationPastYear)))){
			return AbsenceType.find("byCode", "31").first();
		}
		AbsenceType permissionDay = AbsenceType.find("byCode", "94").first();
		
		query.setParameter("begin", new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue())
			.setParameter("end", new LocalDate(year, month, day)).setParameter("person", person).setParameter("type", permissionDay);
		List<Absence> absPermissions = query.getResultList();
		Logger.debug("%s %s quest'anno ha usufruito di %d giorni di permesso", person.name, person.surname, absPermissions.size());
		if(vp.vacationCode.permissionDays > absPermissions.size()){
			return permissionDay;
		}
		/**
		 * bisognerebbe fare il calcolo in base a quanti giorni di ferie sono maturati alla data in cui si chiede l'inserimento dell'assenza
		 */
		query.setParameter("begin", new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue())
			.setParameter("end", new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()).setParameter("person", person).setParameter("type", vacationFromThisYear);
		List<Absence> absVacationThisYear = query.getResultList();
		if(absVacationThisYear.size() <= vp.vacationCode.vacationDays)
			return AbsenceType.find("byCode", "32").first();
		else
			return null;
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
				
				totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
				
			}
			Logger.debug("TotalMinutesJustified= %d. Minuti giustificati: %d", totalMinutesJustified, absenceType.justifiedTimeAtWork.minutesJustified);
			if(absenceType.absenceTypeGroup.limitInMinute >= totalMinutesJustified+absenceType.justifiedTimeAtWork.minutesJustified)
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
	 * Verifica per la persona (se attiva) che alla data 
	 * 	(1) in caso di giorno lavorativo il person day esista. 
	 * 		Altrimenti viene creato e persistito un personday vuoto e inserito un record nella tabella PersonDayInTrouble.
	 * 	(2) il person day presenti una situazione di timbrature corretta dal punto di vista logico. 
	 * 		In caso contrario viene inserito un record nella tabella PersonDayInTrouble. Situazioni di timbrature errate si verificano nei casi 
	 *  	(a) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 		(b) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature. 
	 * @param personid la persona da controllare, null se si desidera controllare tutte le persone attive
	 * @param dayToCheck il giorno da controllare
	 */
	public static void checkDay(Long personid, LocalDate dayToCheck)
	{
		
		Logger.info("Lanciata checkDay per personid %s e giorno %s" ,personid+"", dayToCheck);
		//Costruisco la lista delle persone da controllare
		List<Person> activeList = new ArrayList<Person>();
		if(personid==null)
		{
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			activeList =  Person.getActivePersons(new LocalDate());
		}
		else
		{
			JPAPlugin.closeTx(false);
			JPAPlugin.startTx(false);
			Person person = Person.findById(personid);
			if(person.isActive(dayToCheck))
			{
				activeList.add(person);
			}
			else
			{
				return;
			}
		}
		
		
		for(Person personToCheck : activeList)
		{
			PersonDay pd = PersonDay.find(""
					+ "SELECT pd "
					+ "FROM PersonDay pd "
					+ "WHERE pd.person = ? AND pd.date = ? ", 
					personToCheck, 
					dayToCheck)
					.first();
			
			if(pd!=null)
			{
				Logger.info("personToChek nel metodo checkday %s %s %s con PersonDay !null", personToCheck.id, personToCheck.surname, personToCheck.name);
				
				//check for error
				checkForError(pd, personToCheck); //TODO riabilitarlo
				continue;
			}
			else
			{
				if(DateUtility.isGeneralHoliday(dayToCheck))
				{
					continue;
				}
				if(personToCheck.workingTimeType.workingTimeTypeDays.get(dayToCheck.getDayOfWeek()-1).holiday)
				{
					continue;
				}
				
				Logger.info("personToChek nel metodo checkday %s %s %s con PersonDay null", personToCheck.id, personToCheck.surname, personToCheck.name);
				
				pd = new PersonDay(personToCheck, dayToCheck);
				pd.create();
				pd.populatePersonDay();
				pd.save();
				//check for error
				checkForError(pd, personToCheck);
				continue;
				
			}
		}
	}
	
	/**
	 * Verifica che nel person day vi sia una situazione coerente di timbrature. Situazioni errate si verificano nei casi 
	 *  (1) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 	(2) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature. 
	 * In caso di situazione errata viene aggiunto un record nella tabella PersonDayInTrouble.
	 * @param pd
	 * @param person
	 */
	private static void checkForError(PersonDay pd, Person person)
	{
		Logger.info("Check For Error %s %s %s alla data", person.id, person.surname, pd.date);
		//persona fixed
		StampModificationType smt = pd.getFixedWorkingTime();
		if(smt !=null)
		{
			if(pd.stampings.size()!=0)
			{
				pd.computeValidStampings();
				for(Stamping s : pd.stampings)
				{
					if(!s.valid)
					{
						Logger.info("Check For Error1");
						insertPersonDayInTrouble(pd, "timbratura disaccoppiata");
						return;
					}
				}
			}			
		}
		//persona not fixed
		else
		{
			if(!pd.isAllDayAbsences() && pd.stampings.size()==0)
			{
				if(!pd.isHoliday())	
					//TODO questo e' un controllo aggiuntivo in quanto in teoria i person day senza assenze e timbrature nei giorni di festa 
					//non dovrebbero esistere ma nel database attuale a volte sono presenti e persistiti. Cancellarli e togliere questo controllo
				{
					Logger.info("Check For Error2");
					insertPersonDayInTrouble(pd, "no assenze giornaliere e no timbrature");
				}
				return;
			}
			pd.computeValidStampings();
			for(Stamping s : pd.stampings)
			{
				if(!s.valid)
				{
					Logger.info("Check For Error3");
					insertPersonDayInTrouble(pd, "timbratura disaccoppiata");
					return;
				}
			}
		}
		//giorno senza problemi, se era in trouble lo fixo
		if(pd.troubles!=null && pd.troubles.size()>0)
		{
			for(PersonDayInTrouble pdt : pd.troubles)
			{
				Logger.info("Il problema %s %s %s e' risultato da fixare", pd.date, pd.person.surname, pd.person.name);
				pdt.fixed = true;
				pdt.save();
				
			}
		}
	}
	
	private static void insertPersonDayInTrouble(PersonDay pd, String cause)
	{
		//TODO Controllo che non esista già, in quel caso decidere cosa fare (forse solo aggiornare la causa)
		//System.out.println( "A " + pd.date.toString() +  " " + person.surname + " " +person.name +" non valido. (cella gialla)");
		
		PersonDayInTrouble pdt = PersonDayInTrouble.find(""
				+ "Select pdt "
				+ "from PersonDayInTrouble pdt "
				+ "where pdt.personDay = ?"
				, pd)
				.first();
		
		if(pdt==null)
		{	
			//se non esiste lo creo
			Logger.info("Nuovo PersonDayInTrouble %s %s %s - %s - %s", pd.person.id, pd.person.name, pd.person.surname, pd.date, cause);
			PersonDayInTrouble trouble = new PersonDayInTrouble();
			trouble.personDay = pd;
			trouble.cause = cause;
			trouble.save();
			return;
		}
		
		if(pdt!=null)
		{
			//se esiste lo setto fixed = false;
			pdt.fixed = false;
			pdt.cause = cause;
			pdt.save();
		}
		
	}
	
	
	/**
	 * A partire dal mese e anno passati al metodo fino al giorno di ieri 
	 * controlla la presenza di errori nelle timbrature 
	 * ed eventualmente inserisce i giorni problematici nella tabella PersonDayInTrouble.
	 * @param personid la persona da controllare, null se si vuole controllare ogni persona
	 * @param year l'anno di partenza
	 * @param month il mese di partenza
	 */
	public static void checkHistoryError(Long personid, int year, int month)
	{
		Logger.info("Check history error for personid %s month %s", personid+"", month);
		LocalDate date = new LocalDate(year,month,1);
		LocalDate today = new LocalDate();
		while(true)
		{
			if(personid==null)
				PersonUtility.checkDay(null, date);
			else
				PersonUtility.checkDay(personid, date);
			date = date.plusDays(1);
			if(date.isEqual(today))
				break;
		}
	}
	
	

}




