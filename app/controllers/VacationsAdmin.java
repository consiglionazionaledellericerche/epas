package controllers;

import java.util.List;

import models.VacationCode;
import play.mvc.Controller;

public class VacationsAdmin extends Controller{

	/**
	 * funzione che ritorna la lista degli attuali tipi di ferie
	 */
	public static void manageVacationCode(){
		List<VacationCode> vacationCodeList = VacationCode.findAll();
		render(vacationCodeList);
	}
	
	public static void insertVacationCode(){
		VacationCode vacationCode = new VacationCode();
		vacationCode.description = params.get("nome");
		vacationCode.vacationDays = new Integer(params.get("giorniFerie"));
		vacationCode.permissionDays = new Integer(params.get("giorniPermesso"));
		vacationCode.save();
	}
	
}
