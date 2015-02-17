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
	public Contract contract = null;
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
		 * @param contract il contratto di cui si vuole calcolare il riepilogo.
		 * @param actualDate la data specifica dell'anno attuale in cui si desidera fotografare la situazione in termini di ferie e permessi maturati (tipicamente oggi)
		 * @param considerExpireLastYear impostare false se non si vuole considerare il limite di scadenza per l'utilizzo
		 * @return
		 */
		public static VacationsRecap build(int year, Contract contract,
				LocalDate actualDate, boolean considerExpireLastYear) {
			
			Preconditions.checkNotNull(year);
			Preconditions.checkNotNull(contract);
			Preconditions.checkNotNull(actualDate);
			
			VacationsRecap vr = new VacationsRecap();
			vr.person = contract.person;
			vr.year = year;
			vr.contract = contract;
			
			List<VacationPeriod> vacationPeriodList = ContractManager.getContractVacationPeriods(vr.contract);
			
			//FIXME Se non ho i piani ferie li costruisco. Decidere dove metterli (bootstrap??)
			//il riepilogo non dovrebbe modificare il db
			if(vacationPeriodList==null || vacationPeriodList.isEmpty()) {
				ContractManager.properContractUpdate(vr.contract);
				vacationPeriodList = ContractManager.getContractVacationPeriods(vr.contract);
			}
			Verify.verify(vacationPeriodList!=null && !vacationPeriodList.isEmpty(), "Nessun piano ferie presente per il contratto!");
			
			vr.activeContractInterval = vr.contract.getContractDateInterval();
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
			Integer monthExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("month_expiry_vacation_past_year", year, vr.person.office));
			Integer dayExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("day_expiry_vacation_past_year", year, vr.person.office));
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
			if(vr.contract.sourceDate!=null && vr.contract.sourceDate.getYear()==year)
			{
				//Popolare da source data
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + vr.contract.sourceVacationLastYearUsed;
				DateInterval yearInterSource = new DateInterval(vr.contract.sourceDate.plusDays(1), endYear);
				abs31Last = getVacationDays(yearInterSource, vr.contract, ab31);										
				abs37Last = getVacationDays(yearInterSource, vr.contract, ab37);		
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
			}
			else if(vr.contract.beginContract.getYear()==vr.year)
			{
				//Non esiste anno passato nel presente contratto
				vacationDaysPastYearUsedNew = 0;
				abs31Last  = new ArrayList<Absence>();
				abs37Last  = new ArrayList<Absence>();
			}
			else if(vr.year > LocalDate.now().getYear()) 
			{
				//Caso in cui voglio inserire ferie per l'anno prossimo
				VacationsRecap vrPastYear = VacationsRecap.Factory.build(vr.year-1,vr.contract, endLastYear, true);
				abs31Last = getVacationDays(yearInter, vr.contract, ab31);						
				abs37Last = getVacationDays(yearInter, vr.contract, ab37);		
				vacationDaysPastYearUsedNew = vrPastYear.vacationDaysCurrentYearUsed.size() + abs31Last.size() + abs37Last.size();
			}
			else{
				//Popolare da contractYearRecap
				ContractYearRecap recapPastYear = ContractManager.getContractYearRecap(vr.contract, year-1);
				if(recapPastYear==null)
					throw new IllegalStateException("Mancano i riepiloghi annuali.");
				vacationDaysPastYearUsedNew = recapPastYear.vacationCurrentYearUsed;
				abs31Last = getVacationDays(yearInter, vr.contract, ab31);						
				abs37Last = getVacationDays(yearInter, vr.contract, ab37);						
				vacationDaysPastYearUsedNew = vacationDaysPastYearUsedNew + abs31Last.size() + abs37Last.size();
			}
			//costruisco la lista delle ferie per stampare le date (prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
			abs32Last  = getVacationDays(lastYearInter, vr.contract, ab32);

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
			if(vr.contract.sourceDate!=null && vr.contract.sourceDate.getYear()==year)
			{
				vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + vr.contract.sourceVacationCurrentYearUsed;
				DateInterval yearInterSource = new DateInterval(vr.contract.sourceDate.plusDays(1), endYear);
				abs32Current = getVacationDays(yearInterSource, vr.contract, ab32);
				vacationDaysCurrentYearUsedNew = vacationDaysCurrentYearUsedNew + abs32Current.size();
			}
			else
			{
				abs32Current = getVacationDays(yearInter, vr.contract, ab32);
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
			
			if(vr.contract.sourceDate!=null && vr.contract.sourceDate.getYear()==year)
			{
				permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + vr.contract.sourcePermissionUsed;
				DateInterval yearInterSource = new DateInterval(vr.contract.sourceDate.plusDays(1), endYear);
				abs94Current = getVacationDays(yearInterSource, vr.contract, ab94);
				permissionCurrentYearUsedNew = permissionCurrentYearUsedNew + abs94Current.size();
			}
			else
			{
				abs94Current = getVacationDays(yearInter, vr.contract, ab94);
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
				vr.permissionCurrentYearAccrued = getPermissionAccruedYear( yearActualDateInter, vr);
				vr.vacationDaysCurrentYearAccrued = getVacationAccruedYear( yearActualDateInter, vr);
			}
			else
			{
				//se la query e' per gli anni passati considero fino a endYear
				vr.permissionCurrentYearAccrued = getPermissionAccruedYear(yearInter, vr);
				vr.vacationDaysCurrentYearAccrued = getVacationAccruedYear(yearInter, vr);
			}
			
			//(5)Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente (sono funzione di quanto calcolato precedentemente)
			//Anno passato
			if(actualDate.isBefore(expireVacation) || !considerExpireLastYear)
				vr.vacationDaysLastYearNotYetUsed = vr.vacationDaysLastYearAccrued - vr.vacationDaysLastYearUsed.size();
			else
				vr.vacationDaysLastYearNotYetUsed = 0;
			//Anno corrente
			vr.permissionCurrentYearTotal = getPermissionAccruedYear(yearInter, vr);
			vr.vacationDaysCurrentYearTotal = getVacationAccruedYear(yearInter, vr);
			if(vr.contract.expireContract != null) {
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
		private static int getPermissionAccruedYear(DateInterval yearInterval, /*Contract contract*/ VacationsRecap vr){
			//int days = 0;
			int permissionDays = 0;
		
			//Calcolo l'intersezione fra l'anno e il contratto attuale
			yearInterval = DateUtility.intervalIntersection(yearInterval, new DateInterval(vr.contract.beginContract, vr.contract.expireContract));
			if(yearInterval == null)
				return 0;
			
			
			for(VacationPeriod vp : vr.vacationPeriodList){
				int days = 0;
				DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
				DateInterval intersection = DateUtility.intervalIntersection(vpInterval, yearInterval);
				if(intersection!=null)
				{
					days = yearInterval.getEnd().getDayOfYear() - yearInterval.getBegin().getDayOfYear();
					//days = DateUtility.daysInInterval(intersection);
				}
				if(vp.vacationCode.equals("21+3")){
					permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDaysPartTime(days);
				}
				else{
					permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDays(days);
				}
			}
			
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

			//Calcolo l'intersezione fra l'anno e il contratto attuale
			yearInterval = DateUtility.intervalIntersection(yearInterval, 
					new DateInterval(vr.contract.beginContract, vr.contract.expireContract));
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
				
				//calcolo i giorni maturati col metodo di conversione
				List<Absence> absences = accruedVacationDays(intersection, vr.contract);
				if(vp.vacationCode.description.equals("26+4"))
				{
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(
							days-absences.size());
				}
				if(vp.vacationCode.description.equals("28+4"))
				{
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysMoreThreeYears(
							days-absences.size());
				}
				if(vp.vacationCode.description.equals("21+3")){
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued.converWorkDaysToVacationDaysPartTime(
							days-absences.size());
				}
				
			}
			
			//FIXME decidere se deve essere un parametro di configurazione
			if(vacationDays>28)
				vacationDays = 28;
			
			return vacationDays;

		}
		
		/**
		 * 
		 * @param intersection
		 * @param contract
		 * @return la lista dei giorni di assenza in cui si è usato un codice di assenza per assistenza post partum
		 */
		private static List<Absence> accruedVacationDays(DateInterval intersection, Contract contract){
			List<AbsenceType> postPartumCodeList = AbsenceTypeDao.getPostPartumAbsenceTypeList();
			DateInterval contractInterInterval = DateUtility.intervalIntersection(intersection, contract.getContractDateInterval());
			if(contractInterInterval==null)
				return new ArrayList<Absence>();
			List<Absence> absences = AbsenceDao.getAbsenceWithPostPartumCode(contract.person, contractInterInterval.getBegin(), contractInterInterval.getEnd(), postPartumCodeList, true);
			return absences;
		}
		
	}
		
	
	public VacationsRecap() {
		// TODO Auto-generated constructor stub
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
				
		List<Absence> absences = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(contract.person), Optional.fromNullable(ab.code), 
				contractInterInterval.getBegin(), contractInterInterval.getEnd(), Optional.<JustifiedTimeAtWork>absent(), false, true);
		
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

		return VacationsRecap.Factory.build(year, person.getCurrentContract(), new LocalDate(), false).
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
				return 0;
			if(days >= 16 && days <= 45)
				return 2;
			if(days >= 46 && days <= 75)
				return 4;
			if(days >= 76 && days <= 106)
				return 6;
			if(days >= 107 && days <= 136)
				return 8;
			if(days >= 137 && days <= 167)
				return 10;
			if(days >= 168 && days <= 197)
				return 13;
			if(days >= 198 && days <= 227)
				return 15;
			if(days >= 228 && days <= 258)
				return 17;
			if(days >= 259 && days <= 288)
				return 19;
			if(days >= 289 && days <= 319)
				return 21;
			if(days >= 320 && days <= 349)
				return 23;
			
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
				return 0;
			if(days >= 16 && days <= 45)
				return 2;
			if(days >= 46 && days <= 75)
				return 4;
			if(days >= 76 && days <= 106)
				return 7;
			if(days >= 107 && days <= 136)
				return 9;
			if(days >= 137 && days <= 167)
				return 11;
			if(days >= 168 && days <= 197)
				return 14;
			if(days >= 198 && days <= 227)
				return 16;
			if(days >= 228 && days <= 258)
				return 18;
			if(days >= 259 && days <= 288)
				return 21;
			if(days >= 289 && days <= 319)
				return 23;
			if(days >= 320 && days <= 349)
				return 25;			
			else
				return 28;
			
		}
		
		/**
		 * 
		 * @param days
		 * @return il numero di giorni di ferie maturati secondo il piano di accumulo
		 * previsto per il part time verticale
		 */
		public static int converWorkDaysToVacationDaysPartTime(int days){
			if(days<=0)
				return 0;
			
			if(days >= 1 && days <= 15)
				return 0;
			if(days >= 16 && days <= 45)
				return 2;
			if(days >= 46 && days <= 75)
				return 3;
			if(days >= 76 && days <= 106)
				return 5;
			if(days >= 107 && days <= 136)
				return 6;
			if(days >= 137 && days <= 167)
				return 8;
			if(days >= 168 && days <= 197)
				return 10;
			if(days >= 198 && days <= 227)
				return 12;
			if(days >= 228 && days <= 258)
				return 14;
			if(days >= 259 && days <= 288)
				return 15;
			if(days >= 289 && days <= 319)
				return 17;
			if(days >= 320 && days <= 349)
				return 18;			
			else
				return 21;
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
		
		
		/**
		 * 
		 * @param days
		 * @return il numero di giorni di permesso maturati con il piano ferie
		 * relativo al part time
		 */
		public static int convertWorkDaysToPermissionDaysPartTime(int days){
			int permissionDays = 0;
			if(days >= 45 && days <= 135)
				permissionDays = 1;
			if(days >= 136 && days <= 315)
				permissionDays = 2;			
			if(days >= 316 && days <= 365)
				permissionDays = 3;
			return permissionDays;
		}
		
	}


}
