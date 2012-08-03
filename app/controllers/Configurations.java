package controllers;

import java.util.Date;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.WebStampingAddress;
import play.mvc.Controller;
import models.Configuration;

public class Configurations extends Controller{
	
	
	/**
	 * TODO: implementare il controller per la validazione e l'utilizzo dei parametri di configurazione
	 */
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void show(){
		Date now = new Date();
		List<Configuration> configurations = Configuration.find("WHERE from >= ? and to <= ?", now, now).fetch();
		if(configurations.size() > 1) {
			throw new IllegalStateException("Dovrebbe esserci una sola configurazione valida per ogni periodo di tempo");
		}
		if (configurations.size() == 0) {
			throw new IllegalStateException("Nessuna configurazione valida presente nel db, contattare Dario!");
		}
		
		render(configurations.get(0));
	}
	
	public static void save(Long id, Configuration configuration){
		
	}
	
	
	public static void discard(){
		
	}
}
