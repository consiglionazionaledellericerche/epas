package dto;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import manager.recaps.vacation.VacationsRecap;
import models.Absence;
import models.Contract;

/**
 * 
 * Dto per il template Vacations.show che renderizza la situazione
 * ferie e permessi di una persona in un anno.
 * 
 * @author alessandro
 *
 */
public class VacationsShowDto {
	
	public int year;
	
	public int queryYearVacationTotal;
	public int queryYearVacationAccrued;
	public List<Absence> queryYearVacationUsed = Lists.newArrayList();
	
	public int previousYearVacationTotal;
	public int previousYearVacationAccrued;
	public List<Absence> previousYearVacationUsed = Lists.newArrayList();
	
	public int queryYearPermissionTotal;
	public int queryYearPermissionAccrued;
	public List<Absence> queryYearPermissionUsed = Lists.newArrayList();
	
	public Contract contract;

	private VacationsShowDto(){}
	
	/**
	 * Construisce il dto per l'anno year del contratto appartenente ai recap.
	 * Nel caso di riepilogo per l'anno attuale o precedenti viene utilizzato first.
	 * Nel caso di riepilogo per l'anno successivo a quello attuale viene utilizzato
	 * first per l'anno della query e previous per l'anno attuale.
	 *
	 * @param year
	 * @param first
	 * @param previous
	 * @return
	 */
	public static VacationsShowDto build(int year, VacationsRecap first, VacationsRecap previous) {
		
		if(first == null || previous == null)
			return null;
		
		VacationsShowDto vacationsShow = new VacationsShowDto();
		
		vacationsShow.year = year;
		
		vacationsShow.contract = first.contract;
		
		//Query anno corrente oppure anni passati
		if(year <= LocalDate.now().getYear()) {
			
			vacationsShow.queryYearVacationTotal = first.vacationDaysCurrentYearTotal;
			vacationsShow.queryYearVacationAccrued = first.vacationDaysCurrentYearAccrued;
			vacationsShow.queryYearVacationUsed = first.vacationDaysCurrentYearUsed;
			
			vacationsShow.previousYearVacationTotal = first.vacationDaysLastYearAccrued;
			vacationsShow.previousYearVacationAccrued = first.vacationDaysLastYearAccrued;
			vacationsShow.previousYearVacationUsed = first.vacationDaysLastYearUsed;
			
			vacationsShow.queryYearPermissionTotal = first.permissionCurrentYearTotal;
			vacationsShow.queryYearPermissionAccrued = first.permissionCurrentYearAccrued;
			vacationsShow.queryYearPermissionUsed = first.permissionUsed;
			
			return vacationsShow;
		}
		
		//Query prossimo anno
		if(year == LocalDate.now().getYear() + 1) {
			
			vacationsShow.queryYearVacationTotal = first.vacationDaysCurrentYearTotal;
			vacationsShow.queryYearVacationAccrued = first.vacationDaysCurrentYearAccrued;
			vacationsShow.queryYearVacationUsed = first.vacationDaysCurrentYearUsed;
			
			vacationsShow.previousYearVacationTotal = previous.vacationDaysCurrentYearTotal;
			vacationsShow.previousYearVacationAccrued = previous.vacationDaysCurrentYearAccrued;
			vacationsShow.previousYearVacationUsed = previous.vacationDaysCurrentYearUsed;
			
			vacationsShow.queryYearPermissionTotal = first.permissionCurrentYearTotal;
			vacationsShow.queryYearPermissionAccrued = first.permissionCurrentYearAccrued;
			vacationsShow.queryYearPermissionUsed = first.permissionUsed;
			
		}
		
		return vacationsShow;
	}

}
