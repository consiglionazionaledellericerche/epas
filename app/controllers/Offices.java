package controllers;

import java.util.List;

import org.joda.time.LocalDate;

import models.Office;
import models.Person;
import models.RemoteOffice;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class})
public class Offices extends Controller {

	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void showOffices(){
		Office office = Office.find("Select o from Office o where o.office is null").first();	//prendo la sede suprema
		
		List<RemoteOffice> remoteOffices = RemoteOffice.findAll();
		
				
		render(office, remoteOffices);
		
		
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void editOffice(Long officeId){
		Office office = Office.findById(officeId);
		render(office);
		
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
		remoteOffice.office = Office.find("Select o from Office o where o.joiningDate is null").first();

		remoteOffice.save();
		flash.success("Sede distaccatta correttamente inserita.");
		Offices.showOffices();
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void update(){
		String name = params.get("name");
		String address = params.get("address");
		
		Integer code = params.get("code", Integer.class);
		if(code == null){
			flash.error("Il campo codice deve essere valorizzato SOLO con valori interi");
			Offices.showOffices();
		}
		Office office = Office.findById(params.get("officeId", Long.class));
		office.name = name;
		office.address = address;
		office.code = code;
		office.save();
		flash.success("Sede modificata. Nuovo nome: %s. Nuovo codice: %d", name, code);
		Offices.showOffices();
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void disableRemoteOffice(Long remoteOfficeId){
	
		RemoteOffice remote = RemoteOffice.findById(remoteOfficeId);
		List<Office> offices = Office.find("Select o from Office o where o.joiningDate is null").fetch();
		render(remote, offices);
	}
	
	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void disable(){
		
		String remoteOfficeId = params.get("remoteOfficeId");
		String office = params.get("office");
		Long id = new Long(remoteOfficeId);
		RemoteOffice remote = RemoteOffice.findById(id);

		List<Person> personRemoteOfficeAddicted = Person.find("Select p from Person p where p.office = ?", remote).fetch();
		Office o = Office.find("Select o from Office o where o.name = ?", office).first();
		for(Person p : personRemoteOfficeAddicted){
			p.office = o;
			p.save();
		}
		remote.office = null;
		//remote.joiningDate = null;
		remote.save();
		flash.success("Sede distaccata disabilitata con successo. E persone associate alla sede %s", o.name);
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
