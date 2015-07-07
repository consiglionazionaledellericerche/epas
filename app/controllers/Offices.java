package controllers;

import helpers.ValidationHelper;

import java.util.List;

import javax.inject.Inject;

import manager.OfficeManager;
import models.Office;
import models.Role;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.collect.FluentIterable;

import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class})
public class Offices extends Controller {
	
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static RoleDao roleDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static OfficeManager officeManager;

	public static void showOffices(){

		List<IWrapperOffice> allAreas = FluentIterable
				.from(officeDao.getAreas()).transform(wrapperFunctionFactory.office()).toList();
		
		Role roleAdmin = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);

		render(allAreas, roleAdmin, roleAdminMini);
	}

	public static void insertArea() {
		render();
	}

	public static void insertInstitute(Long areaId) {

		Office area = officeDao.getOfficeById(areaId);

		IWrapperOffice wArea = wrapperFactory.create(area);

		if(area==null || !wArea.isArea()) {
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		render("@editInstitute",area);
	}

	public static void saveInstitute(Office area,@Valid Office institute) {

		if(Validation.hasErrors()){
			flash.error(ValidationHelper.errorsMessages(validation.errors()));
			Offices.showOffices();
		}

		IWrapperOffice wArea = wrapperFactory.create(area);

		if(!area.isPersistent() || !wArea.isArea()) {
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		institute.office = area;
		
		officeManager.saveOffice(institute);
//		if(!){
//			flash.error("Parametri già utilizzati in un altro istituto,verificare.");
//			Offices.showOffices();
//		}

		flash.success("Istituto %s con sigla %s correttamente inserito", institute.name, institute.contraction);
		Offices.showOffices();
	}

	public static void insertSeat(Long instituteId) {
		Office institute = officeDao.getOfficeById(instituteId);
		if(institute==null) {
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		render("@editSeat",institute);
	}

	public static void saveSeat(Office institute,@Valid Office seat) {

		if(Validation.hasErrors()){
			flash.error(ValidationHelper.errorsMessages(validation.errors()));
			Offices.showOffices();
		}

		if(!institute.isPersistent()) {
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		seat.office = institute;
		officeManager.saveOffice(seat);

		flash.success("Sede correttamente inserita: %s",seat.name);
		Offices.showOffices();
	}

	public static void editSeat(Long seatId){

		Office seat = officeDao.getOfficeById(seatId);

		if(seat==null) {
			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}
		Office institute = seat.office;

		render(seat,institute);
	}

	public static void editInstitute(Long instituteId){

		Office institute = officeDao.getOfficeById(instituteId);

		if(institute==null) {
			flash.error("L'istituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		Office area = institute.office;

		render(area,institute);
	}
}
