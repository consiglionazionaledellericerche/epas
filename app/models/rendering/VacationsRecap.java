package models.rendering;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import manager.ConfYearManager;
import manager.ContractManager;
import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.ContractYearRecap;
import models.Person;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import play.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;

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

	
	public static class Factory {
		
		/**
		 * @param person
		 * @param year l'anno in considerazione
		 * @param contract Optional.Se assente viene considerato il contratto attualmente attivo della persona
		 * @param actualDate la data specifica dell'anno attuale in cui si desidera fotografare la situazione in termini di ferie e permessi maturati (tipicamente oggi)
		 * @param considerExpireLastYear impostare false se non si vuole considerare il limite di scadenza per l'utilizzo
		 * @return
		 */
		public static VacationsRecap build(Person person, int year, Optional<Contract> contract,
				LocalDate actualDate, boolean considerExpireLastYear) {
			
			Preconditions.checkNotNull(person);
			Preconditions.checkNotNull(year);
			Preconditions.checkNotNull(contract);
			Preconditions.checkNotNull(actualDate);
			
			VacationsRecap vr = new VacationsRecap();
			vr.person = person;
			vr.year = year;
			vr.activeContract = contract.get();//.or(person.getCurrentContract());
			
			Verify.verify(vr.activeContract != null, "non c'è contratto attivo!");
			
			List<VacationPeriod> vacationPeriodList = ContractManager.getContractVacationPeriods(vr.activeContract);
			
			//FIXME Se non ho i piani ferie li costruisco. Decidere dove metterli (bootstrap??)
			//il riepilogo non dovrebbe modificare il db
			if(vacationPeriodList==null || vacationPeriodList.isEmpty()) {
				ContractManager.properContractUpdate(vr.activeContract);
				vacationPeriodList = ContractManager.getContractVacationPeriods(vr.activeContract);
			}
			Verify.verify(vacationPeriodList!=null && !vacationPeriodList.isEmpty(), "Nessun piano ferie presente per il contratto!");
			
			vr.activeContractInterval = vr.activeContract.getContractDateInterval();
			vr.vacationPeriodList = vacationPeriodList;
			
			LocalDate today = LocalDate.now();
			LocalDate startLastYear = new LocalDate(vr.year-1,1,1);	
			LocalDate endLastYear = new LocalDate(vr.year-1,12,31);	
			LocalDate startYear = new LocalDate(vr.year,1,1);
			LocalDate endYear = new LocalDate(vr.year,12,31);
			DateInterval lastYearInter = new DateInterval(startLastYear, endLastYear);
			DateInterval yearInter = new DateInterval(startYear, endYear);
			DateInterval yearActualDateInter = new DateInterval(startYear, actualDate);
			
			AbsenceType ab32  = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode());
			AbsenceType ab31  = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode());
			AbsenceType ab37  = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode());
			AbsenceType ab94  = AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode());

			//Expire Last Year
			
			//ConfYear conf = ConfYear.getConfYear(year);
			Integer monthExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("month_expiry_vacation_past_year", year, person.office));
			Integer dayExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("day_expiry_vacation_past_year", year, person.office));
			LocalDate expireVacation = actualDate.withMonthOfYear(monthExpiryVacationPastYear).withDayOfMonth(dayExpiryVacationPastYear);
			
			vr.isExpireLastYear = false;
			
			if( vr.year < today.getYear() ) {		//query anni passati 
				vr.isExpireLastYear = true;
			}
			else if( vr.year == today.getYear() && actualDate.isAfter(expireVacation)) {	//query anno attuale
				vr.isExpireLastYear = true;
			}
			
			//Expire Before End Of Year / Active After Begin Of Year
			
			if(vr.activeContractInterval.getEnd().isBefore(endYear))
				vr.isExpireBeforeEndYear = true;
			if(vr.activeContractInterval.getBegin().isAfter(startYear))
				vr.isActiveAfterBeginYear = true;
			
			//(1) Calcolo ferie usate dell'anno passato ---------------------------------------------------------------------------------------------------------------------------------
			List<Absence> abs32Last = null;
			
			List<Absence> abs31Last = null;
			List<Absence> abs37Last = null;
			
			
			int vacationDaysPastYearUsedNew = 0;
			if(vr.activeContract.sourceDate!=null && vr.activeContract.sourceDate.getYear()==year)
			{
				//Popolare da source data
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + vr.activeContract.sourceVacationLastYearUsed;
				DateInterval yearInterSource = new DateInterval(vr.activeContract.sourceDate.plusDays(1), endYear);
				abs31Last = getVacationDays(yearInterSource, vr.activeContract, ab31);										
				abs37Last = getVacationDays(yearInterSource, vr.activeContract, ab37);		
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
			}
			else if(vr.activeContract.beginContract.getYear()==vr.year)
			{
				//Non esiste anno passato nel presente contratto
				vacationDaysPastYearUsedNew = 0;
				abs31Last  = new ArrayList<Absence>();
				abs37Last  = new ArrayList<Absence>();
			}
			else if(vr.year > LocalDate.now().getYear()) 
			{
				//Caso in cui voglio inserire ferie per l'anno prossimo
				VacationsRecap vrPastYear = VacationsRecap.Factory.build(vr.person,
						vr.year-1,Optional.of(vr.activeContract), endLastYear, true);
				abs31Last = getVacationDays(yearInter, vr.activeContract, ab31);						
				abs37Last = getVacationDays(yearInter, vr.activeContract, ab37);		
				vacationDaysPastYearUsedNew = vrPastYear.vacationDaysCurrentYearUsed.size() + abs31Last.size() + abs37Last.size();
			}
			else{
				//Popolare da contractYearRecap
				ContractYearRecap recapPastYear = ContractManager.getContractYearRecap(vr.activeContract, year-1);
				if(recapPastYear==null)
					throw new IllegalStateException("Mancano i riepiloghi annuali.");
				vacationDaysPastYearUsedNew = recapPastYear.vacationCurrentYearUsed;
				abs31Last = getVacationDays(yearInter, vr.activeContract, ab31);						
				abs37Last = getVacationDays(yearInter, vr.activeContract, ab37);						
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
			}
			//costruisco la lista delle ferie per stampare le date (prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
			abs32Last  = getVacationDays(lastYearInter, vr.activeContract, ab32);

//<<<<<<< HEAD
//		//Expire Last Year
//		
//		//ConfYear conf = ConfYear.getConfYear(year);
//		Integer monthExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("month_expiry_vacation_past_year", year, person.office));
//		Integer dayExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("day_expiry_vacation_past_year", year, person.office));
//		LocalDate expireVacation = actualDate.withMonthOfYear(monthExpiryVacationPastYear).withDayOfMonth(dayExpiryVacationPastYear);
//		
//		this.isExpireLastYear = false;
//		
//		if( this.year < today.getYear() ) {		//query anni passati 
//			this.isExpireLastYear = true;
//		}
//		else if( this.year == today.getYear() && actualDate.isAfter(expireVacation)) {	//query anno attuale
//			this.isExpireLastYear = true;
//		}
//		
//		//Expire Before End Of Year / Active After Begin Of Year
//		
//		if(this.activeContractInterval.getEnd().isBefore(endYear))
//			this.isExpireBeforeEndYear = true;
//		if(this.activeContractInterval.getBegin().isAfter(startYear))
//			this.isActiveAfterBeginYear = true;
//		
//		//(1) Calcolo ferie usate dell'anno passato ---------------------------------------------------------------------------------------------------------------------------------
//		List<Absence> abs32Last = null;
//		
//		List<Absence> abs31Last = null;
//		List<Absence> abs37Last = null;
//		
//		
//		int vacationDaysPastYearUsedNew = 0;
//		if(activeContract.sourceDate!=null && activeContract.sourceDate.getYear()==year)
//		{
//			//Popolare da source data
//			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + activeContract.sourceVacationLastYearUsed;
//			DateInterval yearInterSource = new DateInterval(activeContract.sourceDate.plusDays(1), endYear);
//			abs31Last = getVacationDays(yearInterSource, activeContract, ab31);										
//			abs37Last = getVacationDays(yearInterSource, activeContract, ab37);		
//			vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
//		}
//		else if(activeContract.beginContract.getYear()==this.year)
//		{
//			//Non esiste anno passato nel presente contratto
//			vacationDaysPastYearUsedNew = 0;
//			abs31Last  = new ArrayList<Absence>();
//			abs37Last  = new ArrayList<Absence>();
//		}
//		else if(this.year > LocalDate.now().getYear()) 
//		{
//			//Caso in cui voglio inserire ferie per l'anno prossimo
//			VacationsRecap vrPastYear = new VacationsRecap(this.person, this.year-1, this.activeContract, endLastYear, true);
//=======
//				abs31Last = getVacationDays(yearInter, vr.activeContract, ab31);						
//				abs37Last = getVacationDays(yearInter, vr.activeContract, ab37);		
//				vacationDaysPastYearUsedNew = vrPastYear.vacationDaysCurrentYearUsed.size() + abs31Last.size() + abs37Last.size();
//			}
//			else{
//				//Popolare da contractYearRecap
//				ContractYearRecap recapPastYear = vr.activeContract.getContractYearRecap(year-1);
//				if(recapPastYear==null)
//					throw new IllegalStateException("Mancano i riepiloghi annuali.");
//				vacationDaysPastYearUsedNew = recapPastYear.vacationCurrentYearUsed;
//				abs31Last = getVacationDays(yearInter, vr.activeContract, ab31);						
//				abs37Last = getVacationDays(yearInter, vr.activeContract, ab37);						
//				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
//			}
//			//costruisco la lista delle ferie per stampare le date (prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
//			abs32Last  = getVacationDays(lastYearInter, vr.activeContract, ab32);
//>>>>>>> branch 'refactorManager' of https://dario.tagliaferri@wiki.iit.cnr.it/redmine/e-gov-iit-cnr/epas.git

			vr.vacationDaysLastYearUsed.addAll(abs32Last);
			vr.vacationDaysLastYearUsed.addAll(abs31Last);
			vr.vacationDaysLastYearUsed.addAll(abs37Last);
			while(vr.vacationDaysLastYearUsed.size()<vacationDaysPastYearUsedNew)
			{
				Logger.debug("Inserita assenza nulla");
				Absence nullAbsence = null;
				vr.vacationDaysLastYearUsed.add(nullAbsence);
			}
			
			
			//(2) Calcolo ferie usate dell'anno corrente ---------------------------------------------------------------------------------------------------------------------------------
			List<Absence> abs32Current  = null;
			
			int vacationDaysCurrentYearUsedNew = 0;
			if(vr.activeContract.sourceDate!=null && vr.activeContract.sourceDate.getYear()==year)
			{
				vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + vr.activeContract.sourceVacationCurrentYearUsed;
				DateInterval yearInterSource = new DateInterval(vr.activeContract.sourceDate.plusDays(1), endYear);
				abs32Current = getVacationDays(yearInterSource, vr.activeContract, ab32);
				vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size();
			}
			else
			{
				abs32Current = getVacationDays(yearInter, vr.activeContract, ab32);
				vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size();
			}
			vr.vacationDaysCurrentYearUsed.addAll(abs32Current);
			while(vr.vacationDaysCurrentYearUsed.size()<vacationDaysCurrentYearUsedNew)
			{
				Logger.debug("Inserita assenza nulla");
				Absence nullAbsence = null;
				vr.vacationDaysCurrentYearUsed.add(nullAbsence);
			}
			
			//(3) Calcolo permessi usati dell'anno corrente
			List<Absence> abs94Current = null;
			int permissionCurrentYearUsedNew = 0;
			
			if(vr.activeContract.sourceDate!=null && vr.activeContract.sourceDate.getYear()==year)
			{
				permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + vr.activeContract.sourcePermissionUsed;
				DateInterval yearInterSource = new DateInterval(vr.activeContract.sourceDate.plusDays(1), endYear);
				abs94Current = getVacationDays(yearInterSource, vr.activeContract, ab94);
				permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + abs94Current.size();
			}
			else
			{
				abs94Current = getVacationDays(yearInter, vr.activeContract, ab94);
				permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + abs94Current.size();
			}
			vr.permissionUsed.addAll(abs94Current);
			while(vr.permissionUsed.size()<permissionCurrentYearUsedNew)
			{
				Logger.debug("Inserita assenza nulla");
				Absence nullAbsence = null;
				vr.permissionUsed.add(nullAbsence);
			}
			

			//(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente (sono indipendenti dal database)
			vr.vacationDaysLastYearAccrued = getVacationAccruedYear(lastYearInter, vr);
			if(endYear.isAfter(actualDate))
			{
				//se la query e' per l'anno corrente considero fino a actualDate
				vr.permissionCurrentYearAccrued = getPermissionAccruedYear( yearActualDateInter, vr.activeContract);
				vr.vacationDaysCurrentYearAccrued = getVacationAccruedYear( yearActualDateInter, vr);
			}
			else
			{
				//se la query e' per gli anni passati considero fino a endYear
				vr.permissionCurrentYearAccrued = getPermissionAccruedYear(yearInter, vr.activeContract);
				vr.vacationDaysCurrentYearAccrued = getVacationAccruedYear(yearInter, vr);
			}
			
			//(5)Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente (sono funzione di quanto calcolato precedentemente)
			//Anno passato
			if(actualDate.isBefore(expireVacation) || !considerExpireLastYear)
				vr.vacationDaysLastYearNotYetUsed = vr.vacationDaysLastYearAccrued - vr.vacationDaysLastYearUsed.size();
			else
				vr.vacationDaysLastYearNotYetUsed = 0;
			//Anno corrente
			vr.permissionCurrentYearTotal = getPermissionAccruedYear(yearInter, vr.activeContract);
			vr.vacationDaysCurrentYearTotal = getVacationAccruedYear(yearInter, vr);
			if(vr.activeContract.expireContract != null) {
				//per i detereminati considero le maturate (perchè potrebbero decidere di cambiare contratto)
				vr.vacationDaysCurrentYearNotYetUsed = vr.vacationDaysCurrentYearAccrued - vr.vacationDaysCurrentYearUsed.size();
				vr.persmissionNotYetUsed = vr.permissionCurrentYearAccrued - vr.permissionUsed.size();
			}
			else 
			{
				//per gli indeterminati le considero tutte (è più improbabile....)
				vr.vacationDaysCurrentYearNotYetUsed = vr.vacationDaysCurrentYearTotal - vr.vacationDaysCurrentYearUsed.size();
				vr.persmissionNotYetUsed = vr.permissionCurrentYearTotal - vr.permissionUsed.size();
			}
			return vr;
		}
		
		/**
		 * 
		 * @param yearInterval
		 * @param contract
		 * @return numero di permessi maturati nel periodo yearInterval associati a contract
		 */
		private static int getPermissionAccruedYear(DateInterval yearInterval, Contract contract){
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
		 * @param yearInterval
		 * @param contract
		 * @param vacationPeriodList
		 * @return il numero di giorni di ferie maturati nell'anno year 
		 * 	calcolati a partire dai piani ferie associati al contratto corrente
		 */
		private static int getVacationAccruedYear(DateInterval yearInterval, VacationsRecap vr){
			
			int vacationDays = 0;
			List<Absence> abs24Current  = null;
			List<Absence> abs24SCurrent = null;
			List<Absence> abs25Current  = null;
			AbsenceType ab24  = AbsenceTypeDao.getAbsenceTypeByCode("24");
			AbsenceType ab24S = AbsenceTypeDao.getAbsenceTypeByCode("24S"); 
			AbsenceType ab25  = AbsenceTypeDao.getAbsenceTypeByCode("25"); 
			//Calcolo l'intersezione fra l'anno e il contratto attuale
			yearInterval = DateUtility.intervalIntersection(yearInterval, 
					new DateInterval(vr.activeContract.beginContract, vr.activeContract.expireContract));
			if(yearInterval == null)
				return 0;
			
			//per ogni piano ferie conto i giorni trascorsi in yearInterval e applico la funzione di conversione
			for(VacationPeriod vp : vr.vacationPeriodList)
			{
				int days = 0;
				DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
				DateInterval intersection = DateUtility.intervalIntersection(vpInterval, yearInterval);
				if(intersection!=null)
				{
					days = DateUtility.daysInInterval(intersection);
				}
				abs24Current = getVacationDays(intersection, vr.activeContract, ab24);
				abs24SCurrent = getVacationDays(intersection, vr.activeContract, ab24S);
				abs25Current = getVacationDays(intersection, vr.activeContract, ab25);
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
		
	}
		
	
	public VacationsRecap() {
		// TODO Auto-generated constructor stub
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
		AbsenceType ab24  = AbsenceTypeDao.getAbsenceTypeByCode("24");
		AbsenceType ab24S = AbsenceTypeDao.getAbsenceTypeByCode("24S"); 
		AbsenceType ab25  = AbsenceTypeDao.getAbsenceTypeByCode("25"); 
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
				
		return VacationsRecap.Factory.build(person, year, Optional.<Contract>absent(), new LocalDate(), false).
				vacationDaysLastYearNotYetUsed;
		
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
