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
