package controllers;

import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.OfficeManager;
import models.Office;
import models.Role;

import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class})
public class Offices extends Controller {

	@Inject
	static SecurityRules rules;

	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory;

	@Inject
	static IWrapperFactory wrapperFactory;

	@Inject
	static OfficeDao officeDao;

	@Inject
	static OfficeManager officeManager;

	public static void showOffices(){

		List<IWrapperOffice> allAreas = FluentIterable
				.from(officeDao.getAreas()).transform(wrapperFunctionFactory.office()).toList();

		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);

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

	public static void saveInstitute(Office area,@Valid Office institute,@Required String contraction) {

		if(Validation.hasErrors()){
			flash.error("Valorizzare correttamente i campi, operazione annullata.");
			Offices.showOffices();
		}

		IWrapperOffice wArea = wrapperFactory.create(area);

		if(!area.isPersistent() || !wArea.isArea()) {
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		institute.contraction = contraction;
		institute.office = area;

		if(officeDao.checkForDuplicate(institute)){
			flash.error("Parametri già utilizzati in un altro istituto,verificare.");
			Offices.showOffices();
		}

		institute.save();

		officeManager.setPermissionAfterCreation(institute);

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

	public static void saveSeat(Office institute,@Valid Office seat,@Required int code) {

		if(Validation.hasErrors()){
			flash.error("Valorizzare correttamente tutti i campi! operazione annullata.");
			Offices.showOffices();
		}

		if(!institute.isPersistent()) {
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		seat.code = code;

		if(officeDao.checkForDuplicate(seat)){
			flash.error("Parametri già utilizzati in un'altra sede,verificare.");
			Offices.showOffices();
		}

		seat.office = institute;
		
		final boolean newSeat = !seat.isPersistent();
		
		seat.save();

		if(newSeat){
			ConfGeneralManager.buildOfficeConfGeneral(seat, false);

			ConfYearManager.buildOfficeConfYear(seat, LocalDate.now().getYear() - 1, false);
			ConfYearManager.buildOfficeConfYear(seat, LocalDate.now().getYear(), false);

			officeManager.setPermissionAfterCreation(seat);
		}

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
