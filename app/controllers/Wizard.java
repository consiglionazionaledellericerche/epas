package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import models.ConfGeneral;
import models.ConfYear;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import controllers.Resecure.NoCheck;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.data.validation.*;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import validators.StringIsTime;

/**
 * @author daniele
 *
 */
@With( {Resecure.class})
public class Wizard extends Controller {
	
	public static final String STEPS_KEY = "steps";
	public static final String PROPERTIES_KEY = "properties";
	public static final String OFFICE_COUNT = "officeCount";
	
		
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
					WizardStep.of("Creazione Ruolo per l'amministrazione", "seatManagerRole", 3),
					WizardStep.of("Riepilogo", "summary",4));
	}
	

	public static void wizard(int step) {
		Preconditions.checkNotNull(step);
		
		
//    	Recupero dalla cache  	
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	Long officeCount = Cache.get(OFFICE_COUNT,Long.class);
    	
    	if(officeCount == null){
    		officeCount = Office.count();
      		Cache.add(OFFICE_COUNT, officeCount);
    	}
    	
    	if(officeCount > 0){
			flash.error("Impossibile accedere alla procedura di Wizard se è già presente un Ufficio nel sistema");
			Offices.showOffices();
    	}
    	
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
    	
    	if(properties != null && step > 0){
        	try{
        		properties.store(new FileOutputStream("conf/properties.conf"), "Wizard values file");
        		Logger.info("Salvato file properties.conf");
        	}
        	catch(IOException e){
        		flash.error(e.getMessage());    		
        	}
        }
    	
//    	Submit 
    	if(stepsCompleted == steps.size()){
    		Cache.clear();    		
    		submit();
    	}
    	
    	WizardStep currentStep = steps.get(step);

    	render("@" + currentStep.template, steps,currentStep, percent, properties);
    }

    /**
     * STEP 1 "Cambio password admin"
     */
    public static void changeAdminPsw(
    		int stepIndex,
    		@Required String admin_password,
    		@Required @Equals(value="admin_password",
    		message="Le password non corrispondono, riprovare") String admin_password_retype){
    	
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
	
    public static void setOffice(
    	    int stepIndex,
    		@Required String area,
    		@Required String institute,
    		@Required String institute_contraction,
    		@Required String seat,
    		String seat_address,
    		@Required String seat_code,
    		LocalDate seat_affiliation_date ){

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
    		if(seat_affiliation_date!=null){
        		properties.setProperty("seat_affiliation_date",seat_affiliation_date.toString());
    		}

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
	
    public static void seatManagerRole(
    		int stepIndex,
    		@Required String manager_surname,
    		@Required String manager_name,
    		@Required String manager_qualification,
    		String manager_badge_number,
    		String manager_registration_number,
    		@Valid LocalDate manager_birthday,
    		@Email String manager_email,
    		@Required LocalDate manager_contract_begin,
    		@Valid LocalDate manager_contract_end,
    		@Required String manager_username,
    		@Required String manager_password,
    		@Required @Equals(value="manager_password",
    		message="Le password non corrispondono, riprovare") String manager_password_retype){
		
    	if (validation.hasErrors()){
    	    params.flash(); 
    	    validation.keep();
    		wizard(stepIndex);
    	}
    		
    	List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    	Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);
    	
    	if(steps != null){

    		properties.setProperty("manager_surname",manager_surname);
    		properties.setProperty("manager_name",manager_name);
    		properties.setProperty("manager_qualification",manager_qualification);
    		properties.setProperty("manager_badge_number",manager_badge_number);
    		properties.setProperty("manager_registration_number",manager_registration_number);
    		properties.setProperty("manager_email",manager_email);
    		properties.setProperty("manager_contract_begin",manager_contract_begin.toString());
    		properties.setProperty("manager_username",manager_username);
    		properties.setProperty("manager_password",manager_password);

    		if(manager_birthday!= null){
        		properties.setProperty("manager_birthday",manager_birthday.toString());	
    		}
    		if(manager_contract_end!=null){
        		properties.setProperty("manager_contract_end",manager_contract_end.toString());
    		}
    		
    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
            	Logger.info("Completato lo step %s del wizard", stepIndex);
    		}

    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
       	wizard(stepIndex+1);
    }
	
    public static void summary(
    		int stepIndex,
    		@Required String area,
            @Required String institute,
            @Required String institute_contraction,
            @Required String seat,
            String seat_address,
            @Required String seat_code,
            @Valid LocalDate seat_affiliation_date,
            @Required String date_of_patron,        
            @Required @CheckWith (StringIsTime.class) String lunch_pause_start, 
            @Required @CheckWith (StringIsTime.class) String lunch_pause_end,
            @Email String email_to_contact,
            @Required String manager_surname,
    		@Required String manager_name,
    		@Required String manager_qualification,
    		String manager_badge_number,
    		String manager_registration_number,
    		@Valid LocalDate manager_birthday,
    		@Email String manager_email,
    		@Required LocalDate manager_contract_begin,
    		@Valid LocalDate manager_contract_end,
    		@Required String manager_username,
    		@Required String manager_password,
    		@Required @Equals(value="manager_password",
    		message="Le password non corrispondono, riprovare") String manager_password_retype){
		
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
             if(seat_affiliation_date!=null){
                 properties.setProperty("seat_affiliation_date",seat_affiliation_date.toString());
             }

             properties.setProperty("date_of_patron",date_of_patron);
             properties.setProperty("lunch_pause_start",lunch_pause_start);
             properties.setProperty("lunch_pause_end",lunch_pause_end);
             properties.setProperty("email_to_contact",email_to_contact);

             properties.setProperty("manager_surname",manager_surname);
             properties.setProperty("manager_name",manager_name);
             properties.setProperty("manager_qualification",manager_qualification);
             properties.setProperty("manager_badge_number",manager_badge_number);
             properties.setProperty("manager_registration_number",manager_registration_number);
             properties.setProperty("manager_email",manager_email);
             properties.setProperty("manager_contract_begin",manager_contract_begin.toString());
             properties.setProperty("manager_username",manager_username);
             properties.setProperty("manager_password",manager_password);

             if(manager_birthday!= null){
                 properties.setProperty("manager_birthday",manager_birthday.toString()); 
             }
             if(manager_contract_end!=null){
                 properties.setProperty("manager_contract_end",manager_contract_end.toString());
             }

    		
    		if(!steps.get(stepIndex).completed){
    			steps.get(stepIndex).complete();
            	Logger.info("Completato lo step %s del wizard", stepIndex);
    		}

    		Cache.safeSet(STEPS_KEY, steps, "10mn");
    		Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    	}
    	
    	wizard(stepIndex+1);
    }
	
    
    private static void submit(){
		Properties properties = new Properties();
		try{
			properties.load(new FileInputStream("conf/properties.conf"));	
		}
		catch(IOException f){
			Logger.error("Impossibile caricare il file properties.conf per la procedura di Wizard");	
		}
		
//      Cambio password user admin
		
		User admin = User.find("byUsername", "admin").first();
		admin.password = Codec.hexMD5(properties.getProperty("admin_password"));
		admin.save();
		
//		 Creazione Area,Istituto e Sede

//		Area
		Office area = new Office();
		area.name = properties.getProperty("area");
		area.save();
//		Istituto
		Office institute = new Office();
		institute.name = properties.getProperty("institute");
		institute.contraction = properties.getProperty("institute_contraction");
		institute.office = area;
		institute.save();
//		Sede
		Office seat = new Office();
		seat.name = properties.getProperty("seat");
		
		if(!properties.getProperty("seat_address").isEmpty()){
			seat.address = properties.getProperty("seat_address");
		}
		try{
			seat.code = Integer.decode(properties.getProperty("seat_code"));	
		}
		catch(Exception f){
			Logger.error("Errore nel parsing dal properties.conf: %s", f);	
		}
		if(properties.containsKey("seat_affiliation_date") && 
				!properties.getProperty("seat_affiliation_date").isEmpty()){
			seat.joiningDate = LocalDate.parse(properties.getProperty("seat_affiliation_date"));
		}
		seat.office = institute;
		seat.save();
		
		ConfGeneral.buildDefaultConfGeneral(seat);
		
		ConfYear.buildDefaultConfYear(seat, LocalDate.now().getYear());
		ConfYear.buildDefaultConfYear(seat, LocalDate.now().getYear() - 1);
		
		seat.setPermissionAfterCreation();
		
		ConfGeneral confGeneral;
		
//		INIT_USE_PROGRAM
		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.INIT_USE_PROGRAM, seat);
		confGeneral.fieldValue = LocalDate.now().toString();
		confGeneral.save();
//		DAY_OF_PATRON
		LocalDate dayMonth = DateUtility.dayMonth(properties.getProperty("date_of_patron"),Optional.<String>absent());

		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.DAY_OF_PATRON, seat);
		confGeneral.fieldValue = dayMonth.dayOfMonth().getAsString();
		confGeneral.save();
//		MONTH_OF_PATRON
		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.MONTH_OF_PATRON, seat);
		confGeneral.fieldValue = dayMonth.monthOfYear().getAsString();
		confGeneral.save();
//		MEAL_TIME_START_HOUR
		List<String> lunchStart = Splitter.on(":").trimResults().splitToList(properties.getProperty("lunch_pause_start"));    

		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.MEAL_TIME_START_HOUR, seat);
		confGeneral.fieldValue = lunchStart.get(0);
		confGeneral.save();
//		MEAL_TIME_START_MINUTE
		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.MEAL_TIME_START_MINUTE, seat);
		confGeneral.fieldValue = lunchStart.get(1);
		confGeneral.save();	
//		MEAL_TIME_END_HOUR
		List<String> lunchStop = Splitter.on(":").trimResults().splitToList(properties.getProperty("lunch_pause_end"));

		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.MEAL_TIME_END_HOUR, seat);
		confGeneral.fieldValue = lunchStop.get(0);
		confGeneral.save();
//		MEAL_TIME_END_MINUTE
		confGeneral = ConfGeneral.getConfGeneralByField(ConfGeneral.MEAL_TIME_END_MINUTE, seat);
		confGeneral.fieldValue = lunchStop.get(1);
		confGeneral.save();
//		EMAIL_TO_CONTACT
		confGeneral = new ConfGeneral(seat, ConfGeneral.EMAIL_TO_CONTACT, properties.getProperty("email_to_contact"));
		confGeneral.save();
//		institute_name
		confGeneral = new ConfGeneral(seat, ConfigurationFields.InstituteName.description, seat.contraction);
		confGeneral.save();
//		seat_code
		confGeneral = new ConfGeneral(seat, ConfigurationFields.SeatCode.description, seat.code.toString());
		confGeneral.save();
		
//		Creazione Profilo Amministratore
		
		Person p = new Person();
		p.name = properties.getProperty("manager_name");
		p.surname = properties.getProperty("manager_surname");
		
		Qualification qualification = Qualification.find("byQualification", 
				Integer.parseInt(properties.getProperty("manager_qualification"))).first();
		p.qualification = qualification;
		
		if(!properties.getProperty("manager_badge_number").isEmpty()){
			p.badgeNumber = properties.getProperty("manager_badge_number");

		}
		if(!properties.getProperty("manager_registration_number").isEmpty()){
			p.number = Integer.parseInt(properties.getProperty("manager_registration_number"));
		}
		
		if(properties.containsKey("manager_birthday") && 
				!properties.getProperty("manager_birthday").isEmpty()){
			p.birthday = LocalDate.parse(properties.getProperty("manager_birthday"));

		}
		if(!properties.getProperty("manager_email").isEmpty()){
			p.email = properties.getProperty("manager_email");
		}
		p.office = seat;
		p.save();
		
		Contract contract = new Contract();
		
		LocalDate contractBegin = LocalDate.parse(properties.getProperty("manager_contract_begin"));
		LocalDate contractEnd = null;
		if(properties.containsKey("manager_contract_end") && 
				!properties.getProperty("manager_contract_end").isEmpty()){
			contractEnd = LocalDate.parse(properties.getProperty("manager_contract_end"));
		}
		contract.beginContract = contractBegin;
		contract.expireContract = contractEnd;
	
		contract.onCertificate = true;
		contract.person = p;
		
		contract.save();
		contract.setVacationPeriods();
		contract.save();
		
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contractBegin;
		cwtt.endDate = contractEnd;
		cwtt.workingTimeType = WorkingTimeType.find("byDescription", "Normale").first();
		cwtt.contract = contract;
		cwtt.save();
		contract.save();
		
		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contractBegin;
		csp.endTo = contractEnd;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();
		
		User user = new User();
		//user.id = person.id;
		user.password = Codec.hexMD5(properties.getProperty("manager_password"));
		user.person = p;
		user.username = properties.getProperty("manager_username");
		user.save();
		p.user = user;
		p.save();
		
		UsersRolesOffices uro = new UsersRolesOffices();
		uro.office = seat;
		uro.user = user;
		uro.role = Role.find("byName", Role.PERSONNEL_ADMIN).first();
		
		uro.save();
		
		properties.remove("manager_password");
		properties.remove("admin_password");
		
		try{
    		properties.store(new FileOutputStream("conf/properties.conf"), "Wizard values file");
    		Logger.info("Salvato file properties.conf");
    	}
    	catch(IOException e){
    		flash.error(e.getMessage());    		
    	}
		
		Application.index();
    }
            
}
