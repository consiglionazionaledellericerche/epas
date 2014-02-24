package models.rendering;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import play.Logger;
import models.Absence;
import models.AbsenceType;
import models.ConfYear;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.VacationCode;
import models.VacationPeriod;
import models.VacationsPermissionsDaysAccrued;

import com.google.common.collect.Table;

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
	public Integer permissionUsed = 0;
	
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
		{
			return;
		}
		
		//vacation periods list
		this.vacationPeriodList = this.activeContract.getContractVacationPeriods();
		if(vacationPeriodList == null)
		{
			return;
		}
		
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);

		ConfYear conf = ConfYear.getConfYear((int)year);
		LocalDate expireVacation = actualDate.withMonthOfYear(conf.monthExpiryVacationPastYear).withDayOfMonth(conf.dayExpiryVacationPastYear);
		
		//***************************************************************
		//*** calcolo ferie e permessi utilizzati per year e lastYear ***
		//***************************************************************
		
		AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab31 = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab37 = AbsenceType.getAbsenceTypeByCode("37");
		AbsenceType ab94 = AbsenceType.getAbsenceTypeByCode("94");

		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startLastYear, endLastYear), activeContract, ab32));
		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), activeContract, ab31));
		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), activeContract, ab37));
		
		this.vacationDaysCurrentYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), activeContract, ab32));
		
		this.permissionUsed = getVacationDays(new DateInterval(startYear, endYear), activeContract, ab94).size();
		
		
		//***************************************************************
		//*** calcolo ferie e permessi maturati per year e lastyear	  ***
		//***************************************************************
		
		this.vacationDaysLastYearAccrued = getVacationAccruedYear(new DateInterval(startLastYear, endLastYear), this.activeContract, this.vacationPeriodList);
		if(endYear.isAfter(actualDate))
		{
			//se la query e' per l'anno corrente considero fino a actualDate
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(new DateInterval(startYear, actualDate), this.activeContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(new DateInterval(startYear, actualDate), this.activeContract, this.vacationPeriodList);
		}
		else
		{
			//se la query e' per gli anni passati considero fino a endYear
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(new DateInterval(startYear, endYear), this.activeContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(new DateInterval(startYear, endYear), this.activeContract, this.vacationPeriodList);
			
		}
		
		//******************************************************************************************************
		//*** calcolo ferie e permessi non ancora utilizzati  per year e last year 							 ***
		//******************************************************************************************************
		this.permissionCurrentYearTotal = getPermissionAccruedYear(new DateInterval(startYear, endYear), this.activeContract);
		this.vacationDaysCurrentYearTotal = getVacationAccruedYear(new DateInterval(startYear, endYear), this.activeContract, this.vacationPeriodList);		//a cristian da 27 perchè è passato da 26 a 28 durante l'anno
		
		
		if(actualDate.isBefore(expireVacation) || !considerExpireLastYear)
		{
			this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued - this.vacationDaysLastYearUsed.size();
		}
		else
		{
			this.vacationDaysLastYearNotYetUsed = 0;
		}
		this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal - this.vacationDaysCurrentYearUsed.size();									//per adesso quelli non utilizzati li considero tutti
		this.persmissionNotYetUsed = this.permissionCurrentYearTotal - this.permissionUsed;
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

}
