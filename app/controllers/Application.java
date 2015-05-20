package controllers;

import java.util.List;

import javax.inject.Inject;

import models.Office;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import dao.OfficeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

@With( {Secure.class, RequestInit.class} )
public class Application extends Controller {
    
	@Inject
	static OfficeDao officeDao;
	@Inject
	static IWrapperFactory wrapperFactory;
	
    public static void indexAdmin() {
		Logger.debug("chiamato metodo indexAdmin dell'Application controller");
		render();
       
    }
    
    public static void index() {
    	    	
    	List<Office> officeList = Office.findAll();
    	boolean seatExist = false;
    	for(Office office : officeList) {
    		
    		IWrapperOffice wOffice = wrapperFactory.create(office);
    		
    		if(wOffice.isSeat()) {
    			seatExist = true;
    			break;
    		}
    	}
    	
    	if(!seatExist) {
    		
    		Offices.showOffices();
    	}
    	
    	if( Security.getUser().get().username.equals("epas.clocks") ){
    		
    		Clocks.show();
    		return;
    	}
    	
    	if( Security.getUser().get().username.equals("admin") ){
    		
    		Persons.list(null);
    		return;
    	}
    	
    	//inizializzazione functional menu dopo login
    	session.put("monthSelected", new LocalDate().getMonthOfYear());
    	session.put("yearSelected", new LocalDate().getYear());
    	session.put("personSelected", Security.getUser().get().person.id);
    	
    	session.put("methodSelected", "stampingsAdmin");
		session.put("actionSelected", "Stampings.stampings");
    	Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
		

    }
    
    
    
}

