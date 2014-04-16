package controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import play.cache.Cache;
import play.data.validation.*;
import play.mvc.Controller;




/**
 * @author daniele
 *
 */
public class Wizard extends Controller {
	
	public static final String STEPS_KEY = "steps";
	public static final String PROPERTIES_KEY = "properties";
	
	public static class WizardStep {
		public final int index;
		public final String name;
		public final String template;
		public boolean completed = false;
		
		WizardStep(String name, String template,int index) {
			this.name = name;
			this.template = template;
			this.index = index;
		}
		
		public void complete() {
			completed = true;
		}
		
		public static WizardStep of(String name, String template,int index) {
			return new WizardStep(name, template,index);
		}
	}
	
	public static List<WizardStep> createSteps() {
		return ImmutableList
				.of(WizardStep.of("Nuova installazione", "firstChoice",0),
					WizardStep.of("Nuovo admin", "setAdmin",1),
					WizardStep.of("Nuovo ufficio", "setOffice",2),
					WizardStep.of("Configurazione Giorno Patrono", "setPatron",3),
					WizardStep.of("Parametri di configurazione generale", "setGenConf",4),
					WizardStep.of("Configurazione annuale", "setConfYear", 5));
	}
	
    public static void wizard(@Range(min = 0, max = 5)int step) {
    	
    	if(validation.hasError("step")){
    		step = 0;
    	}  
    
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	
    	if (steps == null) {
    		steps = createSteps();
    		try{
    			properties = new Properties();
    			properties.load(new FileInputStream("conf/properties.conf"));	
    		}
    		catch(IOException f){
    			flash.error(f.getMessage());  		
    		}
    	
    		Cache.safeAdd(STEPS_KEY, steps,"10mn");
    		Cache.safeAdd(PROPERTIES_KEY, properties,"10mn");
       	}
		
    	int stepsCompleted = Collections2.filter(steps, 
    		new Predicate<WizardStep>() {
    	    @Override
    	    public boolean apply(WizardStep p) {
    	        return p.completed;
    	    }
    	}).size();

    	double percent = stepsCompleted*(100/steps.size());
    	
    	if(step != 0){
    		if(!steps.get(step-1).completed){
    			step = stepsCompleted;
        	}
    	}
    	
    	WizardStep currentStep = steps.get(step);
    	
    	if(properties != null){
    	try{
    		properties.store(new FileOutputStream("conf/properties.conf"), "File impostazioni wizard");
    	}
    	catch(IOException e){
    		flash.error(e.getMessage());    		
    	}
    	}

    	render("@" + currentStep.template, steps,currentStep, percent, properties);
    }

    /**
     * STEP 1 "Scelta import o manuale"
     */
    public static void firstChoice() {
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	
    	if(steps != null){
        	steps.get(0).complete();
        	Cache.replace(STEPS_KEY, steps);

    	}
    	wizard(1);
    
    }
    
    /**
     * STEP 2 "Creazione Amministratore"
     */
    public static void setAdmin(
    		@Required String user,
    		@Required String password,
    		@Required @Equals("password") String passwordRetype){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(1);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("user",user);
    		properties.setProperty("password",password);
    		properties.setProperty("password_retype",passwordRetype);

    		steps.get(1).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(2);
    }
    
    /**
     * STEP 3 "Inserimento nome istituto e codice sede"
     */
    public static void setOffice(
    		@Required String istituto,
    		@Required String sede ){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(2);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("Istituto",istituto);
    		properties.setProperty("Sede",sede);

    		steps.get(2).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(3);
    }
    
    /**
     * STEP 4 Inserimento Giorno Patrono"
     */
    public static void setPatron(@Required String mese_patrono,@Required String giorno_patrono){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(3);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("Mese Patrono",mese_patrono);
    		properties.setProperty("Giorno Patrono",giorno_patrono);

    		steps.get(3).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(4);
    }
    
    /**
     * STEP 5
     */
    public static void setGenConf(@Required String initUseStart,
    		@Required String lunchPauseStart, 
    		@Required String lunchPauseStop,
    		@Required boolean webStampingAllowed
    		){
    	
    	if (validation.hasErrors()){
//    		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
//    		LocalDate data = new LocalDate(LocalDate.parse(initUseStart,dtf));
//        	flash.error("data inserita %s", data);
    	    params.flash(); 
    	    validation.keep();
    		wizard(4);
        	
    	}
    	    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("Data di inizio utilizzo",initUseStart);
    		properties.setProperty("Inzio pausa pranzo",lunchPauseStart);
    		properties.setProperty("Fine pausa pranzo",lunchPauseStop);
    		String ok = "NO";
    		if(webStampingAllowed){ok ="SI";}
    		properties.setProperty("Permetti timbrature web",ok);

    		steps.get(4).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(5);
    }
    
    public static void setConfYear(@Required String dayVacantionExp,
    		@Required String monthVacantionExp,
    		@Required String monthExpireRecoveryDaysOneThree,
    		@Required String maxRecoveryDaysOneThree,
    		@Required String monthExpireRecoveryDaysFourNine,
    		@Required String maxRecoveryDaysFourNine,
    		@Required String hourMaxToCalculateWorkTime
    		){
    	
    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(5);  	
    	}
    	    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("Giorno scadenza Vacanze anno passato",dayVacantionExp);
    		properties.setProperty("Mese scadenza Vacanze anno passato",monthVacantionExp);
    		properties.setProperty("Mese scadenza giorni di recupero 13",monthExpireRecoveryDaysOneThree);
    		properties.setProperty("Numero massimo giorni di recupero 13",maxRecoveryDaysOneThree);
    		properties.setProperty("Mese scadenza giorni di recupero 49",monthExpireRecoveryDaysFourNine);
    		properties.setProperty("Numero massimo giorni di recupero 49",maxRecoveryDaysFourNine);
    		properties.setProperty("Soglia oraria massima per il calcolo delle timbrature giornaliere",hourMaxToCalculateWorkTime);    		
 
    		steps.get(5).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	renderText("wizard ok");
//       	wizard(6);
    }
    
}
