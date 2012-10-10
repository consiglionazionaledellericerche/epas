package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import java.util.LinkedList;
import java.util.List;

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
	public static void save(@Required Long workingTimeTypeId){
		/**
		 * TODO: completare il metodo per il salvataggio del workingTimeType e dei corrispondenti workingTimeTypeDay
		 */
		WorkingTimeType wtt = WorkingTimeType.findById(workingTimeTypeId);
		if(wtt == null){
			wtt = new WorkingTimeType();
			
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
