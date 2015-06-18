package dto;

import manager.recaps.vacation.VacationsRecap;
import models.Contract;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

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
	
	public int queryYearVacationTotal = 0;
	public int queryYearVacationAccrued = 0;
	public int queryYearVacationUsed = 0;
	
	public int previousYearVacationTotal = 0;
	public int previousYearVacationAccrued = 0;
	public int previousYearVacationUsed = 0;
	
	public int queryYearPermissionTotal = 0;
	public int queryYearPermissionAccrued = 0;
	public int queryYearPermissionUsed = 0;
	
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
	public static VacationsShowDto build(int year, VacationsRecap first, Optional<VacationsRecap> previous) {
		
		//if(first == null || previous == null)
		//	return null;
		
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
			
			if(previous.isPresent()) {
				vacationsShow.previousYearVacationTotal = previous.get().vacationDaysCurrentYearTotal;
				vacationsShow.previousYearVacationAccrued = previous.get().vacationDaysCurrentYearAccrued;
				vacationsShow.previousYearVacationUsed = previous.get().vacationDaysCurrentYearUsed;
			}
			
			vacationsShow.queryYearPermissionTotal = first.permissionCurrentYearTotal;
			vacationsShow.queryYearPermissionAccrued = first.permissionCurrentYearAccrued;
			vacationsShow.queryYearPermissionUsed = first.permissionUsed;
			
		}
		
		return vacationsShow;
	}

}
