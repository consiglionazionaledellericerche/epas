package controllers;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import models.MealTicket;
import models.Person;
import models.User;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonDao;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With( {Resecure.class, RequestInit.class} )
public class MealTickets  extends Controller {
	
	@Inject
	static SecurityRules rules;
	
	public static void manageMealTickets(Integer year, Integer quarter, String name) {
		
		rules.checkIfPermitted();
		
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
			Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), LocalDate.now(), true).list();
		
		List<Person> personWithTicket = Lists.newArrayList();
		
		List<Person> personNoTicket = Lists.newArrayList();
		
		for(Person person : personList) {
			
			MealTicket mealTicket = MealTicket.find("Select mt from MealTicket mt where mt.person = ? and mt.year = ? and mt.quarter = ?",
					person, year, quarter).first();
			
			if(mealTicket == null)
				personNoTicket.add(person);
			else
				personWithTicket.add(person);
		}
		
		LocalDate today = new LocalDate();
		
		User admin = Security.getUser().get();
		
		render(personNoTicket, personWithTicket, year, quarter, today, admin);
		
		
		
	}
	
	public static void submitPersonMealTicketInQuarter(Long personId, Integer year, Integer quarter, 
			Integer codeBlock1, Integer dimBlock1,
			Integer codeBlock2, Integer dimBlock2,
			Integer codeBlock3, Integer dimBlock3) {
		
		//Controllo dei parametri
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		rules.checkIfPermitted(person.office);
		
		if(year == null || quarter == null) {
			
			flash.error("Errore in fase di parsing dei parametri. Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		if(codeBlock1 == null || codeBlock2 == null || codeBlock3 == null || 
				dimBlock1 == null || dimBlock2 == null || dimBlock3 == null) {
			
			flash.error("Errore in fase di parsing dei parametri Inserire correttamente tutte le informazioni."
					+ " Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		if(codeBlock1.equals(codeBlock2) || codeBlock1.equals(codeBlock3) || codeBlock2.equals(codeBlock3)) {
			
			flash.error("Errore in fase di inserimento codice blocchi. Inserire tre blocchi distinti."
					+ " Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		User admin = Security.getUser().get();
		
		List<MealTicket> ticketToAdd = Lists.newArrayList();
		ticketToAdd.addAll(generateBlockMealTicket(codeBlock1, dimBlock1));
		ticketToAdd.addAll(generateBlockMealTicket(codeBlock2, dimBlock2));
		ticketToAdd.addAll(generateBlockMealTicket(codeBlock3, dimBlock3));
		
		//Controllo esistenza
		for(MealTicket mealTicket : ticketToAdd) {
					
			MealTicket exist = MealTicket.find("byCode", mealTicket.code).first();
			if(exist!=null)  {
				
				flash.error("Il buono pasto con codice %s risulta già essere assegnato alla persona %s %s in data %s."
						+ " L'Operazione è annullata", mealTicket.code, exist.person.name, exist.person.surname, exist.date);
				MealTickets.manageMealTickets(year, quarter, null);
			}
		
		}
		
		//Persistenza
		for(MealTicket mealTicket : ticketToAdd) {
		
			mealTicket.date = LocalDate.now();
			mealTicket.person = person;
			mealTicket.year = year;
			mealTicket.quarter = quarter;
			mealTicket.admin = admin.person; 
			mealTicket.save();
		}

		flash.success("Inseriti %s buoni pasto per %s %s nel trimestre %s del %s", ticketToAdd.size(),
				person.name ,person.surname, quarter, year);
		MealTickets.manageMealTickets(year, quarter, null);

	}
	
	public static void deletePersonMealTicketInQuarter(Long personId, Integer year, Integer quarter) {
		
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.person = ? and mt.year = ? and mt.quarter = ?",
				person, year, quarter).fetch();
		
		for(MealTicket mealTicket : mealTicketList) {
			
			mealTicket.delete();
		}
		
		flash.success("Rimossi i buoni pasto per %s %s nel trimestre %s del %s", person.name , person.surname, quarter, year);
		MealTickets.manageMealTickets(year, quarter, null);
	}
	
	public static void showPersonBlockMealTicket(Long personId, Integer codeBlock, Integer year, Integer quarter) {
		
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(year, quarter, null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.person = ? and mt.year = ? and mt.quarter = ? and mt.block = ?",
				person, year, quarter, codeBlock).fetch();
		
		render(mealTicketList, year, quarter, person, codeBlock);
	}
	
	private static List<MealTicket> generateBlockMealTicket(Integer codeBlock, Integer dimBlock) {
		
		
		List<MealTicket> mealTicketList = Lists.newArrayList();
				
		for(int i=1; i<=dimBlock; i++) {
			
			MealTicket mealTicket = new MealTicket();
			mealTicket.block = codeBlock;
			mealTicket.number = i;
			
			if(i<10) 
				mealTicket.code = codeBlock + "0" + i;
			else
				mealTicket.code = "" + codeBlock + i;
			
			mealTicketList.add(mealTicket);
			
			Logger.info(mealTicket.code);
		}
		
		return  mealTicketList;
	}

}
