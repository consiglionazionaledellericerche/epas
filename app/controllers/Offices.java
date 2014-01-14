package controllers;

import java.util.List;

import org.joda.time.LocalDate;

import models.Office;
import models.RemoteOffice;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class})
public class Offices extends Controller {

	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void showOffices(){
		Office office = Office.find("Select o from Office o where o.office is null").first();	//prendo la sede suprema
		
		List<RemoteOffice> remoteOffices = RemoteOffice.findAll();
		
				
		render(office, remoteOffices);
		
		
		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void insertRemoteOffice()
	{
		Office office = Office.find("Select o from Office o where o.office is null").first();	//prendo la sede suprema
		render(office);
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void save()
	{
		String name = params.get("name");
		String address = params.get("address");
		String code = params.get("code");
		String date = params.get("date");
		
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
		
		//Creazione Nuovo RemoteOffice
		
		RemoteOffice remoteOffice = new RemoteOffice();
		remoteOffice.name = name;
		remoteOffice.address = address;
		remoteOffice.code = getInteger(code);
		remoteOffice.joiningDate = getLocalDate(date);
		remoteOffice.office = Office.find("Select o from Office o where o.office is null").first();

		remoteOffice.save();
		flash.success("Sede distaccatta correttamente inserita.");
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
