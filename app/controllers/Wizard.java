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
    public static void setPatron(
    		@Required String patronMonth,
    		@Required String patronDay){

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
    public static void setGenConf(
    		@Required String initUseStart,
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
    
    public static void setConfYear(
    		@Required String dayVacantionExp,
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
		
		
		List<String> cgf = ConfigurationFields.getConfGeneralFields();
		
		List<ConfGeneral> confGeneral = ConfGeneral.find(
				"Select cg from ConfGeneral cg where cg.office = ?", office).fetch();
		
		for(String field : cgf){
			boolean fieldIsPresent = false;
			for(ConfGeneral cg : confGeneral){
				if(cg.field.equals(field)){
					fieldIsPresent = true;
					cg.fieldValue = properties.getProperty(field);
					cg.save();
				}
			}
			if(!fieldIsPresent){
				ConfGeneral ncg = new ConfGeneral();
				ncg.office = office;
				ncg.field = field;
				ncg.fieldValue = properties.getProperty(field);
				ncg.save();
				
			}
		}	
		
		
		List<String> cyf = ConfigurationFields.getConfYearFields();
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
		Integer startYear = LocalDate.parse
				(properties.getProperty("init_use_program"),dtf).getYear();
		
		List<ConfYear> confYear = ConfYear.find(
				"Select cy from ConfYear cy where cy.office = ? and cy.year = ?", 
				office,startYear).fetch();
		
		for(String field : cyf){
			boolean fieldIsPresent = false;
			for(ConfYear cy : confYear){
				if((cy.field.equals(field)) && (cy.year == startYear)){
					fieldIsPresent = true;
					cy.fieldValue = properties.getProperty(field);
					
					ConfYear ncpy = new ConfYear();
					ncpy.year = cy.year -1;
					ncpy.office = cy.office;
					ncpy.field = cy.field;
					ncpy.fieldValue = cy.fieldValue;
					
					cy.save();
					ncpy.save();
				}
			
			}
			
			if(!fieldIsPresent){
				ConfYear ncy = new ConfYear();
				ncy.year = startYear;
				ncy.office = office;
				ncy.field = field;
				ncy.fieldValue = properties.getProperty(field);
				
				ConfYear ncpy = new ConfYear();
				ncpy.year = ncy.year -1;
				ncpy.office = ncy.office;
				ncpy.field = ncy.field;
				ncpy.fieldValue = ncy.fieldValue;
				
				ncy.save();
				ncpy.save();
			}
		}

		Application.index();
    }
            
}
