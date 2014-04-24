package controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.Permission;
import models.User;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import play.Logger;
import play.cache.Cache;
import play.data.validation.*;
import play.libs.Codec;
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
					WizardStep.of("Configurazione generale", "setGenConf",4),
					WizardStep.of("Configurazione annuale", "setConfYear", 5),
					WizardStep.of("Riepilogo", "summary",6));
	}
	
    public static void wizard(int step) {
    	
		Logger.info(ConfigurationFields.getConfGeneralFields().toString());
		Logger.info(ConfigurationFields.getConfYearFields().toString());
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	double percent = 0;
    	
    	if (steps == null) {
    		steps = createSteps();
    		try{
    			properties = new Properties();
    			properties.load(new FileInputStream("conf/properties.conf"));	
    		}
    		catch(IOException f){
    		}
    	
    		Cache.safeAdd(STEPS_KEY, steps,"10mn");
    		Cache.safeAdd(PROPERTIES_KEY, properties,"10mn");
       	}
    	
    	if(step>0 && step<=steps.size()){
    		
    		int stepsCompleted = Collections2.filter(steps, 
    	    		new Predicate<WizardStep>() {
    	    	    @Override
    	    	    public boolean apply(WizardStep p) {
    	    	        return p.completed;
    	    	    }
    	    	}).size();
    		
    		percent = stepsCompleted*(100/steps.size());
    		
    		if(!steps.get(step-1).completed){
    			step = stepsCompleted;
        	}
    	}
    	
    	else{
    		step=0;
    	}
    	
    	WizardStep currentStep = steps.get(step);
    	
    	if(properties != null && step == 6){
    	try{
    		properties.store(new FileOutputStream("conf/properties.conf"), "File impostazioni wizard");
    		Logger.info("Creato file properties.conf");
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
    		
    		properties.setProperty("Nome Amministratore",user);
    		properties.setProperty("Password Amministratore",password);

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
    		@Required String institute,
    		@Required String code ){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(2);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("institute_name",institute);
    		properties.setProperty("seat_code",code);

    		steps.get(2).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(3);
    }
    
    /**
     * STEP 4 Inserimento Giorno Patrono"
     */
    public static void setPatron(@Required String patronMonth,@Required String patronDay){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(3);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("month_of_patron",patronMonth);
    		properties.setProperty("day_of_patron",patronDay);

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
    	    params.flash(); 
    	    validation.keep();
    		wizard(4);
    	}
    	    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("init_use_program",initUseStart);
    		
			List<String> pauseStart = Splitter.on(":").trimResults().splitToList(lunchPauseStart);
			List<String> pauseStop = Splitter.on(":").trimResults().splitToList(lunchPauseStop);
			
    		properties.setProperty("meal_time_start_hour",pauseStart.get(0));
    		properties.setProperty("meal_time_start_minute",pauseStart.get(1));
    		properties.setProperty("meal_time_end_hour",pauseStop.get(0));
    		properties.setProperty("meal_time_end_minute",pauseStop.get(1));
    		String ok = "NO";
    		if(webStampingAllowed){ok ="SI";}
    		properties.setProperty("web_stamping_allowed",ok);

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
    		
    		properties.setProperty("day_expiry_vacation_past_year",dayVacantionExp);
    		properties.setProperty("month_expiry_vacation_past_year",monthVacantionExp);
    		properties.setProperty("month_expire_recovery_days_13",monthExpireRecoveryDaysOneThree);
    		properties.setProperty("max_recovery_days_13",maxRecoveryDaysOneThree);
    		properties.setProperty("month_expire_recovery_days_49",monthExpireRecoveryDaysFourNine);
    		properties.setProperty("max_recovery_days_49",maxRecoveryDaysFourNine);
    		
    		List<String> hmtcwk = Splitter.on(":").trimResults().splitToList(hourMaxToCalculateWorkTime);
    		properties.setProperty("hour_max_to_calculate_worktime",hmtcwk.get(0)); 
    		    		
    		steps.get(5).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    		
    	}
       	wizard(6);
    }
    
    
    public static void submit(){
		Properties properties = new Properties();
		try{
			properties.load(new FileInputStream("conf/properties.conf"));	
		}
		catch(IOException f){
			Logger.info("Creato il file properties.conf per la procedura di Wizard");	
		}
		// setAdmin
		User admin = new User();
		admin.username = properties.getProperty("Nome Amministratore");
		admin.password = Codec.hexMD5(properties.getProperty("Password Amministratore"));
		admin.permissions = Permission.findAll();
		admin.save();
	
		// setOffice
		Office office = Office.findById(new Long(1));
		if(office == null){
			office = new Office();
		}
		office.name = properties.getProperty("institute_name");
		office.code = Integer.parseInt(properties.getProperty("seat_code"));
		office.save();
		
		List<ConfGeneral> confGeneral = ConfGeneral.find(
				"Select cg from ConfGeneral cg where cg.office = ?", office).fetch();
		
		
		for (ConfGeneral cf : confGeneral){
			cf.fieldValue = properties.getProperty(cf.field);
			cf.save();
		}
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
		
		List<ConfYear> confYear = ConfYear.find(
				"Select cy from ConfYear cy where cy.office = ? and cy.year = ?", 
				office,LocalDate.parse
				(properties.getProperty("init_use_program"),dtf).getYear()).fetch();
				
		for (ConfYear cy : confYear){
			cy.fieldValue = Integer.parseInt(properties.getProperty(cy.field));
			cy.save();
		}
				
		// setPatron
//		ConfGeneral confGeneral = new ConfGeneral();
//		confGeneral.instituteName = office.name;
//		confGeneral.seatCode = office.code;
//		confGeneral.urlToPresence = "https://attestati.rm.cnr.it/attestati/";
//		confGeneral.monthOfPatron = Integer.parseInt(properties.getProperty("Mese Patrono"));
//		confGeneral.dayOfPatron = Integer.parseInt(properties.getProperty("Giorno Patrono"));
		//setGenConf
		
		
//		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
//		confGeneral.initUseProgram = LocalDate.parse
//				(properties.getProperty("Data di inizio utilizzo"),dtf);
//		
//		List<String> launchStartTime = 
//				Splitter.on(":").trimResults().splitToList
//				(properties.getProperty("Inzio pausa pranzo"));
//		List<String> launchEndTime = 
//				Splitter.on(":").trimResults().splitToList
//				(properties.getProperty("Fine pausa pranzo"));
//		
//		confGeneral.mealTimeStartHour = Integer.parseInt(launchStartTime.get(0));
//		confGeneral.mealTimeStartMinute = Integer.parseInt(launchStartTime.get(1));
//		
//		confGeneral.mealTimeEndHour = Integer.parseInt(launchEndTime.get(0)); 
//		confGeneral.mealTimeEndMinute = Integer.parseInt(launchEndTime.get(1));
//		
//		if (properties.getProperty("Permetti timbrature web").equalsIgnoreCase("SI")){
//			confGeneral.webStampingAllowed = true;
//		}
//		else{
//			confGeneral.webStampingAllowed = false;
//		}
//		
//		confGeneral.save();
//		
//		// setConfYear
//		LocalDate date = new LocalDate();
//		 
//		ConfYear confYear = new ConfYear();
//		confYear.year = date.getYear();
//		
//		confYear.dayExpiryVacationPastYear = Integer.parseInt(
//				properties.getProperty("Giorno scadenza Vacanze anno passato"));
//		confYear.monthExpiryVacationPastYear = Integer.parseInt(properties.getProperty(
//				"Mese scadenza Vacanze anno passato"));
//		confYear.monthExpireRecoveryDaysOneThree = Integer.parseInt(properties.getProperty(
//				"Mese scadenza giorni di recupero 1-3"));
//		confYear.maxRecoveryDaysOneThree = Integer.parseInt(properties.getProperty(
//				"Numero massimo giorni di recupero 1-3"));
//		confYear.monthExpireRecoveryDaysFourNine = Integer.parseInt(properties.getProperty(
//				"Mese scadenza giorni di recupero 4-9"));
//		confYear.maxRecoveryDaysFourNine = Integer.parseInt(properties.getProperty(
//				"Numero massimo giorni di recupero 4-9"));
//		confYear.hourMaxToCalculateWorkTime = Integer.parseInt(Splitter.on(":").trimResults().splitToList
//				(properties.getProperty("Soglia oraria calcolo timbrature giornaliere")).get(0));
//		
//		confYear.save();
//				
//		
//		ConfYear confPreviousYear = new ConfYear();
//				
//		confPreviousYear.year = confYear.year - 1;
//		confPreviousYear.dayExpiryVacationPastYear = confYear.dayExpiryVacationPastYear;
//		confPreviousYear.monthExpiryVacationPastYear = confYear.monthExpiryVacationPastYear;
//		confPreviousYear.monthExpireRecoveryDaysOneThree = confYear.monthExpireRecoveryDaysOneThree;
//		confPreviousYear.maxRecoveryDaysOneThree = confYear.maxRecoveryDaysOneThree;
//		confPreviousYear.monthExpireRecoveryDaysFourNine = confYear.monthExpireRecoveryDaysFourNine;
//		confPreviousYear.maxRecoveryDaysFourNine = confYear.maxRecoveryDaysFourNine;
//		confPreviousYear.hourMaxToCalculateWorkTime = confYear.hourMaxToCalculateWorkTime;
//		
//		confPreviousYear.save();

		Application.index();
    }
            
}
