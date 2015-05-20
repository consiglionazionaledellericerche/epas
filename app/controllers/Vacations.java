package controllers;

import javax.inject.Inject;

import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Contract;
import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;

import dao.wrapper.IWrapperFactory;
import dto.VacationsShowDto;

@With( {Resecure.class, RequestInit.class} )
public class Vacations extends Controller{
		
	@Inject
	private	static VacationsRecapFactory vacationsFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	
	public static void show(Integer year) {
		
		Optional<User> currentUser = Security.getUser();
		if( ! currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Person person = currentUser.get().person;
		
		if(year == null) {
			year = LocalDate.now().getYear(); 
		}

		Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
		
		Preconditions.checkState(contract.isPresent());
		
		VacationsRecap vacationsRecap;
		Optional<VacationsRecap> vacationsRecapPrevious;
		
		vacationsRecap = vacationsFactory.create(
					year, contract.get(), LocalDate.now(), true);
		
		vacationsRecapPrevious = Optional.fromNullable(
					vacationsFactory.create(year-1, contract.get(), 
							new LocalDate(year-1,12,31), true));
		
		VacationsShowDto vacationShowDto = VacationsShowDto
				.build(year, vacationsRecap, vacationsRecapPrevious);

		render(vacationsRecap, vacationsRecapPrevious, vacationShowDto);
	
		
	}
	
	public static void vacationsCurrentYear(Integer anno){
		
		Optional<User> currentUser = Security.getUser();
		if( ! currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Optional<Contract> contract = wrapperFactory.create(currentUser.get().person)
				.getCurrentContract();
		
		Preconditions.checkState(contract.isPresent());
		
		VacationsRecap vacationsRecap = vacationsFactory.create(
					anno, contract.get(), LocalDate.now(), true);
		render(vacationsRecap);

	}
	

	
	public static void vacationsLastYear(Integer anno){
		
		Optional<User> currentUser = Security.getUser();
		if( ! currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
    	
		Optional<Contract> contract = wrapperFactory.create(currentUser.get().person)
				.getCurrentContract();
		
		Preconditions.checkState(contract.isPresent());
			
		VacationsRecap vacationsRecap = vacationsFactory.create(
					anno, contract.get(), LocalDate.now(), true);
		render(vacationsRecap);
	}
	
	
	public static void permissionCurrentYear(Integer anno){
		
		Optional<User> currentUser = Security.getUser();
		if( ! currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Optional<Contract> contract = wrapperFactory.create(currentUser.get().person)
				.getCurrentContract();
		
		Preconditions.checkState(contract.isPresent());

		VacationsRecap vacationsRecap = vacationsFactory.create(
					anno, contract.get(), LocalDate.now(), true);
		render(vacationsRecap);
	}
	
}
