package controllers;

import helpers.PaginableList;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ContractMonthRecapManager;
import manager.MealTicketManager;
import manager.recaps.mealTicket.BlockMealTicket;
import manager.recaps.mealTicket.MealTicketRecap;
import manager.recaps.mealTicket.MealTicketRecapFactory;
import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Person;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.ContractMonthRecapDao;
import dao.MealTicketDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

@With( {Resecure.class, RequestInit.class} )
public class MealTickets  extends Controller {

	@Inject
	private static SecurityRules rules;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static MealTicketRecapFactory mealTicketFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static MealTicketDao mealTicketDao;
	@Inject
	private static MealTicketManager mealTicketManager;
	@Inject
	private static ContractMonthRecapDao contractMonthRecapDao;
	@Inject
	private static ContractMonthRecapManager contractMonthRecapManager;
	@Inject
	private static ContractDao contractDao;

	public static void recapMealTickets(String name, Integer page, Integer max, 
			List<Integer> blockIdsAdded, Long personIdAdded) {

		// TODO: inserire il filtro degli office
		
		if(page == null) {
			page = 0;
		}

		List<ContractMonthRecap> monthRecapList = contractMonthRecapDao
				.getPersonMealticket(YearMonth.now(), Optional.fromNullable(max));
		
		PaginableList<ContractMonthRecap> paginableList = 
				new PaginableList<ContractMonthRecap>(monthRecapList, page);
		
		List<MealTicketRecap> mealTicketRecaps = Lists.newArrayList();
		
		for(ContractMonthRecap monthRecap : paginableList.getPaginatedItems() ) {
			Optional<MealTicketRecap> recap = mealTicketFactory
					.create(monthRecap.contract);
			if(recap.isPresent()) {
				mealTicketRecaps.add(recap.get());
			}
		}
		
		//Riepilogo buoni inseriti nella precedente action
		if(personIdAdded != null && blockIdsAdded != null) {
			Person personAdded = personDao.getPersonById(personIdAdded);
			Preconditions.checkNotNull(personAdded);
			Preconditions.checkArgument(personAdded.isPersistent());

			List<BlockMealTicket> blockAdded = null;
			List<MealTicket> mealTicketAdded = mealTicketDao.getMealTicketsInCodeBlockIds(blockIdsAdded);
			blockAdded = mealTicketManager.getBlockMealTicketFromMealTicketList(mealTicketAdded);

			render(paginableList, mealTicketRecaps, page, max, name, blockAdded, personAdded);
		}

		render(paginableList, mealTicketRecaps, page, max, name);

	}

	public static void quickBlocksInsert(Long personId, String name, Integer page, Integer max) {

		Person person = personDao.getPersonById(personId);
		Preconditions.checkArgument(person.isPersistent());
		rules.checkIfPermitted(person.office);
		
		MealTicketRecap recap;
		MealTicketRecap recapPrevious = null; // TODO: nella vista usare direttamente optional

		Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
		Preconditions.checkState(contract.isPresent());
		
		// riepilogo contratto corrente
		Optional<MealTicketRecap> currentRecap = mealTicketFactory.create(contract.get());
		Preconditions.checkState(currentRecap.isPresent());
		recap = currentRecap.get();
		
		//riepilogo contratto precedente
		Contract previousContract = personDao.getPreviousPersonContract(contract.get());
		if(previousContract != null) {
			Optional<MealTicketRecap> previousRecap = mealTicketFactory.create(previousContract);
			if(previousRecap.isPresent()) {
				recapPrevious = previousRecap.get();
			}
		}

		LocalDate today = LocalDate.now();
		LocalDate expireDate = mealTicketDao.getFurtherExpireDateInOffice(person.office);
		User admin = Security.getUser().get();

		render(recap, recapPrevious, today, admin, expireDate, name, page, max);
	}


	public static void mealTicketsLegacy(Long contractId, String name, Integer page, Integer max) {

		Contract contract = contractDao.getContractById(contractId);
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
		Person person = personDao.getPersonById(personId);
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

		Set<Contract> contractUpdated = Sets.newHashSet();
		
		//Persistenza
		for(MealTicket mealTicket : ticketToAdd) {
			mealTicket.date = LocalDate.now();
			mealTicket.contract = contractDao.getContract(mealTicket.date, person);
			mealTicket.admin = admin.person; 
			mealTicket.save();
			
			contractUpdated.add(mealTicket.contract);
		}
		
		for(Contract contract : contractUpdated) {
			contractMonthRecapManager.populateContractMonthRecap(contract, 
					Optional.<YearMonth>absent());
		}

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

		Set<Contract> contractUpdated = Sets.newHashSet();
		
		int deleted = 0;
		for(MealTicket mealTicket : mealTicketList) {

			mealTicket.delete();
			deleted++;
			
			contractUpdated.add(mealTicket.contract);
		}

		for(Contract contract : contractUpdated) {
			contractMonthRecapManager.populateContractMonthRecap(contract, 
					Optional.<YearMonth>absent());
		}
		
		flash.success("Rimosso blocco %s con dimensione %s per %s %s", codeBlock, deleted,
				person.name , person.surname);

		MealTickets.recapMealTickets(name, page, max, null, null);
	}


}
