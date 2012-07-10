package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.WebStampingAddress;
import play.mvc.Controller;

public class ConfParameters extends Controller{
	
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.confParameters;
	/**
	 * TODO: implementare il controller per la validazione e l'utilizzo dei parametri di configurazione
	 */
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void show(){
		String menuItem = actionMenuItem.toString();
		
		ConfParameters confParameters = new ConfParameters();
		WebStampingAddress webStampingAddress = new WebStampingAddress();
		render(confParameters,webStampingAddress, menuItem);
	}
	public static void save(){
		
	}
	
	public static void discard(){
		
	}
}
