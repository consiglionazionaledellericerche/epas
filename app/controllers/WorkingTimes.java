package controllers;

import java.util.List;

import models.VacationCode;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.mvc.Controller;

public class WorkingTimes extends Controller{

	/**
	 * ritorna la lista dei workingTimeType da cui posso accedere ai workingTimeTypeDays per le caratteristiche che devo evidenziare
	 * nella view
	 */
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void manageWorkingTime(){
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		
		render(wttList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void insertWorkingTime(){
		/**
		 * TODO: completare il controller 
		 */
	}
	
}
