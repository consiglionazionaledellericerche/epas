package controllers.rendering;

import java.util.ArrayList;
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

public class VacationsRecap {
	
	//variabili necessarie al rendering
	public Person person;
	public short year;
	public Contract currentContract = null;
	public VacationPeriod vacationPeriod = null;
	
	public Integer vacationDaysLastYear = 0;
	public Integer vacationDaysCurrentYear = 0;
	public Integer permissionUsed = 0;
	
	public Integer vacationDaysLastYearNotYetUsed = 0;
	public Integer personalVacationDays = 0;
	public Integer personalPermission = 0;
	
	public Integer vacationAccruedCurrentYear = 0;
	public Integer permissionAccruedCurrentYear = 0;
	
	
	//costruttore
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
		
		//current vacation period
		this.vacationPeriod = getCurrentPersonVacationPeriod(person);
		if(vacationPeriod == null)
		{
			return;
		}
		
		
		this.vacationDaysLastYear = getVacationDaysLastYear(person, currentContract, year);
		this.vacationDaysCurrentYear = getVacationDaysCurrentYear(person, currentContract, year);
		this.permissionUsed = getPersonalPermissionUsed(person, currentContract, year);
		this.vacationDaysLastYearNotYetUsed = 
				this.vacationPeriod.vacationCode.vacationDays //TODO devo considerare quelli effettivamente maturati se il contratto è iniziato a metà anno 
				- this.vacationDaysLastYear;
	
		this.personalVacationDays = vacationPeriod.vacationCode.vacationDays;
		this.personalPermission = vacationPeriod.vacationCode.permissionDays;
		
		//TODO contattare baesso per la funzione che deve controllare il passaggio da 26+4 a 28+4 (e viceversa)
		this.vacationAccruedCurrentYear = getVacationAccruedCurrentYear(this.currentContract, this.vacationPeriod.vacationCode, this.year);
		this.permissionAccruedCurrentYear = getPermissionAccruedCurrentYear(this.year);
		
	}
	
	//metodi
	
	/**
	 * @param person
	 * @return il vacation period attuale per la persona,
	 * 		 	null in caso di vacation period inesistente o scaduto
	 */
	public VacationPeriod getCurrentPersonVacationPeriod(Person person)
	{
		//vacation period piu' recente per la persona
		VacationPeriod period = VacationPeriod.find(  "SELECT vp "
													+ "FROM VacationPeriod vp "
													+ "WHERE vp.person = ? "
													+ "ORDER BY vp.beginFrom DESC",
													person).first();
		
		//se il piano ferie non esiste 
		if(period==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}
		
		//se il piano ferie è scaduto
		LocalDate now = new LocalDate();
		if(period.endTo != null && period.endTo.isBefore(now))
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è scaduto");
			return null;
		}
		return period;
	}
	
	
	/**
	 * 
	 * @param person
	 * @param currentContract
	 * @param year
	 * @return il numero di giorni di ferie presi l'anno precedente. 
	 * Il numero di giorni di ferie corrisponde a tutte quelle giornate di assenza registrate sul database 
	 * (1) nell'anno precedente con codice 32 ("ferie anno corrente")
	 * (2)nell'anno corrente con codice 31 ("ferie anno precedente") - 
	 * imputabili al contratto corrente 
	 */
	public int getVacationDaysLastYear(Person person, Contract currentContract, int year){
		int vacationDaysLastYear = 0;
		
		//calcolo inizio e fine da considerare per le ferie 32
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		if(currentContract.beginContract.isAfter(endLastYear))
		{
			//il contratto e' iniziato nell'anno corrente
			Logger.debug("VacationDaysLastYear calcolo 32: il contratto è iniziato nell'anno corrente");
			return 0;
		}
		
		//ferie 32
		if(currentContract.beginContract.isAfter(beginLastYear))
		{
			Logger.debug("VacationDaysLastYear calcolo 32: il contratto è iniziato nell'anno passato");
			beginLastYear=currentContract.beginContract;
		}

		List<Absence> absences = Absence.find(
				"SELECT ab "
				+ "FROM Absence ab "
				+ "WHERE ab.personDay.person = ? AND ( ab.personDay.date between ? AND ? ) AND ab.absenceType.code = ?",
				person, beginLastYear, endLastYear, "32").fetch();
		
		vacationDaysLastYear = absences.size();	
		
		//ferie 31
		LocalDate beginYear = new LocalDate(year, 1,1);
		LocalDate now = new LocalDate().minusYears(new LocalDate().getYear()-year);
		if(currentContract.beginContract.isAfter(now))
		{
			Logger.debug("VacationDaysLastYear calcolo 31: il contratto non e' ancora iniziato");				
			return 0;
		}
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginYear, now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("31")){
				vacationDaysLastYear++;	//TODO migliorare l'efficienza e gestire absences.size > 1
			}
		}

		return vacationDaysLastYear;
	}

	/**
	 * 
	 * @param person
	 * @param currentContract
	 * @param year
	 * @return il numero di giorni di ferie per l'anno corrente. 
	 * Il numero di giorni di ferie corrisponde a tutte quelle giornate di
	 * assenza registrate sul database col codice 32 ovvero "ferie anno corrente" imputabili al contratto attuale
	 */
	public int getVacationDaysCurrentYear(Person person, Contract currentContract, int year){
		
		int vacationDaysCurrentYear = 0;
		LocalDate beginYear = new LocalDate(year, 1,1);
		LocalDate now = new LocalDate();
		if(currentContract.beginContract.isAfter(now))
		{
			Logger.debug("VacationDaysCurrentYear: il contratto non e' ancora iniziato");				
			return 0;
		}
		if(currentContract.beginContract.isAfter(beginYear))
		{
			Logger.debug("VacationDaysCurrentYear: il contratto è iniziato nell'anno corrente ");
			beginYear= currentContract.beginContract;
		}
	
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?",
				person, beginYear, now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("32"))
				vacationDaysCurrentYear ++;	//TODO migliorare l'efficienza e gestire absences.size > 1
		}

		return vacationDaysCurrentYear;
	}
	
	/**
	 * 
	 * @param person
	 * @param currentContract
	 * @param year
	 * @return i giorni di permesso che la persona ha utilizzato nell'anno imputabili al contratto attuale
	 */
	public int getPersonalPermissionUsed(Person person, Contract currentContract, int year){
		Logger.debug("Inizio il calcolo dei permessi utilizzati");
		int permissionDays = 0;
		LocalDate beginYear = new LocalDate(year, 1,1);
		LocalDate now = new LocalDate();
		if(currentContract.beginContract.isAfter(now))
		{
			Logger.debug("PersonalPermissionUsed: il contratto non e' ancora iniziato");				
			return 0;
		}
		if(currentContract.beginContract.isAfter(beginYear))
		{
			Logger.debug("PersonalPermissionUsed: il contratto è iniziato nell'anno corrente ");
			beginYear = currentContract.beginContract;
		}
		
		
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginYear, now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("94"))
				permissionDays++; 	//TODO migliorare l'efficienza e gestire absences.size > 1
		}
	
		Logger.debug("Fine del calcolo dei permessi utilizzati che sono %s", permissionDays);
		return permissionDays;
	}
	
	/**
	 * 
	 * @param currentContract
	 * @param vacationCode
	 * @param year
	 * @return il numero di giorni di ferie maturati nell'anno corrente year calcolati a partire da un codice di piano ferie ed un contratto
	 */
	public int getVacationAccruedCurrentYear(Contract currentContract, VacationCode vacationCode, int year){
		int days = 0;
		int vacationDays = 0;
		LocalDate now = new LocalDate().withYear(year);
	
		//la persona è in istituto da più di 3 anni
		if(vacationCode.description.equals("28+4"))
		{
			days = now.getDayOfYear();
			vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysMoreThreeYears(days);
			Logger.debug("I giorni di ferie maturati per %s %s nell'anno %d sono %d", person.name, person.surname, year, vacationDays);
			return vacationDays;
		}
		
		//la persona è in istituto da meno di 3 anni
		LocalDate beginContract = currentContract.beginContract;
		if(vacationCode.description.equals("26+4"))
		{
			//giorni trascorsi dall'inizio del contratto
			if(now.getYear() == beginContract.getYear())
			{	
				days = now.getDayOfYear() - beginContract.getDayOfYear();	//TODO potrebbe essere necessario un +1
			}
			else
			{
				days = now.getDayOfYear();
			}
			vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);
			Logger.debug("I giorni di ferie maturati per %s %s nell'anno %d sono %d", person.name, person.surname, year, vacationDays);
			return vacationDays;
		}
		return 0;
	}
	
	/**
	 * 
	 * @param currentContract
	 * @param vacationCode
	 * @param year
	 * @return il numero di giorni di ferie maturati nell'anno passato calcolati a partire da un codice di piano ferie ed un contratto
	 */
	public int getVacationAccruedLastYear(Contract currentContract, VacationCode vacationCode, int year){
		int days = 0;
		int vacationDays = 0;
		LocalDate now = new LocalDate().withYear(year);
	
		//la persona è in istituto da più di 3 anni
		if(vacationCode.description.equals("28+4"))
		{
			Logger.debug("I giorni di ferie maturati per %s %s nell'anno %d sono %d", person.name, person.surname, year-1, vacationDays);
			return vacationCode.vacationDays;
		}
		
		//la persona è in istituto da meno di 3 anni
		LocalDate beginContract = currentContract.beginContract;
		if(vacationCode.description.equals("26+4"))
		{
			//giorni trascorsi dall'inizio del contratto
			if(now.getYear() == beginContract.getYear())
			{	
				days = now.getDayOfYear() - beginContract.getDayOfYear();	//TODO potrebbe essere necessario un +1
			}
			else
			{
				days = now.getDayOfYear();
			}
			vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);
			Logger.debug("I giorni di ferie maturati per %s %s nell'anno %d sono %d", person.name, person.surname, year, vacationDays);
			return vacationDays;
		}
		return 0;
	}
	
	/**
	 * 
	 * @param year
	 * @return il numero di giorni di permesso legge maturati nell'anno corrente
	 */
	public int getPermissionAccruedCurrentYear(int year){
		int days = 0;
		int permissionDays = 0;
		LocalDate now = new LocalDate().withYear(year);
		days = now.getDayOfYear();
		permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}
	

}
