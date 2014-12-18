package controllers;

import java.util.List;

import javax.inject.Inject;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.Role;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import controllers.Resecure.NoCheck;
import dao.OfficeDao;
import dao.RoleDao;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With( {Resecure.class, RequestInit.class})
public class Offices extends Controller {

	@Inject
	static SecurityRules rules;
	
	@NoCheck
	public static void showOffices(){
		
		List<Office> allAreas = Office.getAllAreas();
		
		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
//		Role roleAdmin = Role.find("byName", Role.PERSONNEL_ADMIN).first();
//		Role roleAdminMini = Role.find("byName", Role.PERSONNEL_ADMIN_MINI).first();
		
		render(allAreas, roleAdmin, roleAdminMini);
	}
	
	@NoCheck
	public static void insertArea() {
		
		render();
	}
	
	@NoCheck
	public static void insertInstitute(Long areaId) {
		
		Office area = OfficeDao.getOfficeById(areaId);
		//Office area = Office.findById(areaId);
		
		if(area == null || !area.isArea()) {
			
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}
		
		rules.checkIfPermitted(area);
		
		render(area);
	}
	
	@NoCheck
	public static void saveInstitute(Long areaId, String name, String contraction) {
		
		Office area = OfficeDao.getOfficeById(areaId);
		//Office area = Office.findById(areaId);
		
		if(area == null || !area.isArea()) {
			
			flash.error("L'area specificata è inesistente. Operazione annullata.");
			Offices.showOffices();
		}

		rules.checkIfPermitted(area);
		
		if( name == null || name.equals("") || contraction == null || contraction.equals("") ) {
			
			flash.error("Valorizzare correttamente entrambi i campi, operazione annullata.");
			Offices.showOffices();
		}
		
		Office office = OfficeDao.getOfficeByNameOrByContraction(Optional.fromNullable(name), Optional.<String>absent());
		//Office office = Office.find("byName",name).first();
		if( office != null ) {
			
			flash.error("Esiste gia' un istituto con nome %s, operazione annullata.", name);
			Offices.showOffices();
		}
		
		office = OfficeDao.getOfficeByNameOrByContraction(Optional.<String>absent(), Optional.fromNullable(contraction));
		//office = Office.find("byContraction",name).first();
		if( office != null ) {
			
			flash.error("Esiste gia' un istituto con sigla %s, operazione annullata.", contraction);
			Offices.showOffices();
		}

		office = new Office();
		office.name = name;
		office.contraction = contraction;
		office.office = area;
		office.save();
		
		office.setPermissionAfterCreation();
		
		flash.success("Istituto %s con sigla %s correttamente inserito", name, contraction);
		Offices.showOffices();
	}
			
	@NoCheck
	public static void insertSeat(Long instituteId) {
		Office institute = OfficeDao.getOfficeById(instituteId);
		//Office institute = Office.findById(instituteId);
		if(institute==null) {
			
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}
		
		rules.checkIfPermitted(institute);
		
		render(institute);
	}
	
	@NoCheck
	public static void saveSeat(Long instituteId, String name, String address, String code, String date) {
	
		Office institute = OfficeDao.getOfficeById(instituteId);
		//Office institute = Office.findById(instituteId);
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

		//errore campo data
		if(getLocalDate(date)==null){
			flash.error("Errore nell'inserimento del campo data. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}

		//errore campo sede
		if(getInteger(code)==null){
			flash.error("Errore nell'inserimento del campo codice sede. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}

		//codice esistente
		Office alreadyExist = OfficeDao.getOfficeByCode(getInteger(code));
		//Office alreadyExist = Office.find("Select o from Office o where o.code = ?", getInteger(code)).first();
		if(alreadyExist!=null){
			flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}
		
		Office office = new Office();
		office.name = name;
		office.address = address;
		office.code = getInteger(code);
		office.joiningDate = getLocalDate(date);
		office.office = institute;
		office.save();
		
		//ConfGeneral
		ConfGeneral.buildDefaultConfGeneral(office);
		
		//ConfYear
		ConfYear.buildDefaultConfYear(office, LocalDate.now().getYear());
		ConfYear.buildDefaultConfYear(office, LocalDate.now().getYear() - 1);
		
		
		office.setPermissionAfterCreation();
		
		flash.success("Sede correttamente inserita");
		Offices.showOffices();
	}

	@NoCheck
	public static void editSeat(Long officeId){
		
		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
		
		if(office==null) {
			
			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}
		
		rules.checkIfPermitted(office);
		
		render(office);
		
	}
	
	@NoCheck
	public static void updateSeat(Long officeId, String name, String address, String code, String date) {
	
		Office office = OfficeDao.getOfficeById(officeId);
		//Office office = Office.findById(officeId);
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
		if(getLocalDate(date)==null){
			flash.error("Errore nell'inserimento del campo data. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}

		//errore campo sede
		if(getInteger(code)==null){
			flash.error("Errore nell'inserimento del campo codice sede. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}

		//codice uguale a sedi diverse da remoteOffice
		List<Office> officeList = OfficeDao.getOfficesByCode(getInteger(code));
		//List<Office> officeList = Office.find("Select o from Office o where o.code = ?", getInteger(code)).fetch();
		for(Office off : officeList) {
			
			if( !off.id.equals(office.id) ) {
				
				flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
				Offices.showOffices();
			}
		}
		
		office.name = name;
		office.address = address;
		office.code = getInteger(code);
		office.joiningDate = getLocalDate(date);
		
		office.save();
		
		flash.success("Sede correttamente modificata");
		Offices.showOffices();
	}
	
	
	
	private static boolean isNullOrEmpty(String parameter)
	{
		if( (parameter==null || parameter.equals("") ))
			return true;
		return false;
	}
	
	private static Integer getInteger(String parameter)
	{
		try{
			Integer i = Integer.parseInt(parameter);
			return i;

		}catch(Exception e)
		{
			return null;
		}
	}
	
	private static LocalDate getLocalDate(String parameter)
	{
		try{
			LocalDate date = new LocalDate(parameter);
			return date;

		}catch(Exception e)
		{
			return null;
		}
	}

}
