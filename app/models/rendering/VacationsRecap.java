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
import models.Configuration;
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
	public short year;
	public Contract currentContract = null;
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

	
	
	public VacationsRecap(Person person, short year)
	{
		this.person = person;
		this.year = year;
		
		//current contract
		this.currentContract = person.getCurrentContract();
		if(currentContract == null)
		{
			return;
		}
		
		//vacation periods list
		this.vacationPeriodList = this.currentContract.getContractVacationPeriods();
		if(vacationPeriodList == null)
		{
			return;
		}
		
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);
		LocalDate today = new LocalDate();
		Configuration config = Configuration.getCurrentConfiguration();
		LocalDate expireVacation = today.withMonthOfYear(config.monthExpiryVacationPastYear).withDayOfMonth(config.dayExpiryVacationPastYear);
		
		//***************************************************************
		//*** calcolo ferie e permessi utilizzati per year e lastYear ***
		//***************************************************************
		
		AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab31 = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab37 = AbsenceType.getAbsenceTypeByCode("37");
		AbsenceType ab94 = AbsenceType.getAbsenceTypeByCode("94");

		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startLastYear, endLastYear), currentContract, ab32));
		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), currentContract, ab31));
		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), currentContract, ab37));
		
		this.vacationDaysCurrentYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), currentContract, ab32));
		
		this.permissionUsed = getVacationDays(new DateInterval(startYear, endYear), currentContract, ab94).size();
		
		
		//***************************************************************
		//*** calcolo ferie e permessi maturati per year e lastyear	  ***
		//***************************************************************
		
		this.vacationDaysLastYearAccrued = getVacationAccruedYear(new DateInterval(startLastYear, endLastYear), this.currentContract, this.vacationPeriodList);
		if(endYear.isAfter(today))
		{
			//se la query e' per l'anno corrente considero fino a today
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(new DateInterval(startYear, today), this.currentContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(new DateInterval(startYear, today), this.currentContract, this.vacationPeriodList);
		}
		else
		{
			//se la query e' per gli anni passati considero fino a endYear
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(new DateInterval(startYear, endYear), this.currentContract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(new DateInterval(startYear, endYear), this.currentContract, this.vacationPeriodList);
			
		}
		
		//******************************************************************************************************
		//*** calcolo ferie e permessi non ancora utilizzati  per year e last year 							 ***
		//******************************************************************************************************
		this.permissionCurrentYearTotal = getPermissionAccruedYear(new DateInterval(startYear, endYear), this.currentContract);
		this.vacationDaysCurrentYearTotal = getVacationAccruedYear(new DateInterval(startYear, endYear), this.currentContract, this.vacationPeriodList);		//a cristian da 27 perchè è passato da 26 a 28 durante l'anno
		
		
		if(today.isBefore(expireVacation))
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
	 * metodo che ritorna il numero di giorni di ferie dell'anno scorso ancora disponibili dopo il 31/8
	 * controlla anche se dei "37" sono stati utilizzati fino alla data in cui si chiama il metodo di inserimento.
	 */
	public static int remainingPastVacations(int year, Person person, AbsenceType abt){
		int days = 0;
		Configuration config = Configuration.getCurrentConfiguration();
		Contract contractLastYear = person.getContract(new LocalDate(year-1,12,31));
		if(contractLastYear == null)
			return 0;
		
		VacationCode code = VacationCode.find("Select code from VacationCode code, VacationPeriod period where period.vacationCode = code" +
				" and period.contract = ? ", contractLastYear).first();
		if(code == null)
			return 0;
		DateInterval inter = new DateInterval(new LocalDate(year,1,1), new LocalDate(year, config.monthExpiryVacationPastYear, config.dayExpiryVacationPastYear));
		DateInterval pastInter = new DateInterval(new LocalDate(year-1,1,1), new LocalDate(year-1,12,31));
		DateInterval inter37 = new DateInterval(
				new LocalDate(year, config.monthExpiryVacationPastYear,config.dayExpiryVacationPastYear).plusDays(1), new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue());
		Contract contract = person.getCurrentContract();
		AbsenceType ab32 = AbsenceType.find("byCode", "32").first();
		AbsenceType ab31 = AbsenceType.find("byCode", "31").first();
		AbsenceType ab37 = AbsenceType.find("byCode", "37").first();
		Logger.debug("Il tipo di assenza è: %s", ab37.description);
		List<Absence> absencePastYearThisYear = getVacationDays(inter, contract, ab31);
		List<Absence> absencePastYearLastYear = getVacationDays(pastInter, contractLastYear, ab32);
		List<Absence> absence37 = getVacationDays(inter37, contract, ab37);
		
		Logger.debug("La somma dei giorni di ferie tra anno passato e anno attuale è: %d", absencePastYearThisYear.size()+absencePastYearLastYear.size());
		Logger.debug("I codici %s già usati quest'anno sono: %d", ab37.code,  absence37.size());
		Logger.debug("I giorni da usare di ferie per un anno sono: %d", code.vacationDays);
		if(absencePastYearThisYear.size()+absencePastYearLastYear.size()+ absence37.size() < code.vacationDays)
			return code.vacationDays - absencePastYearThisYear.size()+absencePastYearLastYear.size()+ absence37.size();
		else
			
		return days;
	}

}
