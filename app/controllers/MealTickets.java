package controllers;

import helpers.PaginableList;

import java.util.List;

import javax.inject.Inject;

import manager.MealTicketManager;
import manager.recaps.mealTicket.BlockMealTicket;
import manager.recaps.mealTicket.MealTicketRecap;
import manager.recaps.mealTicket.MealTicketRecapFactory;
import models.Contract;
import models.MealTicket;
import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.MealTicketDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

@With( {Resecure.class, RequestInit.class} )
public class MealTickets  extends Controller {
	
	@Inject
	static SecurityRules rules;
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static MealTicketManager mealTicketManager;
	
	@Inject
	static MealTicketDao mealTicketDao;
	
	@Inject
	static MealTicketRecapFactory mealTicketFactory;
	
	@Inject 
	static IWrapperFactory wrapperFactory;
	
	public static void recapMealTickets(String name, Integer page, Integer max, 
			List<Integer> blockIdsAdded, Long personIdAdded) {

		if(page == null)
			page = 0;
				
		rules.checkIfPermitted();
		
		//La lista di tutte le person con contratto attivo ad oggi.
		final List<Person> personList = PersonDao.list( 
				Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), 
				false, LocalDate.now(), LocalDate.now(), true).list();

		List<MealTicketRecap> mealTicketRecaps = Lists.newArrayList();
		
		for(Person person : personList) {

			MealTicketRecap recap = mealTicketFactory.create(
					wrapperFactory.create(person).getCurrentContract().get());
			
			if( max == null || recap.getRemaining() <= max ) {
				mealTicketRecaps.add(recap);
			}
		}
		
		PaginableList<MealTicketRecap> paginableList = new PaginableList<MealTicketRecap>(mealTicketRecaps, page);
		
		//Riepilogo buoni inseriti nella precedente action
		if(personIdAdded != null && blockIdsAdded != null) {
			Person personAdded = PersonDao.getPersonById(personIdAdded);
			Preconditions.checkNotNull(personAdded);
			Preconditions.checkArgument(personAdded.isPersistent());
			
			List<BlockMealTicket> blockAdded = null;
			List<MealTicket> mealTicketAdded = mealTicketDao.getMealTicketsInCodeBlockIds(blockIdsAdded);
			blockAdded = mealTicketManager.getBlockMealTicketFromMealTicketList(mealTicketAdded);

			render(paginableList, page, max, name, blockAdded, personAdded);
		}
		
		render(paginableList, page, max, name);
		
	}
	
	public static void quickBlocksInsert(Long personId, String name, Integer page, Integer max) {
		
		Person person = PersonDao.getPersonById(personId);

		Preconditions.checkArgument(person.isPersistent());
		
		rules.checkIfPermitted(person.office);
		
		Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
		Preconditions.checkState(contract.isPresent());
				
		MealTicketRecap recap = mealTicketFactory.create(contract.get());
		
		Contract previousContract = PersonDao.getPreviousPersonContract(contract.get());
		
		MealTicketRecap recapPrevious = null;
		
		if(previousContract != null)
			recapPrevious = mealTicketFactory.create(previousContract);
		
		LocalDate today = LocalDate.now();
		
		LocalDate expireDate = mealTicketDao.getFurtherExpireDateInOffice(person.office);
		
		User admin = Security.getUser().get();

		render(recap, recapPrevious, today, admin, expireDate, name, page, max);
	}
	
	
	public static void mealTicketsLegacy(Long contractId, String name, Integer page, Integer max) {
		
		Contract contract = ContractDao.getContractById(contractId);
		Preconditions.checkNotNull(contract);
		Preconditions.checkArgument(contract.isPersistent());
		
		rules.checkIfPermitted(contract.person.office);
		
		int mealTicketsTransfered = mealTicketManager.mealTicketsLegacy(contract);
		
		if(mealTicketsTransfered == 0) {
			flash.error("Non e' stato trasferito alcun buono pasto. Riprovare o effettuare una segnalazione.");
		}
		else {
			flash.success("Trasferiti con successo %s buoni pasto per %s %s", 
					mealTicketsTransfered, contract.person.name, contract.person.surname);
		}
		
		MealTickets.recapMealTickets(name, page, max, null, null);
	}
	
	
	public static void submitPersonMealTicket(Long personId, 
			String name, Integer page, Integer max,
			Integer codeBlock1, Integer dimBlock1, LocalDate expireDate1,
			Integer codeBlock2, Integer dimBlock2, LocalDate expireDate2,
			Integer codeBlock3, Integer dimBlock3, LocalDate expireDate3) {
		
		//Controllo dei parametri
		Person person = PersonDao.getPersonById(personId);
		if(person == null) {
			
			flash.error("Impossibile trovare la persona specificata. Operazione annullata");
			MealTickets.recapMealTickets(name, page, max, null, null);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<MealTicket> ticketToAdd = Lists.newArrayList();
		List<Integer> blockIdsToAdd = Lists.newArrayList();
		
		//Blocco1
		if(codeBlock1 != null && dimBlock1 != null) {
			ticketToAdd.addAll(mealTicketManager.buildBlockMealTicket(codeBlock1, dimBlock1, expireDate1));
			blockIdsToAdd.add(codeBlock1);
		}
		
		//Blocco2
		if(codeBlock2 != null && dimBlock2 != null) {
			ticketToAdd.addAll(mealTicketManager.buildBlockMealTicket(codeBlock2, dimBlock2, expireDate2));
			blockIdsToAdd.add(codeBlock2);
		}
		
		//Blocco3
		if(codeBlock3 != null && dimBlock3 != null) {
			ticketToAdd.addAll(mealTicketManager.buildBlockMealTicket(codeBlock3, dimBlock3, expireDate3));
			blockIdsToAdd.add(codeBlock3);
		}
		
		User admin = Security.getUser().get();
		
		//Controllo esistenza
		for(MealTicket mealTicket : ticketToAdd) {
					
			MealTicket exist = mealTicketDao.getMealTicketByCode(mealTicket.code);
			if(exist!=null)  {
				
				flash.error("Il buono pasto con codice %s risulta già essere assegnato alla persona %s %s in data %s."
						+ " L'Operazione è annullata", mealTicket.code, exist.contract.person.name, exist.contract.person.surname, exist.date);
				MealTickets.recapMealTickets(name, page, max, null, null);
			}
		}
		
		//Persistenza
		for(MealTicket mealTicket : ticketToAdd) {
			mealTicket.date = LocalDate.now();
			mealTicket.contract = ContractDao.getContract(mealTicket.date, person);
			mealTicket.admin = admin.person; 
			mealTicket.save();
		}

		// Questo flash success è sostituito dal riepilogo al momento della recap 
		//flash.success("Inseriti %s buoni pasto per %s %s", ticketToAdd.size(),
		//		person.name ,person.surname);
		
		
		MealTickets.recapMealTickets(name, page, max, blockIdsToAdd, personId);

	}
	
	public static void deletePersonMealTicket(Integer codeBlock, 
			String name, Integer page, Integer max) {
		
		//TODO un metodo deletePersonMealTicketConfirmed
		
		if(codeBlock == null){
			flash.error("Impossibile trovare il codice blocco specificato. Operazione annullata");
			MealTickets.recapMealTickets(name, page, max, null, null);
		}
		List<Integer> codeBlockIds = Lists.newArrayList();
		codeBlockIds.add(codeBlock);
		List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlockIds(codeBlockIds);
		
		if(mealTicketList == null || mealTicketList.size() == 0) {
			flash.error("Il blocco selezionato è inesistente. Operazione annullata");
			MealTickets.recapMealTickets(name, page, max, null, null);
		}
		
		Person person = mealTicketList.get(0).contract.person;
		
		rules.checkIfPermitted(person.office);
		
		int deleted = 0;
		for(MealTicket mealTicket : mealTicketList) {
			
			mealTicket.delete();
			deleted++;
		}
		
		flash.success("Rimosso blocco %s con dimensione %s per %s %s", codeBlock, deleted,
				person.name , person.surname);
		
		MealTickets.recapMealTickets(name, page, max, null, null);
	}


}
