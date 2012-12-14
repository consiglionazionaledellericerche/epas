package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import models.Stamping;
import models.VacationCode;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.data.validation.Required;
import play.mvc.Controller;

public class WorkingTimes extends Controller{

	private final static ActionMenuItem actionMenuItem = ActionMenuItem.manageWorkingTime;
	/**
	 * ritorna la lista dei workingTimeType da cui posso accedere ai workingTimeTypeDays per le caratteristiche che devo evidenziare
	 * nella view
	 */
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void manageWorkingTime(){
		String menuItem = actionMenuItem.toString();
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		
		render(wttList,menuItem);
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void insertWorkingTime(){
		List<WorkingTimeTypeDay> wttd = new LinkedList<WorkingTimeTypeDay>();
		WorkingTimeType wtt = new WorkingTimeType();
		for(int i = 1; i < 8; i++){
			WorkingTimeTypeDay w = new WorkingTimeTypeDay();
			wttd.add(w);
		}
		render(wtt, wttd);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void save(Long workingTimeTypeId){
		/**
		 * TODO: completare il metodo per il salvataggio del workingTimeType e dei corrispondenti workingTimeTypeDay
		 */
		if(workingTimeTypeId == null){
			String nameWorkingTime = params.get("nuovoOrario");
			WorkingTimeType wtt = new WorkingTimeType();
			wtt.description = nameWorkingTime;
			wtt.save();
			Map<Integer, String> workingTimeMap = new HashMap<Integer, String>();
			Map<Integer, String> mealTicketTimeMap = new HashMap<Integer, String>();
			Map<Integer, String> breakTicketTimeMap = new HashMap<Integer, String>();
			Map<Integer, String> holidayMap = new HashMap<Integer, String>();
			
			workingTimeMap.put(1, params.get("tempoLavoroLunedi"));
			workingTimeMap.put(2, params.get("tempoLavoroMartedi"));
			workingTimeMap.put(3, params.get("tempoLavoroMercoledi"));
			workingTimeMap.put(4, params.get("tempoLavoroGiovedi"));
			workingTimeMap.put(5, params.get("tempoLavoroVenerdi"));
			workingTimeMap.put(6, params.get("tempoLavoroSabato"));
			workingTimeMap.put(7, params.get("tempoLavoroDomenica"));
			
			mealTicketTimeMap.put(1, params.get("tempoMinimoPranzoLunedi"));
			mealTicketTimeMap.put(2, params.get("tempoMinimoPranzoMartedi"));
			mealTicketTimeMap.put(3, params.get("tempoMinimoPranzoMercoledi"));
			mealTicketTimeMap.put(4, params.get("tempoMinimoPranzoGiovedi"));
			mealTicketTimeMap.put(5, params.get("tempoMinimoPranzoVenerdi"));
			mealTicketTimeMap.put(6, params.get("tempoMinimoPranzoSabato"));
			mealTicketTimeMap.put(7, params.get("tempoMinimoPranzoDomenica"));
			
			breakTicketTimeMap.put(1, params.get("tempoPerPranzoLunedi"));
			breakTicketTimeMap.put(2, params.get("tempoPerPranzoMartedi"));
			breakTicketTimeMap.put(3, params.get("tempoPerPranzoMercoledi"));
			breakTicketTimeMap.put(4, params.get("tempoPerPranzoGiovedi"));
			breakTicketTimeMap.put(5, params.get("tempoPerPranzoVenerdi"));
			breakTicketTimeMap.put(6, params.get("tempoPerPranzoSabato"));
			breakTicketTimeMap.put(7, params.get("tempoPerPranzoDomenica"));
			
			holidayMap.put(1, params.get("festivoLunedi"));
			holidayMap.put(2, params.get("festivoMartedi"));
			holidayMap.put(3, params.get("festivoMercoledi"));
			holidayMap.put(4, params.get("festivoGiovedi"));
			holidayMap.put(5, params.get("festivoVenerdi"));
			holidayMap.put(6, params.get("festivoSabato"));
			holidayMap.put(7, params.get("festivoDomenica"));			
			
			for(int i = 1; i < 8; i++){
				WorkingTimeTypeDay w = new WorkingTimeTypeDay();
				w.dayOfWeek = i;
				w.workingTime = new Integer(workingTimeMap.get(i));
				w.mealTicketTime = new Integer(mealTicketTimeMap.get(i));
				w.breakTicketTime = new Integer(breakTicketTimeMap.get(i));
				if(new Boolean(holidayMap.get(i))== true)
					w.holiday = true;
				else 
					w.holiday = false;
				w.workingTimeType = wtt;
				w.save();
								
			}
			flash.success(String.format("Inserito nuovo orario di lavoro denominato %s '", nameWorkingTime + "'"));
			Application.indexAdmin();
		}
		else{
			
		}		
		
	}
	
	
	
	public static void updateWorkingTime(Long workingTimeTypeId){
		/**
		 * TODO: completare il metodo di modifica
		 */
		WorkingTimeType wtt = WorkingTimeType.findById(workingTimeTypeId);
		String description = params.get("workingTimeTypeDescription");
		if(!wtt.description.equals(description)){
			wtt.description = description;
		}
		Integer mondayWorkingMinutes = params.get("mondayWorkingTime", Integer.class);
		Integer tuesdayWorkingMinutes = params.get("tuesdayWorkingTime", Integer.class);
		Integer wednesdayWorkingMinutes = params.get("wednesdayWorkingTime", Integer.class);
		Integer thursdayWorkingMinutes = params.get("thursdayWorkingTime", Integer.class);
		Integer fridayWorkingMinutes = params.get("fridayWorkingTime", Integer.class);
		Integer saturdayWorkingMinutes = params.get("saturdayWorkingTime", Integer.class);
		Integer sundayWorkingMinutes = params.get("sundayWorkingTime", Integer.class);
		if(wtt.getWorkingTimeFromWorkinTimeType(1).workingTime != mondayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(1).workingTime = mondayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(1).save();
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(2).workingTime != tuesdayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(2).workingTime = tuesdayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(2).save();
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(3).workingTime != wednesdayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(3).workingTime = wednesdayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(3);
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(4).workingTime != thursdayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(4).workingTime = thursdayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(4);
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(5).workingTime != fridayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(5).workingTime = fridayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(5);
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(6).workingTime != saturdayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(6).workingTime = saturdayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(6);
		}
		if(wtt.getWorkingTimeFromWorkinTimeType(7).workingTime != sundayWorkingMinutes){
			wtt.getWorkingTimeFromWorkinTimeType(7).workingTime = sundayWorkingMinutes;
			wtt.getWorkingTimeFromWorkinTimeType(7);
		}
		
		Integer mondayMinimalTimeLunch = params.get("mondayMinimalTimeLunch", Integer.class);
		Integer tuesdayMinimalTimeLunch = params.get("tuesdayMinimalTimeLunch", Integer.class);
		Integer wednesdayMinimalTimeLunch = params.get("wednesdayMinimalTimeLunch", Integer.class);
		Integer thursdayMinimalTimeLunch = params.get("thursdayMinimalTimeLunch", Integer.class);
		Integer fridayMinimalTimeLunch = params.get("fridayMinimalTimeLunch", Integer.class);
		Integer saturdayMinimalTimeLunch = params.get("saturdayMinimalTimeLunch", Integer.class);
		Integer sundayMinimalTimeLunch = params.get("sundayMinimalTimeLunch", Integer.class);
		if(wtt.getMinimalTimeForLunch(1, wtt) != mondayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(1).mealTicketTime = mondayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(1);
		}
		if(wtt.getMinimalTimeForLunch(2, wtt) != tuesdayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(2).mealTicketTime = tuesdayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(2);
		}
		if(wtt.getMinimalTimeForLunch(3, wtt) != wednesdayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(3).mealTicketTime = wednesdayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(3);
		}
		if(wtt.getMinimalTimeForLunch(4, wtt) != thursdayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(4).mealTicketTime = thursdayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(4);
		}
		if(wtt.getMinimalTimeForLunch(5, wtt) != fridayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(5).mealTicketTime = fridayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(5);
		}
		if(wtt.getMinimalTimeForLunch(6, wtt) != saturdayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(6).mealTicketTime = saturdayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(6);
		}
		if(wtt.getMinimalTimeForLunch(7, wtt) != sundayMinimalTimeLunch){
			wtt.getWorkingTimeFromWorkinTimeType(7).mealTicketTime = sundayMinimalTimeLunch;
			wtt.getWorkingTimeFromWorkinTimeType(7);
		}
		
		Integer mondayBreakTime = params.get("mondayBreakTime", Integer.class);
		Integer tuesdayBreakTime = params.get("tuesdayBreakTime", Integer.class);
		Integer wednesdayBreakTime = params.get("wednesdayBreakTime", Integer.class);
		Integer thursdayBreakTime = params.get("thursdayBreakTime", Integer.class);
		Integer fridayBreakTime = params.get("fridayBreakTime", Integer.class);
		Integer saturdayBreakTime = params.get("saturdayBreakTime", Integer.class);
		Integer sundayBreakTime = params.get("sundayBreakTime", Integer.class);
		if(wtt.getBreakTime(1, wtt) != mondayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(1).breakTicketTime = mondayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(1);
		}
		if(wtt.getBreakTime(2, wtt) != tuesdayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(2).breakTicketTime = tuesdayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(2);
		}
		if(wtt.getBreakTime(3, wtt) != wednesdayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(3).breakTicketTime = wednesdayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(3);
		}
		if(wtt.getBreakTime(4, wtt) != thursdayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(4).breakTicketTime = thursdayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(4);
		}
		if(wtt.getBreakTime(5, wtt) != fridayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(5).breakTicketTime = fridayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(5);
		}
		if(wtt.getBreakTime(6, wtt) != saturdayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(6).breakTicketTime = saturdayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(6);
		}
		if(wtt.getBreakTime(7, wtt) != sundayBreakTime){
			wtt.getWorkingTimeFromWorkinTimeType(7).breakTicketTime = sundayBreakTime;
			wtt.getWorkingTimeFromWorkinTimeType(7);
		}
		
		Boolean mondayHoliday = params.get("mondayHoliday", Boolean.class);
		Boolean tuesdayHoliday = params.get("tuesdayHoliday", Boolean.class);
		Boolean wednesdayHoliday = params.get("wednesdayHoliday", Boolean.class);
		Boolean thursdayHoliday = params.get("thursdayHoliday", Boolean.class);
		Boolean fridayHoliday = params.get("fridayHoliday", Boolean.class);
		Boolean saturdayHoliday = params.get("saturdayHoliday", Boolean.class);
		Boolean sundayHoliday = params.get("sundayHoliday", Boolean.class);
		if(wtt.getHolidayFromWorkinTimeType(1, wtt) != mondayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(1).holiday = mondayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(1);
		}
		if(wtt.getHolidayFromWorkinTimeType(2, wtt) != tuesdayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(2).holiday = tuesdayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(2);
		}
		if(wtt.getHolidayFromWorkinTimeType(3, wtt) != wednesdayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(3).holiday = wednesdayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(3);
		}
		if(wtt.getHolidayFromWorkinTimeType(4, wtt) != thursdayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(4).holiday = thursdayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(4);
		}
		if(wtt.getHolidayFromWorkinTimeType(5, wtt) != fridayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(5).holiday = fridayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(5);
		}
		if(wtt.getHolidayFromWorkinTimeType(6, wtt) != saturdayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(6).holiday = saturdayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(6);
		}
		if(wtt.getHolidayFromWorkinTimeType(7, wtt) != sundayHoliday){
			wtt.getWorkingTimeFromWorkinTimeType(7).holiday = sundayHoliday;
			wtt.getWorkingTimeFromWorkinTimeType(7);
		}
		Boolean defaultWorkingTime = params.get("default", Boolean.class);
		/**
		 * scommentare quando si è capito come inserire una nuova colonna nella classe senza generare disastri
		 */
//		if(wtt.defaultWorkingTimeType != defaultWorkingTime)
//			wtt.defaultWorkingTimeType = defaultWorkingTime;
		
		Boolean shift = params.get("turni", Boolean.class);
		if(wtt.shift != shift)
			wtt.shift = shift;

		wtt.save();
		
		flash.success(String.format("Aggiornato orario di lavoro con nuovo nome %s '", description + "'"));
		Application.indexAdmin();
		
	}
	
	public static void edit(@Required Long workingTimeTypeId){
		/**
		 * TODO: completare il metodo edit
		 */
		WorkingTimeType wtt = WorkingTimeType.findById(workingTimeTypeId);
    	if (wtt == null) {
    		notFound();
    	}

		render(wtt);	
		
	}
}
