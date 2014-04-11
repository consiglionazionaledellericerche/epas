package models.rendering;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.ConfYear;
import models.Contract;
import models.ContractYearRecap;
import models.Person;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import play.Logger;

/**
 * @author alessandro
 * Classe da utilizzare per il riepilogo delle informazioni relative al piano ferie di una persona.
 */
public class VacationsRecap {
	
	public Person person;
	public int year;
	public Contract activeContract = null;
	public List<VacationPeriod>vacationPeriodList = null;	//lista vacation period del current contract
	
	public List<Absence> vacationDaysLastYearUsed = new ArrayList<Absence>();
	public List<Absence> vacationDaysCurrentYearUsed = new ArrayList<Absence>();
	public List<Absence> permissionUsed = new ArrayList<Absence>();
	
	public Integer vacationDaysLastYearAccrued = 0;
	public Integer vacationDaysCurrentYearAccrued = 0;
	public Integer permissionCurrentYearAccrued = 0;
	
	public Integer vacationDaysLastYearNotYetUsed = 0;
	public Integer vacationDaysCurrentYearNotYetUsed = 0;
	public Integer persmissionNotYetUsed = 0;
		
	public Integer vacationDaysCurrentYearTotal = 0;
	public Integer permissionCurrentYearTotal = 0;

	
	/**
	 * La situazione sui residui e maturazioni di ferie e permessi nell'anno.
	 * @param person
	 * @param year l'anno in considerazione
	 * @param actualDate la data specifica nell'anno in cui si desidera fotografare la situazione, tipicamente oggi
	 * @param considerExpireLastYear impostare true se non si vuole considerare il limite di scadenza per l'utilizzo
	 * delle ferie dell'anno precedente (utile per assegnare il codice 37)
	 */
	public VacationsRecap(Person person, int year, Contract contract, LocalDate actualDate, boolean considerExpireLastYear)
	{
		
		this.person = person;
		this.year = year;
		
		//active contract
		this.activeContract = contract;
		if(activeContract == null)
			return;
	
		//vacation periods list
		this.vacationPeriodList = this.activeContract.getContractVacationPeriods();
		if(vacationPeriodList == null)
			return;
		
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);	
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);	
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);
		DateInterval lastYearInter = new DateInterval(startLastYear, endLastYear);
		DateInterval yearInter = new DateInterval(startYear, endYear);
		DateInterval yearActualDateInter = new DateInterval(startYear, actualDate);

		AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab31 = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab37 = AbsenceType.getAbsenceTypeByCode("37");
		AbsenceType ab94 = AbsenceType.getAbsenceTypeByCode("94");
		
		ConfYear conf = ConfYear.getConfYear(year);
		LocalDate expireVacation = actualDate.withMonthOfYear(conf.monthExpiryVacationPastYear).withDayOfMonth(conf.dayExpiryVacationPastYear);
		
		//(1) Calcolo ferie usate dell'anno passato ---------------------------------------------------------------------------------------------------------------------------------
		List<Absence> abs32Last = null;
		List<Absence> abs31Last = null;
		List<Absence> abs37Last = null;
		
		int vacationDaysPastYearUsedNew = 0;
		if(activeContract.sourceDate!=null && activeContract.sourceDate.getYear()==year)
		{
			//Popolare da source data
			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + activeContract.sourceVacationLastYearUsed;
			DateInterval yearInterSource = new DateInterval(activeContract.sourceDate.plusDays(1), endYear);
			abs31Last = getVacationDays(yearInterSource, activeContract, ab31);										
			abs37Last = getVacationDays(yearInterSource, activeContract, ab37);										
			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
		}
		else if(activeContract.beginContract.getYear()==this.year)
		{
			//Non esiste anno passato nel presente contratto
			vacationDaysPastYearUsedNew = 0;
		}
		else
		{
			//Popolare da contractYearRecap
			ContractYearRecap recapPastYear = contract.getContractYearRecap(year-1);
			vacationDaysPastYearUsedNew = recapPastYear.vacationCurrentYearUsed;
			abs31Last = getVacationDays(yearInter, activeContract, ab31);						
			abs37Last = getVacationDays(yearInter, activeContract, ab37);						
			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
		}
		//costruisco la lista delle ferie per stampare le date (prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
		abs32Last = getVacationDays(lastYearInter, activeContract, ab32);
		abs31Last = getVacationDays(yearInter, activeContract, ab31);
		abs37Last = getVacationDays(yearInter, activeContract, ab37);
		this.vacationDaysLastYearUsed.addAll(abs32Last);
		this.vacationDaysLastYearUsed.addAll(abs31Last);
		this.vacationDaysLastYearUsed.addAll(abs37Last);
		while(this.vacationDaysLastYearUsed.size()<vacationDaysPastYearUsedNew)
		{
			Logger.debug("Inserita assenza nulla");
			Absence nullAbsence = null;
			this.vacationDaysLastYearUsed.add(nullAbsence);
		}
		
		if(this.vacationDaysLastYearUsed.size()==vacationDaysPastYearUsedNew)
			Logger.debug("Ok per %s %s ",person.name, person.surname);
		else
			Logger.debug("Nok %s %s", person.name, person.surname);
		
		
		//(2) Calcolo ferie usate dell'anno corrente ---------------------------------------------------------------------------------------------------------------------------------
		List<Absence> abs32Current = null;
		/**
		 * controllo che nell'anno ci siano dei codici di assenza di tipo 24/24S/25 (per ora abbastanza limitato eventualmente da rivedere)
		 * di modo da conteggiarli come giorni di ferie utilizzati nell'anno
		 */
		List<Absence> abs2425Current = Absence.find("Select abs from Absence abs where abs.absenceType.code in (?,?,?) " +
				"and abs.personDay.person = ? and abs.personDay.date between ? and ?", "24", "24S", "25", this.person, startYear, endYear).fetch();
		Logger.debug("Giorni di codice 24, 25 o simili per %s %s: %d", person.name, person.surname, abs2425Current.size());
		
		int vacationDaysCurrentYearUsedNew = 0;
		if(activeContract.sourceDate!=null && activeContract.sourceDate.getYear()==year)
		{
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + activeContract.sourceVacationCurrentYearUsed;
			DateInterval yearInterSource = new DateInterval(activeContract.sourceDate.plusDays(1), endYear);
			abs32Current = getVacationDays(yearInterSource, activeContract, ab32);										
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size() + abs2425Current.size();
		}
		else
		{
			abs32Current = getVacationDays(yearInter, activeContract, ab32);
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size() + abs2425Current.size();
		}
		this.vacationDaysCurrentYearUsed.addAll(abs32Current);
		if(abs2425Current.size() > 0)
			this.vacationDaysCurrentYearUsed.addAll(abs2425Current);
		while(this.vacationDaysCurrentYearUsed.size()<vacationDaysCurrentYearUsedNew)
		{
			Logger.debug("Inserita assenza nulla");
			Absence nullAbsence = null;
			this.vacationDaysCurrentYearUsed.add(nullAbsence);
		}
		
		//(3) Calcolo permessi usati dell'anno corrente
		List<Absence> abs94Current = null;
		int permissionCurrentYearUsedNew = 0;
		
		if(activeContract.sourceDate!=null && activeContract.sourceDate.getYear()==year)
		{
			permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + activeContract.sourcePermissionUsed;
			DateInterval yearInterSource = new DateInterval(activeContract.sourceDate.plusDays(1), endYear);
			abs94Current = getVacationDays(yearInterSource, activeContract, ab94);
			permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + abs94Current.size();
		}
		else
		{
			abs94Current = getVacationDays(yearInter, activeContract, ab94);
			permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + abs94Current.size();
		}
		this.permissionUsed.addAll(abs94Current);
		while(this.permissionUsed.size()<permissionCurrentYearUsedNew)
		{
			Logger.debug("Inserita assenza nulla");
			Absence nullAbsence = null;
			this.permissionUsed.add(nullAbsence);
		}
		

		//(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente (sono indipendenti dal database)
		this.vacationDaysLastYearAccrued = getVacationAccruedYear(lastYearInter, this.activeContract, this.vacationPeriodList);
		if(endYear.isAfter(actualDate))
		{
			//se la query e' per l'anno corrente considero fino a actualDate
			this.permissionCurrentYearAccrued = getPermissionAccruedYear( yearActualDateInter, this.activeContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear( yearActualDateInter, this.activeContract, this.vacationPeriodList);
		}
		else
		{
			//se la query e' per gli anni passati considero fino a endYear
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(yearInter, this.activeContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(yearInter, this.activeContract, this.vacationPeriodList);
		}
		
		//(5)Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente (sono funzione di quanto calcolato precedentemente)
		//Anno passato
		if(actualDate.isBefore(expireVacation) || !considerExpireLastYear)
			this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued - this.vacationDaysLastYearUsed.size();
		else
			this.vacationDaysLastYearNotYetUsed = 0;
		//Anno corrente
		this.permissionCurrentYearTotal = getPermissionAccruedYear(yearInter, this.activeContract);
		this.vacationDaysCurrentYearTotal = getVacationAccruedYear(yearInter, this.activeContract, this.vacationPeriodList);	
		this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal - this.vacationDaysCurrentYearUsed.size();									//per adesso quelli non utilizzati li considero tutti
		this.persmissionNotYetUsed = this.permissionCurrentYearTotal - this.permissionUsed.size();
	}
	
	
	/**
	 * 
	 * @param yearInterval
	 * @param contract
	 * @param vacationPeriodList
	 * @return il numero di giorni di ferie maturati nell'anno year 
	 * 	calcolati a partire dai piani ferie associati al contratto corrente
	 */
	public int getVacationAccruedYear(DateInterval yearInterval, Contract contract, List<VacationPeriod>vacationPeriodList){
		
		int vacationDays = 0;

		//Calcolo l'intersezione fra l'anno e il contratto attuale
		yearInterval = DateUtility.intervalIntersection(yearInterval, new DateInterval(contract.beginContract, contract.expireContract));
		if(yearInterval == null)
			return 0;
		
		//per ogni piano ferie conto i giorni trascorsi in yearInterval e applico la funzione di conversione
		for(VacationPeriod vp : vacationPeriodList)
		{
			int days = 0;
			DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
			DateInterval intersection = DateUtility.intervalIntersection(vpInterval, yearInterval);
			if(intersection!=null)
			{
				days = DateUtility.daysInInterval(intersection);
			}
			
			//calcolo i giorni maturati col metodo di conversione
			if(vp.vacationCode.description.equals("26+4"))
			{
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);
			}
			if(vp.vacationCode.description.equals("28+4"))
			{
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysMoreThreeYears(days);
			}
			
		}
		
		return vacationDays;

	}

	
	

	/**
	 * 
	 * @param yearInterval
	 * @param contract
	 * @return numero di permessi maturati nel periodo yearInterval associati a contract
	 */
	public int getPermissionAccruedYear(DateInterval yearInterval, Contract contract){
		int days = 0;
		int permissionDays = 0;
	
		//Calcolo l'intersezione fra l'anno e il contratto attuale
		yearInterval = DateUtility.intervalIntersection(yearInterval, new DateInterval(contract.beginContract, contract.expireContract));
		if(yearInterval == null)
			return 0;
		
		days = yearInterval.getEnd().getDayOfYear() - yearInterval.getBegin().getDayOfYear();
		permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}
	
	
	/**
	 * 
	 * @param inter
	 * @param contract
	 * @param ab
	 * @return la lista di assenze effettuate dal titolare del contratto del tipo ab nell'intervallo temporale inter
	 */
	public static List<Absence> getVacationDays(DateInterval inter, Contract contract, AbsenceType ab)
	{
		
			
		//calcolo inizio fine a seconda del contratto
		if(inter.getBegin().isBefore(contract.beginContract))
		{
			inter = new DateInterval(contract.beginContract, inter.getEnd());
		}
		if(contract.expireContract!=null && inter.getEnd().isAfter(contract.expireContract))
		{
			inter = new DateInterval(inter.getBegin(), contract.expireContract);
		}
		
		List<Absence> absences = Absence.find(
				"SELECT ab "
						+ "FROM Absence ab "
						+ "WHERE ab.personDay.person = ? AND ( ab.personDay.date between ? AND ? ) AND ab.absenceType.code = ? order by ab.personDay.date",
						contract.person, inter.getBegin(), inter.getEnd(), ab.code).fetch();
		
		return absences;	

	}
	
	/**
	 * Il numero di giorni di ferie dell'anno passato non ancora utilizzati (senza considerare l'expire limit di utilizzo)
	 * Il valore ritornato contiene i giorni ferie maturati previsti dal contratto nell'anno passato meno 
	 * i 32 utilizzati in past year
	 * i 31 utilizzati in current year
	 * i 37 utilizzati in current year
	 * @param year
	 * @param person
	 * @param abt
	 * @return
	 */
	public static int remainingPastVacationsAs37(int year, Person person){
		Contract contract = person.getCurrentContract();
		VacationsRecap vc = new VacationsRecap(person, year, contract, new LocalDate(), false);
		return vc.vacationDaysLastYearNotYetUsed;
		
	}
	
	/**
	 * Progressione maturazione ferie e permessi nell'arco dell'anno
	 */
	private static class VacationsPermissionsDaysAccrued {

		/**
		 * 
		 * @param days
		 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in 
		 * istituto da meno di tre anni
		 */
		public static int convertWorkDaysToVacationDaysLessThreeYears(int days){
			
			if(days<=0)
				return 0;
			
			if(days >= 1 && days <= 15)
				return 1;
			if(days >= 16 && days <= 30)
				return 2;
			if(days >= 31 && days <= 45)
				return 3;
			if(days >= 46 && days <= 60)
				return 4;
			if(days >= 61 && days <= 75)
				return 5;
			if(days >= 76 && days <= 90)
				return 6;
			if(days >= 91 && days <= 105)
				return 7;
			if(days >= 106 && days <= 120)
				return 9;
			if(days >= 121 && days <= 135)
				return 10;
			if(days >= 136 && days <= 150)
				return 11;
			if(days >= 151 && days <= 165)
				return 12;
			if(days >= 166 && days <= 180)
				return 13;
			if(days >= 181 && days <= 195)
				return 14;
			if(days >= 196 && days <= 210)
				return 15;
			if(days >= 211 && days <= 225)
				return 16;
			if(days >= 226 && days <= 240)
				return 17;
			if(days >= 241 && days <= 255)
				return 18;
			if(days >= 256 && days <= 270)
				return 19;
			if(days >= 271 && days <= 285)
				return 20;
			if(days >= 286 && days <= 300)
				return 21;
			if(days >= 301 && days <= 315)
				return 22;
			if(days >= 316 && days <= 330)
				return 24;
			if(days >= 331 && days <= 345)
				return 25;
			else
				return 26;
			
		}

		/**
		 * 
		 * @param days
		 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in
		 * istituto da più di tre anni
		 */
		public static int convertWorkDaysToVacationDaysMoreThreeYears(int days){
			if(days<=0)
				return 0;
			
			if(days >= 1 && days <= 15)
				return 1;
			if(days >= 16 && days <= 30)
				return 2;
			if(days >= 31 && days <= 45)
				return 3;
			if(days >= 46 && days <= 60)
				return 5;
			if(days >= 61 && days <= 75)
				return 6;
			if(days >= 76 && days <= 90)
				return 7;
			if(days >= 91 && days <= 105)
				return 8;
			if(days >= 106 && days <= 120)
				return 9;
			if(days >= 121 && days <= 135)
				return 10;
			if(days >= 136 && days <= 150)
				return 12;
			if(days >= 151 && days <= 165)
				return 13;
			if(days >= 166 && days <= 180)
				return 14;
			if(days >= 181 && days <= 195)
				return 15;
			if(days >= 196 && days <= 210)
				return 16;
			if(days >= 211 && days <= 225)
				return 17;
			if(days >= 226 && days <= 240)
				return 18;
			if(days >= 241 && days <= 255)
				return 20;
			if(days >= 256 && days <= 270)
				return 21;
			if(days >= 271 && days <= 285)
				return 22;
			if(days >= 286 && days <= 300)
				return 23;
			if(days >= 301 && days <= 315)
				return 24;
			if(days >= 316 && days <= 330)
				return 25;
			if(days >= 331 && days <= 345)
				return 26;
			else
				return 28;
			
		}

		/**
		 * 
		 * @param days
		 * @return il numero di giorni di permesso legge spettanti al dipendente a seconda dei giorni di presenza
		 */
		public static int convertWorkDaysToPermissionDays(int days){
			int permissionDays = 0;
			if(days >= 45 && days <= 135)
				permissionDays = 1;
			if(days >= 136 && days <= 225)
				permissionDays = 2;
			if(days >= 226 && days <= 315)
				permissionDays = 3;
			if(days >= 316 && days <= 365)
				permissionDays = 4;
			return permissionDays;
		}
		
	}


}
