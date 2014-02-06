package controllers;

import java.util.List;

import models.VacationCode;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class VacationsAdmin extends Controller{


	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void manageVacationCode(){
		List<VacationCode> vacationCodeList = VacationCode.findAll();
		render(vacationCodeList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void edit(Long vacationCodeId){
		VacationCode vc = VacationCode.findById(vacationCodeId);
		render(vc);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void save(){
		VacationCode vacationCode = new VacationCode();
		vacationCode.description = params.get("nome");
		vacationCode.vacationDays = params.get("giorniFerie", Integer.class);
		vacationCode.permissionDays = params.get("giorniPermesso", Integer.class);
		VacationCode vc = VacationCode.find("Select vc from VacationCode vc where vc.description = ?", params.get("nome")).first();
		if(vc == null){
			vacationCode.save();
			flash.success(String.format("Inserito nuovo piano ferie con nome %s", vacationCode.description));
			VacationsAdmin.manageVacationCode();
		}
		else{
			flash.error(String.format("Esiste già un piano ferie con nome: %s. Cambiare il nome.", params.get("nome")));
			VacationsAdmin.manageVacationCode();
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void update(){
		Long vacationCodeId = params.get("vacationCodeId", Long.class);
		VacationCode code = VacationCode.findById(vacationCodeId);
		code.description = params.get("nome");
		code.vacationDays = params.get("giorniFerie", Integer.class);
		code.permissionDays = params.get("giorniPermesso", Integer.class);
		code.save();
		flash.success("Aggiornato valore del piano ferie %s", code.description);
		VacationsAdmin.manageVacationCode();
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void insertVacationCode(){
		VacationCode vacationCode = new VacationCode();
		render(vacationCode);
	}
	
	@Check(Security.INSERT_AND_UPDATE_VACATIONS)
	public static void discard(){
		manageVacationCode();
	}
	
	
}
