package controllers;

import java.util.LinkedList;
import java.util.List;

import models.Contract;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class WorkingTimes extends Controller{

	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void manageWorkingTime(){
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		render(wttList);
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
	public static void discard(){
		manageWorkingTime();
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void save(
			WorkingTimeType wtt, 
			WorkingTimeTypeDay wttd1,
			WorkingTimeTypeDay wttd2,
			WorkingTimeTypeDay wttd3,
			WorkingTimeTypeDay wttd4,
			WorkingTimeTypeDay wttd5,
			WorkingTimeTypeDay wttd6,
			WorkingTimeTypeDay wttd7){
			
			if(wtt.description == null || wtt.description.isEmpty())
			{
				flash.error("Il campo nome tipo orario è obbligatorio. Operazione annullata");
				WorkingTimes.manageWorkingTime();
			}
			if( WorkingTimeType.find("byDescription", wtt.description).first() != null)
			{
				flash.error("Il nome tipo orario è già esistente. Sceglierne un'altro. Operazione annullata");
				WorkingTimes.manageWorkingTime();
			}
			wtt.save();
			
			wttd1.dayOfWeek = 1;
			wttd1.workingTimeType = wtt;
			wttd1.save();
			
			wttd2.dayOfWeek = 2;
			wttd2.workingTimeType = wtt;
			wttd2.save();
			
			wttd3.dayOfWeek = 3;
			wttd3.workingTimeType = wtt;
			wttd3.save();
			
			wttd4.dayOfWeek = 4;
			wttd4.workingTimeType = wtt;
			wttd4.save();
			
			wttd5.dayOfWeek = 5;
			wttd5.workingTimeType = wtt;
			wttd5.save();
			
			wttd6.dayOfWeek = 6;
			wttd6.workingTimeType = wtt;
			wttd6.save();
			
			wttd7.dayOfWeek = 7;
			wttd7.workingTimeType = wtt;
			wttd7.save();
		
			flash.success("Inserito nuovo orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime();

	}
	

	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void updateWorkingTime(
			WorkingTimeType wtt, 
			WorkingTimeTypeDay wttd1,
			WorkingTimeTypeDay wttd2,
			WorkingTimeTypeDay wttd3,
			WorkingTimeTypeDay wttd4,
			WorkingTimeTypeDay wttd5,
			WorkingTimeTypeDay wttd6,
			WorkingTimeTypeDay wttd7){

		//descrizione vuota
		if(wtt.description==null || wtt.description.isEmpty())
		{
			flash.error("Il campo nome tipo orario è obbligatorio. Operazione annullata");
			WorkingTimes.manageWorkingTime();
		}
		
		//descrizione già esistente
		WorkingTimeType wttExist = WorkingTimeType.find("byDescription", wtt.description).first();
		if(wttExist.id!=wtt.id)
		{
			flash.error("Il nome tipo orario è già esistente. Sceglierne un'altro. Operazione annullata");
			WorkingTimes.manageWorkingTime();
		}
		
		wtt.save();
		wttd1.properSave();
		wttd2.properSave();
		wttd3.properSave();
		wttd4.properSave();
		wttd5.properSave();
		wttd6.properSave();
		wttd7.properSave();
		
		
		flash.success("Aggiornato orario di lavoro denominato %s.", wtt.description);
		WorkingTimes.manageWorkingTime();
		
	}
	
	public static void edit(@Required Long workingTimeTypeId){
		
		WorkingTimeType wtt = WorkingTimeType.findById(workingTimeTypeId);
    	if (wtt == null) {
    		notFound();
    	}

		render(wtt);	
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void delete(WorkingTimeType wtt){

		//descrizione vuota
		if(wtt.description==null || wtt.description.isEmpty())
		{
			flash.error("Il campo nome tipo orario è obbligatorio. Operazione annullata");
			WorkingTimes.manageWorkingTime();
		}
		if(wtt.contractWorkingTimeType.size()!=0)
		{
			flash.error("Impossibile eliminare il tipo orario selezionato perchè assegnato ad almeno un contratto storicizzato. Operazione annullata");
			WorkingTimes.manageWorkingTime();
		}
		for(WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays)
		{
			wttd.delete();
		}
		wtt.delete();
		
		flash.success("Eliminato orario di lavoro denominato %s.", wtt.description);
		WorkingTimes.manageWorkingTime();
		
	}
	
}
