package controllers;

import java.util.List;

import javax.inject.Inject;

import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Contract;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

@With( {Resecure.class, RequestInit.class} )
public class Vacations extends Controller{
		
	@Inject
	private	static VacationsRecapFactory vacationsFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	
	public static void show(Integer year) {
		
		Optional<User> currentUser = Security.getUser();
		
		Preconditions.checkState(currentUser.isPresent());
		Preconditions.checkNotNull(currentUser.get().person);
		
		IWrapperPerson person = wrapperFactory.create(currentUser.get().person);

		if(year == null) {
			year = LocalDate.now().getYear(); 
		}
		
		List<Contract> contractList = person.getYearContracts(year);
		
		if(contractList.isEmpty()) {
			flash.error("Non ci sono contratti attivi nel %s", year);
			YearMonth lastActiveMonth = person.getLastActiveMonth();
			show(lastActiveMonth.getYear());
		}
		
		List<VacationsRecap> vacationsRecapList = Lists.newArrayList();
		
		for(Contract contract : contractList) {
			
			Optional<VacationsRecap> vacationsRecap;

			vacationsRecap = vacationsFactory.create(
					year, contract, LocalDate.now(), true);

			Preconditions.checkState(vacationsRecap.isPresent());
			
			vacationsRecapList.add(vacationsRecap.get());
		}

		render(vacationsRecapList);
	
		
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
		
		Optional<VacationsRecap> vr = vacationsFactory.create(
				anno, contract.get(), LocalDate.now(), true);
	
		Preconditions.checkState(vr.isPresent());

		VacationsRecap vacationsRecap = vr.get();
		
		boolean activeVacationCurrentYear = true;
		
		render("@recapVacation", vacationsRecap, activeVacationCurrentYear);
		
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
			
		Optional<VacationsRecap> vr = vacationsFactory.create(
					anno, contract.get(), LocalDate.now(), true);
		
		Preconditions.checkState(vr.isPresent());

		VacationsRecap vacationsRecap = vr.get();
		
		boolean activeVacationLastYear = true;
		
		render("@recapVacation", vacationsRecap, activeVacationLastYear);
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

		Optional<VacationsRecap> vr = vacationsFactory.create(
				anno, contract.get(), LocalDate.now(), true);
	
		Preconditions.checkState(vr.isPresent());

		VacationsRecap vacationsRecap = vr.get();
		
		boolean activePermission = true;
		
		render("@recapVacation", vacationsRecap, activePermission);
	}
	
	
}
