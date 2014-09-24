package controllers;

import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import models.MealTicket;
import models.Person;
import models.User;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;
import dao.PersonDao;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

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
	
	public static class TemporaryPersonMealTicketRecap {
		public Person person;
		public int numberOfMealTicketToUse;
		public List<MealTicket> mealTickets;
		
		public TemporaryPersonMealTicketRecap(Person person) {
			this.person = person;
			
			//Numero ticket consegnati dal primo luglio
			this.mealTickets = MealTicket.find("select mt from MealTicket mt where mt.person = ? and mt.date > ?",
		    		person, new LocalDate(2014,7,1)).fetch();

			//TODO parametrizzare. Giorno iniziale da mettere in configurazione (e considerare la scadenza buoni pasto??)
			int numberOfMealTicketToUse = 0;
			numberOfMealTicketToUse = numberOfMealTicketToUse + PersonUtility.numberOfMealTicketToUse(person, 2014, 7);
			numberOfMealTicketToUse = numberOfMealTicketToUse + PersonUtility.numberOfMealTicketToUse(person, 2014, 8);
			numberOfMealTicketToUse = numberOfMealTicketToUse + PersonUtility.numberOfMealTicketToUse(person, 2014, 9);	
			this.numberOfMealTicketToUse = numberOfMealTicketToUse;
		}
		
		public boolean isOk(){
			return remaining() >= 5;
		}
		public boolean isWarning(){			
			return remaining() < 5 && remaining() > 0;
		}
		public boolean isDanger(){
			return remaining() < 1;
		}
		
		private int remaining() {
			return mealTickets.size() - numberOfMealTicketToUse;
		}
		//Implementare setter per gestire i casi particolari
		//Introdurre il concetto di periodo se serve
	}
	
	
	@NoCheck
	public static void recapMealTickets() {
		
		List<Person> personList = PersonDao.list( Optional.<String>absent(),
				Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), LocalDate.now(), true).list();

		List<TemporaryPersonMealTicketRecap> mealTicketRecaps = Lists.newArrayList();
		
		for(Person person : personList) {
			TemporaryPersonMealTicketRecap recap = new TemporaryPersonMealTicketRecap(person);
			mealTicketRecaps.add(recap);
		}
		
		render(mealTicketRecaps); 
		
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
			ticketToAdd.addAll(generateBlockMealTicket(codeBlock1, dimBlock1));
		}
		
		//Blocco2
		if(codeBlock2 != null && dimBlock2 != null) {
			ticketToAdd.addAll(generateBlockMealTicket(codeBlock2, dimBlock2));
		}
		
		//Blocco3
		if(codeBlock3 != null && dimBlock3 != null) {
			ticketToAdd.addAll(generateBlockMealTicket(codeBlock3, dimBlock3));
		}
		
		User admin = Security.getUser().get();
		
		//Controllo esistenza
		for(MealTicket mealTicket : ticketToAdd) {
					
			MealTicket exist = MealTicket.find("byCode", mealTicket.code).first();
			if(exist!=null)  {
				
				flash.error("Il buono pasto con codice %s risulta già essere assegnato alla persona %s %s in data %s."
						+ " L'Operazione è annullata", mealTicket.code, exist.person.name, exist.person.surname, exist.date);
				MealTickets.manageMealTickets(null);
			}
		
		}
		
		//Persistenza
		for(MealTicket mealTicket : ticketToAdd) {
		
			mealTicket.date = LocalDate.now();
			mealTicket.person = person;
			mealTicket.admin = admin.person; 
			mealTicket.save();
		}

		flash.success("Inseriti %s buoni pasto per %s %s", ticketToAdd.size(),
				person.name ,person.surname);
		MealTickets.manageMealTickets(null);

	}
	
	public static void deletePersonMealTicket(Long personId, Integer codeBlock) {
		
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		if(codeBlock == null){
			flash.error("Impossibile trovare il codice blocco specificato. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.person = ? and mt.block = ?",
				person, codeBlock).fetch();
		
		for(MealTicket mealTicket : mealTicketList) {
			
			mealTicket.delete();
		}
		
		flash.success("Rimossi i buoni pasto specificati per %s %s", person.name , person.surname);
		MealTickets.manageMealTickets(null);
	}
	
	public static void showPersonBlockMealTicket(Long personId, Integer codeBlock) {
		
		Person person = Person.findById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.manageMealTickets(null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> mealTicketList = MealTicket.find("Select mt from MealTicket mt "
				+ "where mt.person = ? and mt.block = ?",
				person, codeBlock).fetch();
		
		render(mealTicketList, person, codeBlock);
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
