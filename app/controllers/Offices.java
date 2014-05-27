package controllers;

import java.util.List;

import models.Office;
import models.RemoteOffice;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class})
public class Offices extends Controller {

	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void showOffices(){
		
		//TODOOFF1 Prendere quelli di cui la persona loggata ha diritti
		List<Office> instituteList = Office.find("Select o from Office o where o.office is null").fetch();	//prendo gli istituti
		render(instituteList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void insertInstitute() {
		
		render();
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void saveInstitute(String name, String contraction) {
		
		if( name == null || name.equals("") || contraction == null || contraction.equals("") ) {
			
			flash.error("Valorizzare correttamente entrambi i campi, operazione annullata.");
			Offices.showOffices();
		}
		
		Office office = Office.find("byName",name).first();
		if( office != null ) {
			
			flash.error("Esiste gia' un istituto con nome %s, operazione annullata.", name);
			Offices.showOffices();
		}
		
		office = Office.find("byContraction",name).first();
		if( office != null ) {
			
			flash.error("Esiste gia' un istituto con sigla %s, operazione annullata.", contraction);
			Offices.showOffices();
		}

		office = new Office();
		office.name = name;
		office.contraction = contraction;
		office.save();
		
		flash.success("Istituto %s con sigla %s correttamente inserito", name, contraction);
		Offices.showOffices();
	}
			
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void insertSeat(Long officeId) {
		Office office = Office.findById(officeId);
		if(office==null) {
			
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}
		render(office);
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void saveSeat(Long officeId, String name, String address, String code, String date) {
	
		Office office = Office.findById(officeId);
		if(office==null) {
			
			flash.error("L'instituto selezionato non esiste. Operazione annullata.");
			Offices.showOffices();
		}

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
		Office alreadyExist = Office.find("Select o from Office o where o.code = ?", getInteger(code)).first();
		if(alreadyExist!=null){
			flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
			Offices.showOffices();
		}
		
		RemoteOffice remoteOffice = new RemoteOffice();
		remoteOffice.name = name;
		remoteOffice.address = address;
		remoteOffice.code = getInteger(code);
		remoteOffice.joiningDate = getLocalDate(date);
		remoteOffice.office = office;

		remoteOffice.save();
		
		flash.success("Sede correttamente inserita");
		Offices.showOffices();
	}

	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void editSeat(Long officeId){
		
		RemoteOffice remoteOffice = RemoteOffice.findById(officeId);
		
		if(remoteOffice==null) {
			
			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}
		
		render(remoteOffice);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void updateSeat(Long officeId, String name, String address, String code, String date) {
	
		RemoteOffice remoteOffice = RemoteOffice.findById(officeId);
		if(remoteOffice==null) {
			
			flash.error("La sede selezionata non esiste. Operazione annullata.");
			Offices.showOffices();
		}

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
		List<Office> officeList = Office.find("Select o from Office o where o.code = ?", getInteger(code)).fetch();
		for(Office office : officeList) {
			
			if( !office.id.equals(remoteOffice.id) ) {
				
				flash.error("Il codice sede risulta gia' presente. Valorizzare correttamente tutti i parametri.");
				Offices.showOffices();
			}
		}
		
		remoteOffice.name = name;
		remoteOffice.address = address;
		remoteOffice.code = getInteger(code);
		remoteOffice.joiningDate = getLocalDate(date);
		
		remoteOffice.save();
		
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
