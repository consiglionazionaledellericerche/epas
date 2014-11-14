package controllers;

import helpers.ModelQuery.SimpleResults;

import java.util.List;

import javax.inject.Inject;

import manager.MealTicketManager;
import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.MealTicket;
import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;
import dao.MealTicketDao;
import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class MealTickets  extends Controller {
	
	@Inject
	static SecurityRules rules;
	
	public static void manageMealTickets(String name) {
		
		rules.checkIfPermitted();
		
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
			Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), LocalDate.now(), true).list();
		
		LocalDate today = new LocalDate();
		
		User admin = Security.getUser().get();
		
		render(personList, today, admin);
		
	}
	
	@NoCheck
	public static void recapMealTickets(String name, Integer page) {

		if(page==null)
			page = 0;

		SimpleResults<Person> simpleResults = PersonDao.list( Optional.fromNullable(name),
				Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), LocalDate.now(), true);

		List<Person> personList = simpleResults.paginated(page).getResults();
		
		List<PersonResidualMonthRecap> personsMonthRecaps = Lists.newArrayList();
		
		for(Person person : personList) {
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(person.getCurrentContract(), LocalDate.now().getYear(), null);
			personsMonthRecaps.add(c.getMese(LocalDate.now().getMonthOfYear()));
		}
		
		render(personsMonthRecaps, simpleResults); 
		
	}
	
	
	public static void submitPersonMealTicket(Long personId, 
			Integer codeBlock1, Integer dimBlock1,
			Integer codeBlock2, Integer dimBlock2,
			Integer codeBlock3, Integer dimBlock3) {
		
		//Controllo dei parametri
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> ticketToAdd = Lists.newArrayList();
		
		//Blocco1
		if(codeBlock1 != null && dimBlock1 != null) {
			ticketToAdd.addAll(MealTicketManager.buildBlockMealTicket(codeBlock1, dimBlock1));
		}
		
		//Blocco2
		if(codeBlock2 != null && dimBlock2 != null) {
			ticketToAdd.addAll(MealTicketManager.buildBlockMealTicket(codeBlock2, dimBlock2));
		}
		
		//Blocco3
		if(codeBlock3 != null && dimBlock3 != null) {
			ticketToAdd.addAll(MealTicketManager.buildBlockMealTicket(codeBlock3, dimBlock3));
		}
		
		User admin = Security.getUser().get();
		
		//Controllo esistenza
		for(MealTicket mealTicket : ticketToAdd) {
					
			MealTicket exist = MealTicket.find("byCode", mealTicket.code).first();
			if(exist!=null)  {
				
				flash.error("Il buono pasto con codice %s risulta già essere assegnato alla persona %s %s in data %s."
						+ " L'Operazione è annullata", mealTicket.code, exist.contract.person.name, exist.contract.person.surname, exist.date);
				MealTickets.manageMealTickets(null);
			}
		}
		
		//Persistenza
		for(MealTicket mealTicket : ticketToAdd) {
		
			mealTicket.date = LocalDate.now();
			mealTicket.contract = person.getContract(mealTicket.date);
			mealTicket.admin = admin.person; 
			mealTicket.save();
		}

		flash.success("Inseriti %s buoni pasto per %s %s", ticketToAdd.size(),
				person.name ,person.surname);
		MealTickets.manageMealTickets(null);

	}
	
	public static void deletePersonMealTicket(Integer codeBlock) {
		
		//TODO un metodo deletePersonMealTicketConfirmed
		
		if(codeBlock == null){
			flash.error("Impossibile trovare il codice blocco specificato. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
				
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.block = ?",
				codeBlock).fetch();
		
		if(mealTicketList == null || mealTicketList.size() == 0) {
			flash.error("Il blocco selezionato è inesistente. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		Person person = mealTicketList.get(0).contract.person;
		
		rules.checkIfPermitted(person.office);
		
		
		for(MealTicket mealTicket : mealTicketList) {
			
			mealTicket.delete();
		}
		
		flash.success("Rimossi i buoni pasto specificati per %s %s", person.name , person.surname);
		MealTickets.manageMealTickets(null);
	}
	
	public static void showPersonBlockMealTicket(Integer codeBlock) {
		
		List<MealTicket> mealTicketList = MealTicketDao.getMealTicketInBlock(codeBlock);
		
		if(mealTicketList == null || mealTicketList.size() == 0) {
			flash.error("Il blocco selezionato è inesistente. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		Person person = mealTicketList.get(0).contract.person;
		
		rules.checkIfPermitted(person.office);
		
		render(mealTicketList, person, codeBlock);
	}
	


}
