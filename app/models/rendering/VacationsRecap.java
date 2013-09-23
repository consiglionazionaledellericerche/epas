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
 * Contiene inoltre due metodi statici di utilità 
 * 1) getContractVacationPeriods(Contract) per estrarre dal contratto i vacation period associati
 * 2) getCurrentVacationPeriod(Contract) per estrarre dal contratto il vacation period attuale
 */
public class VacationsRecap {
	
	public Person person;
	public short year;
	public Contract currentContract = null;
	public List<VacationPeriod>vacationPeriodList = null;	//lista vacation period del current contract
	
	public List<Absence> vacationDaysLastYearUsed = new ArrayList<Absence>();
	public Integer vacationDaysLastYearAccrued = 0;
	public Integer vacationDaysLastYearNotYetUsed = 0;
	
	public List<Absence> vacationDaysCurrentYearUsed = new ArrayList<Absence>();
	public Integer vacationDaysCurrentYearAccrued = 0;
	
	public Integer permissionUsed = 0;
		
	public Integer vacationDaysCurrentYearTotal = 0;
	public Integer permissionCurrentYearTotal = 0;
	
	
	public Integer permissionCurrentYearAccrued = 0;
	
	
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
		this.vacationPeriodList = getContractVacationPeriods(this.currentContract);
		if(vacationPeriodList == null)
		{
			return;
		}
		
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);
		LocalDate today = new LocalDate();
		
		//***************************************************************
		//*** calcolo ferie e permessi utilizzati per year e lastYear ***
		//***************************************************************
		
		AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab31 = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab94 = AbsenceType.getAbsenceTypeByCode("94");

		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startLastYear, endLastYear), currentContract, ab32));
		this.vacationDaysLastYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), currentContract, ab31));
										 
		this.vacationDaysCurrentYearUsed.addAll(getVacationDays(new DateInterval(startYear, endYear), currentContract, ab32));
		
		this.permissionUsed = getVacationDays(new DateInterval(startYear, endYear), currentContract, ab94).size();
		
		//**************************************************************
		//*** calcolo giorni e permessi maturali per year e lastYear ***
		//**************************************************************
		
		this.vacationDaysLastYearAccrued = getVacationAccruedYear(new DateInterval(startLastYear, endLastYear), this.currentContract, this.vacationPeriodList);
		this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued - this.vacationDaysLastYearUsed.size();	//TODO considerare quando scadono
		
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
		
		
		this.permissionCurrentYearTotal = getPermissionAccruedYear(new DateInterval(startYear, endYear), this.currentContract);
		this.vacationDaysCurrentYearTotal = getVacationAccruedYear(new DateInterval(startYear, endYear), this.currentContract, this.vacationPeriodList);		//a cristian da 27 perchè è passato da 26 a 28 durante l'anno

	}
	
	/**
	 * @param person
	 * @return i vacation period associati al contratto, ordinati in ordine crescente per data inizio
	 * 		 	null in caso di vacation period inesistente
	 */
	public static List<VacationPeriod> getContractVacationPeriods(Contract currentContract)
	{
		//vacation period piu' recente per la persona
		List<VacationPeriod> vpList = VacationPeriod.find(  "SELECT vp "
													+ "FROM VacationPeriod vp "
													+ "WHERE vp.contract = ? "
													+ "ORDER BY vp.beginFrom",
													currentContract).fetch();
		
		//se il piano ferie associato al contratto non esiste 
		if(vpList==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}
		
		
		return vpList;
	}
	

	/**
	 * TODO questo metodo andrebbe messo nella classe VacationPeriod o Contract
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public static VacationPeriod getCurrentVacationPeriod(Contract contract)
	{
		List<VacationPeriod> vpList = getContractVacationPeriods(contract);
		for(VacationPeriod vp : vpList)
		{
			
			LocalDate now = new LocalDate();
			
			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
		}
		return null;
	}
	
	
	/**
	 * 
	 * @param beginYear
	 * @param endYear
	 * @param contract
	 * @param vacationPeriodList
	 * @return il numero di giorni di ferie maturati nell'anno year 
	 * 	calcolati a partire dai piani ferie associati al contratto corrente
	 */
	public int getVacationAccruedYear(DateInterval yearInterval, Contract contract, List<VacationPeriod>vacationPeriodList){
		
		int vacationDays = 0;

		//data iniziale da consideare //TODO usare intersection
		if(yearInterval.getBegin().isBefore(contract.beginContract))		
		{
			yearInterval = new DateInterval(contract.beginContract, yearInterval.getEnd());
		}
		
		//per ogni piano ferie conto i giorni trascorsi in year e applico la funzione
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
	 * @param start
	 * @param end
	 * @param contract
	 * @return
	 */
	public int getPermissionAccruedYear(DateInterval yearInterval, Contract contract){
		int days = 0;
		int permissionDays = 0;
	
		//data iniziale da consideare //TODO usare intersection con progressione contratti
		if(yearInterval.getBegin().isBefore(contract.beginContract))
		{
			
			if(contract.beginContract.isAfter(yearInterval.getEnd()))
			{
				return 0;
			}
			//TODO importante. Il begin contract che devo considerare e' l'inizio che lavora al cnr?????
			yearInterval = new DateInterval(contract.beginContract, yearInterval.getEnd());
		}

		days = yearInterval.getEnd().getDayOfYear() - yearInterval.getBegin().getDayOfYear();
		permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}
	
	
	/**
	 * 
	 * @param inter
	 * @param contract
	 * @param ab
	 * @return a lista di assenze effettuate dal titolare del contratto del tipo ab nell'intervallo temporale inter
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
	

	

}
