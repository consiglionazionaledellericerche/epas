package controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.User;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import controllers.Resecure.NoCheck;
import play.Logger;
import play.cache.Cache;
import play.data.validation.*;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import validators.StringIsTime;

/**
 * @author daniele
 *
 */
@With( {Resecure.class, RequestInit.class})
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
				.of(WizardStep.of("Cambio password Admin", "changeAdminPsw",0),
					WizardStep.of("Nuovo ufficio", "setOffice",1),
					WizardStep.of("Configurazione generale", "setGenConf",2),
					WizardStep.of("Creazione Ruolo per l'amministrazione", "administrationRole", 3),
					WizardStep.of("Riepilogo", "summary",4));
	}
	
	@NoCheck
	public static void wizard(int step) {
		Preconditions.checkNotNull(step);
    	    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	double percent = 0;
    	
    	if (steps == null) {
    		steps = createSteps();
    		Cache.safeAdd(STEPS_KEY, steps,"10mn");
       	}
    	
    	if(properties == null){
    		try{
    			properties = new Properties();
    			properties.load(new FileInputStream("conf/properties.conf"));	
    		}
    		catch(IOException f){
    			Logger.error("Impossibile caricare il file properties.conf per la procedura di Wizard");
    		}
    		Cache.safeAdd(PROPERTIES_KEY, properties,"10mn");
    	}
    	
    	int stepsCompleted = Collections2.filter(steps, 
	    		new Predicate<WizardStep>() {
	    	    @Override
	    	    public boolean apply(WizardStep p) {
	    	        return p.completed;
	    	    }
	    	}).size();
    	
    	percent = stepsCompleted*(100/steps.size());
    	
    	if (step < 0 | step > steps.size()){
    		step = stepsCompleted;
    	}
    		
    	else if(step != 0 && !steps.get(step-1).completed){
    			step = stepsCompleted;
        	}
    	
    	WizardStep currentStep = steps.get(step);

    	if(properties != null && step > 0){
    	try{
    		properties.store(new FileOutputStream("conf/properties.conf"), "Wizard values file");
    		Logger.info("Salvato file properties.conf");
    	}
    	catch(IOException e){
    		flash.error(e.getMessage());    		
    	}
    	}

    	render("@" + currentStep.template, steps,currentStep, percent, properties);
    }

    /**
     * STEP 1 "Cambio password admin"
     */
	@NoCheck
    public static void changeAdminPsw(
    		int stepIndex,
    		@Required String admin_password,
    		@Required @Equals("admin_password") String admin_password_retype){
    	
    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(stepIndex);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		properties.setProperty("admin_password",admin_password);
    		
    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
            	Logger.info("Completato lo step %s del wizard", stepIndex);
    		}
    		
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(stepIndex+1);
    }
    
    
    /**
     * STEP 2 "Creazione Area,Istituto e Sede"
     */
	
	@NoCheck
    public static void setOffice(
    	    int stepIndex,
    		@Required String area,
    		@Required String institute,
    		@Required String institute_contraction,
    		@Required String seat,
    		@Required String seat_address,
    		@Required String seat_code,
    		@Required String seat_affiliation_date ){

    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(stepIndex);
    	}
    	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){
    		
    		properties.setProperty("area",area);
    		properties.setProperty("institute",institute);
    		properties.setProperty("institute_contraction",institute_contraction);
    		properties.setProperty("seat",seat);
    		properties.setProperty("seat_address",seat_address);
    		properties.setProperty("seat_code",seat_code);
    		properties.setProperty("seat_affiliation_date",seat_affiliation_date);

    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
    		}
    		
    		steps.get(stepIndex).complete();
    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    		
    	}

       	wizard(stepIndex+1);
    }
      
    /**
     * STEP 3 Impostazioni Generali relativi alla Sede creata
     */
	
	@NoCheck
    public static void setGenConf(
    		int stepIndex,
    		@Required String date_of_patron,  		
    		@Required @CheckWith (StringIsTime.class) String lunch_pause_start, 
    		@Required @CheckWith (StringIsTime.class) String lunch_pause_end,
    		@Email String email_to_contact
    		){
		
    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(stepIndex);
    	}
    		
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){

    		properties.setProperty("date_of_patron",date_of_patron);
    		properties.setProperty("lunch_pause_start",lunch_pause_start);
    		properties.setProperty("lunch_pause_end",lunch_pause_end);
    		properties.setProperty("email_to_contact",email_to_contact);

    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
            	Logger.info("Completato lo step %s del wizard", stepIndex);
    		}

    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(stepIndex+1);
    }
	
	
    /**
     * STEP 4 Creazione Profilo per l'amministratore
     */
	
	@NoCheck
    public static void administrationRole(
    		int stepIndex,
    		@Required String admin_surname,
    		@Required String admin_name,
    		@Required String admin_qualification,
    		String admin_badge_number,
    		@Required String admin_registration_number,
    		String admin_birthday,
    		@Email String admin_email,
    		@Required String admin_contract_begin,
    		String admin_contract_end
    		){
		
    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(stepIndex);
    	}
    		
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){

    		properties.setProperty("admin_surname",admin_surname);
    		properties.setProperty("admin_name",admin_name);
    		properties.setProperty("admin_qualification",admin_qualification);
    		properties.setProperty("admin_badge_number",admin_badge_number);
    		properties.setProperty("admin_registration_number",admin_registration_number);
    		properties.setProperty("admin_birthday",admin_birthday);
    		properties.setProperty("admin_email",admin_email);
    		properties.setProperty("admin_contract_begin",admin_contract_begin);
    		properties.setProperty("admin_contract_end",admin_contract_end);

    		
    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
            	Logger.info("Completato lo step %s del wizard", stepIndex);
    		}

    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(stepIndex+1);
    }
	
    
	@NoCheck
    public static void submit(){
		Properties properties = new Properties();
		try{
			properties.load(new FileInputStream("conf/properties.conf"));	
		}
		catch(IOException f){
			Logger.error("Impossibile caricare il file properties.conf per la procedura di Wizard");	
		}
		//Admin password set
		User admin = User.find("byUsername", "admin").first();
		admin.password = Codec.hexMD5(properties.getProperty("Nuova Password Admin"));
		admin.save();
		
		
		
//		List<String> pauseStart = Splitter.on(":").trimResults().splitToList(lunch_pause_start);
//		List<String> pauseStop = Splitter.on(":").trimResults().splitToList(lunch_pause_end);
//		
//		properties.setProperty("meal_time_start_hour",pauseStart.get(0));
//		properties.setProperty("meal_time_start_minute",pauseStart.get(1));
//		properties.setProperty("meal_time_end_hour",pauseStop.get(0));
//		properties.setProperty("meal_time_end_minute",pauseStop.get(1));
		
		
		// setOffice
		Office office = Office.findById(1L); 
		if(office == null){
			office = new Office();
		}
		
		office.name= properties.getProperty("institute_name");
		office.code = Integer.parseInt(properties.getProperty("seat_code"));
		office.save();
		
		// setAdmin
		User newAdmin = new User();
		newAdmin.username = properties.getProperty("Nome Amministratore");
		newAdmin.password = Codec.hexMD5(properties.getProperty("Password Amministratore"));
		newAdmin.save();
		
		List<String> descPermissions = new ArrayList<String>();
		descPermissions.add("insertAndUpdateOffices");
		descPermissions.add("viewPersonList");
		descPermissions.add("deletePerson");
//		descPermissions.add("insertAndUpdateStamping");
		descPermissions.add("insertAndUpdatePerson");
//		descPermissions.add("insertAndUpdateWorkingTime");
//		descPermissions.add("insertAndUpdateAbsence");
		descPermissions.add("insertAndUpdateConfiguration");
		descPermissions.add("insertAndUpdatePassword");
		descPermissions.add("insertAndUpdateAdministrator");
//		descPermissions.add("insertAndUpdateCompetences");
//		descPermissions.add("insertAndUpdateVacations");
//		descPermissions.add("viewPersonalSituation");
//		descPermissions.add("uploadSituation");
		
		
		/* TODO rifattorizzare con i nuovi permessi 
		List<Permission> permissions = Permission.find("description in (?1)", descPermissions).fetch();
		
		List<UsersPermissionsOffices> usersPermissionOffices = new ArrayList<UsersPermissionsOffices>(); 
		
		for (Permission p: permissions){
			UsersPermissionsOffices upo = new UsersPermissionsOffices();
			upo.office = office;
			upo.user = newAdmin;
			upo.permission = p;
			upo.save();
			usersPermissionOffices.add(upo);
		}
		
		newAdmin.userPermissionOffices = usersPermissionOffices;
		
		*/
		newAdmin.save();
		
		// setGenConf
		List<String> cgf = ConfigurationFields.getConfGeneralFields();
		
		List<ConfGeneral> confGeneral = ConfGeneral.find("office_id", office).fetch();
		
		for(String field : cgf){
			boolean fieldIsPresent = false;
			for(ConfGeneral cg : confGeneral){
				if(cg.field.equals(field)){
					fieldIsPresent = true;
					cg.fieldValue = properties.getProperty(field);
					cg.save();
					break;
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
				
		// setConfYear
		List<String> cyf = ConfigurationFields.getConfYearFields();
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
		Integer startYear = LocalDate.parse
				(properties.getProperty("init_use_program"),dtf).getYear();
		
		List<ConfYear> confYear = ConfYear.find(
				"Select cy from ConfYear cy where cy.office = ? and cy.year = ? or cy.year = ? order by cy.field asc", 
				office,startYear,startYear - 1).fetch();
		
		for(String field : cyf){
			boolean thisYearPresent = false;
			boolean prevYearPresent = false;
			for(ConfYear cy : confYear){
				if(cy.field.equals(field)){
					
					if(cy.year.compareTo(startYear) == 0 ){	
						thisYearPresent = true;
					}
					else{ 
						prevYearPresent = true;
						}
					
					cy.fieldValue = properties.getProperty(field);					
					cy.save();
					
				if (thisYearPresent && prevYearPresent){
					break;	
					} 
				}
			}
			
			if(!thisYearPresent){
				ConfYear ncy = new ConfYear();
				ncy.year = startYear;
				ncy.office = office;
				ncy.field = field;
				ncy.fieldValue = properties.getProperty(field);
				ncy.save();
			}
			if(!prevYearPresent){
				ConfYear ncy = new ConfYear();
				ncy.year = startYear - 1;
				ncy.office = office;
				ncy.field = field;
				ncy.fieldValue = properties.getProperty(field);
				ncy.save();
			}
			
		}

		Application.index();
    }
            
}
