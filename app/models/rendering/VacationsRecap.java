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
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import play.Logger;

/**
 * @author alessandro
 * Classe da utilizzare per il riepilogo delle informazioni relative al piano ferie di una persona.
 */
public class VacationsRecap {
	
	public Person person;
	public int year;
	public Contract activeContract = null;
	public DateInterval activeContractInterval = null;
	
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
	
	public boolean isExpireLastYear = false;		/* true se le ferie dell'anno passato sono scadute */
  
	public boolean isExpireBeforeEndYear = false;	/* true se il contratto termina prima della fine dell'anno richiesto */
	public boolean isActiveAfterBeginYear = false;	/* true se il contratto inizia dopo l'inizio dell'anno richiesto */
	 
	
	/**
	 * La situazione sui residui e maturazioni di ferie e permessi nell'anno. 
	 * @param person
	 * @param year l'anno in considerazione
	 * @param actualDate la data specifica dell'anno attuale in cui si desidera fotografare la situazione in termini di ferie e permessi maturati (tipicamente oggi)
	 * @param considerExpireLastYear impostare false se non si vuole considerare il limite di scadenza per l'utilizzo
	 * delle ferie dell'anno precedente (utile per assegnare il codice 37)
	 * @throws IllegalStateException nel caso in cui la persona non abbia sufficienti riepiloghi annuali dovuti dalla mancanza di dati di inizializzazione
	 * 
	 */
	public VacationsRecap(Person person, int year, Contract contract, LocalDate actualDate, boolean considerExpireLastYear) throws IllegalStateException
	{
		
		this.person = person;
		this.year = year;
		
		//active contract
		this.activeContract = contract;
		if(activeContract == null)
			return;
		
		this.activeContractInterval = this.activeContract.getContractDateInterval();
		
		//vacation periods list
		this.vacationPeriodList = this.activeContract.getContractVacationPeriods();
		if(vacationPeriodList == null)
			return;
		
		LocalDate today = LocalDate.now();
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);	
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);	
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);
		DateInterval lastYearInter = new DateInterval(startLastYear, endLastYear);
		DateInterval yearInter = new DateInterval(startYear, endYear);
		DateInterval yearActualDateInter = new DateInterval(startYear, actualDate);

		AbsenceType ab32  = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab31  = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab37  = AbsenceType.getAbsenceTypeByCode("37");
		AbsenceType ab94  = AbsenceType.getAbsenceTypeByCode("94");

		//Expire Last Year
		
		//ConfYear conf = ConfYear.getConfYear(year);
		Integer monthExpiryVacationPastYear = Integer.parseInt(ConfYear.getFieldValue("month_expiry_vacation_past_year", year, person.office));
		Integer dayExpiryVacationPastYear = Integer.parseInt(ConfYear.getFieldValue("day_expiry_vacation_past_year", year, person.office));
		LocalDate expireVacation = actualDate.withMonthOfYear(monthExpiryVacationPastYear).withDayOfMonth(dayExpiryVacationPastYear);
		
		this.isExpireLastYear = false;
		
		if( this.year < today.getYear() ) {		//query anni passati 
			this.isExpireLastYear = true;
		}
		else if( this.year == today.getYear() && actualDate.isAfter(expireVacation)) {	//query anno attuale
			this.isExpireLastYear = true;
		}
		
		//Expire Before End Of Year / Active After Begin Of Year
		
		if(this.activeContractInterval.getEnd().isBefore(endYear))
			this.isExpireBeforeEndYear = true;
		if(this.activeContractInterval.getBegin().isAfter(startYear))
			this.isActiveAfterBeginYear = true;
		
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
			abs31Last  = new ArrayList<Absence>();
			abs37Last  = new ArrayList<Absence>();
		}
		else if(this.year > LocalDate.now().getYear()) 
		{
			//Caso in cui voglio inserire ferie per l'anno prossimo
			VacationsRecap vrPastYear = new VacationsRecap(this.person, this.year-1, this.activeContract, endLastYear, true);

			abs31Last = getVacationDays(yearInter, activeContract, ab31);						
			abs37Last = getVacationDays(yearInter, activeContract, ab37);		
			vacationDaysPastYearUsedNew = vrPastYear.vacationDaysCurrentYearUsed.size() + abs31Last.size() + abs37Last.size();
		}
		else
		{
			//Popolare da contractYearRecap
			ContractYearRecap recapPastYear = contract.getContractYearRecap(year-1);
			if(recapPastYear==null)
				throw new IllegalStateException("Mancano i riepiloghi annuali.");
			vacationDaysPastYearUsedNew = recapPastYear.vacationCurrentYearUsed;
			abs31Last = getVacationDays(yearInter, activeContract, ab31);						
			abs37Last = getVacationDays(yearInter, activeContract, ab37);						
			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
		}
		//costruisco la lista delle ferie per stampare le date (prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
		abs32Last  = getVacationDays(lastYearInter, activeContract, ab32);

		this.vacationDaysLastYearUsed.addAll(abs32Last);
		this.vacationDaysLastYearUsed.addAll(abs31Last);
		this.vacationDaysLastYearUsed.addAll(abs37Last);
		while(this.vacationDaysLastYearUsed.size()<vacationDaysPastYearUsedNew)
		{
			Logger.debug("Inserita assenza nulla");
			Absence nullAbsence = null;
			this.vacationDaysLastYearUsed.add(nullAbsence);
		}
		
		
		//(2) Calcolo ferie usate dell'anno corrente ---------------------------------------------------------------------------------------------------------------------------------
		List<Absence> abs32Current  = null;
		
		int vacationDaysCurrentYearUsedNew = 0;
		if(activeContract.sourceDate!=null && activeContract.sourceDate.getYear()==year)
		{
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + activeContract.sourceVacationCurrentYearUsed;
			DateInterval yearInterSource = new DateInterval(activeContract.sourceDate.plusDays(1), endYear);
			abs32Current = getVacationDays(yearInterSource, activeContract, ab32);
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size();
		}
		else
		{
			abs32Current = getVacationDays(yearInter, activeContract, ab32);
			vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size();
		}
		this.vacationDaysCurrentYearUsed.addAll(abs32Current);
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
		if(contract.expireContract != null) 
		{
			//per i detereminati considero le maturate (perchè potrebbero decidere di cambiare contratto)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearAccrued - this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearAccrued - this.permissionUsed.size();
		}
		else 
		{
			//per gli indeterminati le considero tutte (è più improbabile....)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal - this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearTotal - this.permissionUsed.size();
		}
		
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
		List<Absence> abs24Current  = null;
		List<Absence> abs24SCurrent = null;
		List<Absence> abs25Current  = null;
		AbsenceType ab24  = AbsenceType.getAbsenceTypeByCode("24");
		AbsenceType ab24S = AbsenceType.getAbsenceTypeByCode("24S");
		AbsenceType ab25  = AbsenceType.getAbsenceTypeByCode("25");
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
			abs24Current = getVacationDays(intersection, activeContract, ab24);
			abs24SCurrent = getVacationDays(intersection, activeContract, ab24S);
			abs25Current = getVacationDays(intersection, activeContract, ab25);
			//calcolo i giorni maturati col metodo di conversione
			if(vp.vacationCode.description.equals("26+4"))
			{
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days-abs24Current.size()-abs25Current.size()-abs24SCurrent.size());
			}
			if(vp.vacationCode.description.equals("28+4"))
			{
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysMoreThreeYears(days-abs24Current.size()-abs25Current.size()-abs24SCurrent.size());
			}
			
		}
		
		//FIXME decidere se deve essere un parametro di configurazione
		if(vacationDays>28)
			vacationDays = 28;
		
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
		
		DateInterval contractInterInterval = DateUtility.intervalIntersection(inter, contract.getContractDateInterval());
		if(contractInterInterval==null)
			return new ArrayList<Absence>();
		
		/*
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
		*/
		
		
		List<Absence> absences = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(contract.person), Optional.fromNullable(ab.code), 
				contractInterInterval.getBegin(), contractInterInterval.getEnd(), Optional.<JustifiedTimeAtWork>absent(), false, true);
//		List<Absence> absences = Absence.find(
//				"SELECT ab "
//						+ "FROM Absence ab "
//						+ "WHERE ab.personDay.person = ? AND ( ab.personDay.date between ? AND ? ) AND ab.absenceType.code = ? order by ab.personDay.date",
//						contract.person, contractInterInterval.getBegin(), contractInterInterval.getEnd(), ab.code).fetch();
		
		
		return absences;	

	}
	
	/**
	 * 
	 * @param year l'anno per il quale vogliamo capire se le ferie dell'anno precedente sono scadute
	 * @param expireDate l'ultimo giorno utile per usufruire delle ferie dell'anno precedente
	 * @return
	 */
	public static boolean isVacationsLastYearExpired(int year, LocalDate expireDate)
	{
		LocalDate today = LocalDate.now();
		
		if( year < today.getYear() ) {		//query anni passati 
			return true;
		}
		else if( year == today.getYear() && today.isAfter(expireDate)) {	//query anno attuale
			return true;
		}
		return false;
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
		
		VacationsRecap vr = null;
		try {
			vr = new VacationsRecap(person, year, contract, new LocalDate(), false);
		}
		catch(IllegalStateException e){
			return 0;
		}
		
		return vr.vacationDaysLastYearNotYetUsed;
		
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
