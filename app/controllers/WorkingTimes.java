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
import play.Logger;
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
	
	
	
	public static void updateWorkingTime(){

		long workingTimeTypeId = params.get("wttId", Long.class);
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
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(1).workingTime.equals(mondayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1).workingTime = mondayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(2).workingTime.equals(tuesdayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).workingTime = tuesdayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(3).workingTime.equals(wednesdayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).workingTime = wednesdayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(4).workingTime.equals(thursdayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).workingTime = thursdayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(5).workingTime.equals(fridayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).workingTime = fridayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(6).workingTime.equals(saturdayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).workingTime = saturdayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).save();
		}
		if(!wtt.getWorkingTimeTypeDayFromDayOfWeek(7).workingTime.equals(sundayWorkingMinutes)){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).workingTime = sundayWorkingMinutes;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).save();
		}
		
		Integer mondayMinimalTimeLunch = params.get("mondayMinimalTimeLunch", Integer.class);
		Integer tuesdayMinimalTimeLunch = params.get("tuesdayMinimalTimeLunch", Integer.class);
		Integer wednesdayMinimalTimeLunch = params.get("wednesdayMinimalTimeLunch", Integer.class);
		Integer thursdayMinimalTimeLunch = params.get("thursdayMinimalTimeLunch", Integer.class);
		Integer fridayMinimalTimeLunch = params.get("fridayMinimalTimeLunch", Integer.class);
		Integer saturdayMinimalTimeLunch = params.get("saturdayMinimalTimeLunch", Integer.class);
		Integer sundayMinimalTimeLunch = params.get("sundayMinimalTimeLunch", Integer.class);
		if(wtt.getMinimalTimeForLunch(1, wtt) != mondayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1).mealTicketTime = mondayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1);
		}
		if(wtt.getMinimalTimeForLunch(2, wtt) != tuesdayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).mealTicketTime = tuesdayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).save();
		}
		if(wtt.getMinimalTimeForLunch(3, wtt) != wednesdayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).mealTicketTime = wednesdayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).save();
		}
		if(wtt.getMinimalTimeForLunch(4, wtt) != thursdayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).mealTicketTime = thursdayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).save();
		}
		if(wtt.getMinimalTimeForLunch(5, wtt) != fridayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).mealTicketTime = fridayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).save();
		}
		if(wtt.getMinimalTimeForLunch(6, wtt) != saturdayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).mealTicketTime = saturdayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).save();
		}
		if(wtt.getMinimalTimeForLunch(7, wtt) != sundayMinimalTimeLunch){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).mealTicketTime = sundayMinimalTimeLunch;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).save();
		}
		
		Integer mondayBreakTime = params.get("mondayBreakTime", Integer.class);
		Integer tuesdayBreakTime = params.get("tuesdayBreakTime", Integer.class);
		Integer wednesdayBreakTime = params.get("wednesdayBreakTime", Integer.class);
		Integer thursdayBreakTime = params.get("thursdayBreakTime", Integer.class);
		Integer fridayBreakTime = params.get("fridayBreakTime", Integer.class);
		Integer saturdayBreakTime = params.get("saturdayBreakTime", Integer.class);
		Integer sundayBreakTime = params.get("sundayBreakTime", Integer.class);
		if(wtt.getBreakTime(1, wtt) != mondayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1).breakTicketTime = mondayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(1).save();
		}
		if(wtt.getBreakTime(2, wtt) != tuesdayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).breakTicketTime = tuesdayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(2).save();
		}
		if(wtt.getBreakTime(3, wtt) != wednesdayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).breakTicketTime = wednesdayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(3).save();
		}
		if(wtt.getBreakTime(4, wtt) != thursdayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).breakTicketTime = thursdayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(4).save();
		}
		if(wtt.getBreakTime(5, wtt) != fridayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).breakTicketTime = fridayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(5).save();
		}
		if(wtt.getBreakTime(6, wtt) != saturdayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).breakTicketTime = saturdayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(6).save();
		}
		if(wtt.getBreakTime(7, wtt) != sundayBreakTime){
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).breakTicketTime = sundayBreakTime;
			wtt.getWorkingTimeTypeDayFromDayOfWeek(7).save();
		}
		
		String mondayHoliday = params.get("mondayHoliday", String.class);
		String tuesdayHoliday = params.get("tuesdayHoliday",String.class);
		String wednesdayHoliday = params.get("wednesdayHoliday",String.class);
		String thursdayHoliday = params.get("thursdayHoliday", String.class);
		String fridayHoliday = params.get("fridayHoliday", String.class);
		String saturdayHoliday = params.get("saturdayHoliday", String.class);
		String sundayHoliday = params.get("sundayHoliday", String.class);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", mondayHoliday);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", tuesdayHoliday);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", wednesdayHoliday);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", thursdayHoliday);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", fridayHoliday);
		Logger.debug("Il valore preso dalla form per festività di giovedi è: %s", saturdayHoliday);
		Logger.debug("Il valore preso dalla form per festività di domenica è: %s", sundayHoliday);
		WorkingTimeTypeDay wttd1 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 1).first();
		WorkingTimeTypeDay wttd2 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 2).first();
		WorkingTimeTypeDay wttd3 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 3).first();
		WorkingTimeTypeDay wttd4 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 4).first();
		WorkingTimeTypeDay wttd5 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 5).first();
		WorkingTimeTypeDay wttd6 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 6).first();
		WorkingTimeTypeDay wttd7 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ? " +
				"and wttd.dayOfWeek = ?", wtt, 7).first();

		if(mondayHoliday == null || mondayHoliday.equals("no")){
			
			wttd1.holiday = false;
		}
		else
			wttd1.holiday = true;
		
		wttd1.save();

		if(tuesdayHoliday == null || tuesdayHoliday.equals("no")){
			wttd2.holiday = false;
		}
		else
			wttd2.holiday = true;
		wttd2.save();

		if(wednesdayHoliday == null || wednesdayHoliday.equals("no")){
			wttd3.holiday = false;
		}
		else
			wttd3.holiday = true;
		wttd3.save();

		if(thursdayHoliday == null || thursdayHoliday.equals("no")){
			wttd4.holiday = false;
		}
		else
			wttd4.holiday = true;
		wttd4.save();

		if(fridayHoliday == null || fridayHoliday.equals("no")){
			wttd5.holiday = false;
		}
		else
			wttd5.holiday = true;
		wttd5.save();

		if(saturdayHoliday == null || saturdayHoliday.equals("no")){
			wttd6.holiday = false;
		}
		else
			wttd6.holiday = true;
		wttd6.save();

		if(sundayHoliday == null || sundayHoliday.equals("no")){
			wttd7.holiday = false;
		}
		else
			wttd7.holiday = true;
		wttd7.save();
		
		Boolean shift = params.get("turni", Boolean.class);
		if(shift == null || shift == false)
			wtt.shift = false;
		else 
			wtt.shift = true;
		
		wtt.save();
		
		flash.success("Aggiornato orario di lavoro con nuovo nome %s '", description + "'");
		Application.indexAdmin();
		
	}
	
	public static void edit(@Required Long workingTimeTypeId){
		
		WorkingTimeType wtt = WorkingTimeType.findById(workingTimeTypeId);
    	if (wtt == null) {
    		notFound();
    	}

		render(wtt);	
		
	}
}
