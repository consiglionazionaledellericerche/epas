package controllers;

import helpers.Web;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.OfficeManager;
import models.Institute;
import models.Office;
import models.Role;

import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.mysema.query.SearchResults;

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
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static OfficeManager officeManager;
	@Inject
	private static ConfGeneralManager confGeneralManager;
	@Inject
	private static ConfYearManager confYearManager;
	
	@Inject
	private static RoleDao roleDao;

	public static void list(String name) {
		
		//la lista di office su cui si ha tecnical admin. Nel template l'iterazione
		//sui seats.
		
		SearchResults<?> results = officeDao.offices(Optional.<String>fromNullable(name),
				Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN))
				.listResults();
		
		render(results, name);
		
	}
	
	public static void show(Long id) {
//		final Institute institute = Institute.findById(id);
//		notFoundIfNull(institute);
//		render(institute);
	}

	public static void edit(Long id) {
		
//		final Institute institute = Institute.findById(id);
//		notFoundIfNull(institute);
//		render(institute);
	}

	public static void blank() {
//		final Institute institute = new Institute();
//		render("@edit", institute);
	}

	public static void save(@Valid Institute institute) {
		
//		if (Validation.hasErrors()) {
//			response.status = 400;
//			log.warn("validation errors for {}: {}", institute,
//					validation.errorsMap());
//			flash.error(Web.msgHasErrors());
//			render("@edit", institute);
//		} else {
//			institute.save();
//			flash.success(Web.msgSaved(Institute.class));
//			index();
//		}
	}
	
	public static void delete(Long id) {
//		final Institute institute = Institute.findById(id);
//		notFoundIfNull(institute);
//		
//		if(institute.seats.isEmpty()) {
//			institute.delete();
//			flash.success(Web.msgDeleted(Institute.class));
//			index();
//		}
//		flash.error(Web.msgHasErrors());
//		index();
	}
	
	@Deprecated
	public static void showOffices(String name){

		renderText("to implemente");
	}
	

	public static void insertArea() {
		render();
	}

	public static void insertInstitute(Long areaId) {

		Office area = officeDao.getOfficeById(areaId);

		IWrapperOffice wArea = wrapperFactory.create(area);

		if(area==null) {
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices(null);
		}

		render("@editInstitute", area);
	}

	public static void saveInstitute(Office area,@Valid Office institute,@Required String contraction) {

		if(Validation.hasErrors()){
			flash.error("Valorizzare correttamente i campi, operazione annullata.");
			Offices.showOffices(null);
		}

		IWrapperOffice wArea = wrapperFactory.create(area);

		if(!area.isPersistent()) {
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices(null);
		}

		//institute.office = area;

		if(officeDao.checkForDuplicate(institute)){
			flash.error("Parametri già utilizzati in un altro istituto,verificare.");
			Offices.showOffices(null);
		}

		institute.save();

		officeManager.setSystemUserPermission(institute);

		flash.success("Istituto %s con sigla %s correttamente inserito", institute.name);
		Offices.showOffices(null);
	}

	public static void insertSeat(Long instituteId) {
		Office institute = officeDao.getOfficeById(instituteId);
		if(institute==null) {
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices(null);
		}

		render("@editSeat",institute);
	}

	public static void saveSeat(Office institute,@Valid Office seat,@Required int code) {

		if(Validation.hasErrors()){
			flash.error("Valorizzare correttamente tutti i campi! operazione annullata.");
			Offices.showOffices(null);
		}

		if(!institute.isPersistent()) {
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices(null);
		}

		seat.code = code;

		if(officeDao.checkForDuplicate(seat)){
			flash.error("Parametri già utilizzati in un'altra sede,verificare.");
			Offices.showOffices(null);
		}

//		seat.office = institute;

		final boolean newSeat = !seat.isPersistent();

		seat.save();

		if(newSeat){
			confGeneralManager.buildOfficeConfGeneral(seat, false);

			confYearManager.buildOfficeConfYear(seat, LocalDate.now().getYear() - 1, false);
			confYearManager.buildOfficeConfYear(seat, LocalDate.now().getYear(), false);

			officeManager.setSystemUserPermission(seat);
		}

		flash.success("Sede correttamente inserita: %s",seat.name);
		Offices.showOffices(null);
	}

	public static void editSeat(Long seatId){

		Office seat = officeDao.getOfficeById(seatId);

		if(seat==null) {
			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices(null);
		}
//		Office institute = seat.office;

//		render(seat,institute);
	}

	public static void editInstitute(Long instituteId){

		Office institute = officeDao.getOfficeById(instituteId);

		if(institute==null) {
			flash.error("L'istituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices(null);
		}

//		Office area = institute.office;
//
//		render(area,institute);
	}
}
