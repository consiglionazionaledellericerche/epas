package controllers;

import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.OfficeManager;
import models.Office;
import models.Role;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.collect.FluentIterable;

import controllers.Resecure.NoCheck;
import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class})
public class Offices extends Controller {

	@Inject
	static SecurityRules rules;

	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory;
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static OfficeManager officeManager;

	@NoCheck
	public static void showOffices(){

		List<IWrapperOffice> allAreas = FluentIterable
				.from(OfficeDao.getAreas()).transform(wrapperFunctionFactory.office()).toList();

		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);

		render(allAreas, roleAdmin, roleAdminMini);
	}

	@NoCheck
	public static void insertArea() {

		render();
	}

	@NoCheck
	public static void insertInstitute(Long areaId) {

		Office area = officeDao.getOfficeById(areaId);
		if(area == null || !OfficeManager.isArea(area)) {

			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(area);

		render(area);
	}

	@NoCheck
	public static void saveInstitute(Long areaId, String name, String contraction) {

		Office area = officeDao.getOfficeById(areaId);

		if(area == null || !OfficeManager.isArea(area)) {

			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(area);

		if( name == null || name.equals("") || contraction == null || contraction.equals("") ) {

			flash.error("Valorizzare correttamente entrambi i campi, operazione annullata.");
			Offices.showOffices();
		}
		Office office = officeDao.getOfficeByName(name);
		String message = officeManager.checkIfExists(office, name, contraction); 
		if(!message.equals("")){
			flash.error(message);
			Offices.showOffices();
		}
		office = new Office();
		OfficeManager.saveInstitute(office, area, name, contraction);

		officeManager.setPermissionAfterCreation(office);

		flash.success("Istituto %s con sigla %s correttamente inserito", name, contraction);
		Offices.showOffices();
	}

	@NoCheck
	public static void insertSeat(Long instituteId) {
		Office institute = officeDao.getOfficeById(instituteId);
		if(institute==null) {

			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(institute);

		render(institute);
	}

	@NoCheck
	public static void saveSeat(Long instituteId, String name, String address, String code, String date) {

		Office institute = officeDao.getOfficeById(instituteId);
		if(institute==null) {

			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(institute);

		//Parametri null
		if( isNullOrEmpty(name) || isNullOrEmpty(address) || isNullOrEmpty(code) || isNullOrEmpty(date) ){
			flash.error("Errore. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}

		String message = OfficeManager.checkIfExistsSeat(code, date);
		if(!message.equals("")){
			flash.error(message);
			Offices.showOffices();
		}

		//codice esistente
		Office alreadyExist = OfficeDao.getOfficeByCode(OfficeManager.getInteger(code));
		if(alreadyExist!=null){
			flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}
		Office office = new Office();
		OfficeManager.saveSeat(office, name, address, code, date, institute);

		ConfGeneralManager.buildDefaultConfGeneral(office);

		ConfYearManager.buildDefaultConfYear(office, LocalDate.now().getYear());
		ConfYearManager.buildDefaultConfYear(office, LocalDate.now().getYear() - 1);		

		officeManager.setPermissionAfterCreation(office);

		flash.success("Sede correttamente inserita");
		Offices.showOffices();
	}

	@NoCheck
	public static void editSeat(Long officeId){

		Office office = officeDao.getOfficeById(officeId);

		if(office==null) {

			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(office);

		render(office);

	}

	@NoCheck
	public static void updateSeat(Long officeId, String name, String address, String code, String date) {

		Office office = officeDao.getOfficeById(officeId);
		if(office==null) {

			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(office);

		//Parametri null
		if( isNullOrEmpty(name) || isNullOrEmpty(address) || isNullOrEmpty(code) || isNullOrEmpty(date) ){
			flash.error("Valorizzare correttamente tutti i parametri. Operazione annullata.");
			Offices.showOffices();
		}

		//errore campo data
		String message = OfficeManager.checkIfExistsSeat(code, date);
		if(!message.equals("")){
			flash.error(message);
			Offices.showOffices();
		}

		//codice uguale a sedi diverse da remoteOffice
		List<Office> officeList = OfficeDao.getOfficesByCode(OfficeManager.getInteger(code));
		for(Office off : officeList) {

			if( !off.id.equals(office.id) ) {

				flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
				Offices.showOffices();
			}
		}
		OfficeManager.updateSeat(office, name, address, code, date);
		
		flash.success("Sede correttamente modificata");
		Offices.showOffices();
	}



	private static boolean isNullOrEmpty(String parameter)
	{
		if( (parameter==null || parameter.equals("") ))
			return true;
		return false;
	}



}
