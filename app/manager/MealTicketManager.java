package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import org.joda.time.LocalDate;

import manager.recaps.mealTicket.BlockMealTicket;
import manager.recaps.mealTicket.MealTicketRecap;
import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Contract;
import models.MealTicket;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.MealTicketDao;
import dao.PersonDao;

/**
 * Manager per MealTicket
 * @author alessandro
 *
 */
public class MealTicketManager {
	
	@Inject
	public PersonResidualYearRecapFactory yearFactory;
	
	/**
	 * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock
	 * @param codeBlock il codice del blocco di meal ticket
	 * @param dimBlock la dimensione del blocco di meal ticket
	 * @param expireDate la data di scadenza dei buoni nel blocco
	 * @return la lista di MealTicket appartenenti al blocco.
	 */
	public List<MealTicket> buildBlockMealTicket(
			Integer codeBlock, Integer dimBlock, LocalDate expireDate) {

		List<MealTicket> mealTicketList = Lists.newArrayList();

		for(int i=1; i<=dimBlock; i++) {

			MealTicket mealTicket = new MealTicket();
			mealTicket.expireDate = expireDate;
			mealTicket.block = codeBlock;
			mealTicket.number = i;

			if(i<10) 
				mealTicket.code = codeBlock + "0" + i;
			else
				mealTicket.code = "" + codeBlock + i;

			mealTicketList.add(mealTicket);
		}

		return mealTicketList;
	}
	
	
	/**
	 * Verifica che nel contratto precedente a contract siano avanzati dei buoni
	 * pasto assegnati. In tal caso per quei buoni pasto viene modificata la relazione
	 * col contratto successivo e cambiata la data di attribuzione in modo che ricada 
	 * all'inizio del nuovo contratto.
	 * @param contract
	 * @return il numero di buoni pasto trasferiti fra un contratto e l'altro.
	 */
	public int mealTicketsLegacy(Contract contract) {
		
		Contract previousContract = PersonDao.getPreviousPersonContract(contract);
		if(previousContract == null)
			return 0;
		
		DateInterval previousContractInterval = previousContract.getContractDateInterval();
		
		//Data inizio utilizzo mealticket
		PersonResidualYearRecap c = 
				yearFactory.create(previousContract, previousContractInterval.getEnd().getYear(), null);
		PersonResidualMonthRecap monthRecap = c.getMese(previousContractInterval.getEnd().getMonthOfYear());
		
		if(monthRecap == null)
			return 0;
		
		if(monthRecap.buoniPastoResidui == 0)
			return 0;
		
		int mealTicketsTransfered = 0;
		
		List<MealTicket> contractMealTicketsDesc = MealTicketDao.getOrderedMealTicketInContract(previousContract);
		for(int i = 0; i<monthRecap.buoniPastoResidui; i++) {

			MealTicket ticketToChange = contractMealTicketsDesc.get(i);
			ticketToChange.contract = contract;
			ticketToChange.date = contract.beginContract;
			ticketToChange.save();
			mealTicketsTransfered++;
		}

		return mealTicketsTransfered;
	}
	
	/**
	 * Ritorna l'intervallo valido ePAS per il contratto riguardo la gestione dei buoni pasto.
	 * (scarto la parte precedente a source se definita, e la parte precedente alla data inizio 
	 * utilizzo per la sede della persona).
	 * @return null in caso non vi siano giorni coperti dalla gestione dei buoni pasto.
	 */
	public DateInterval getContractMealTicketDateInterval(Contract contract) {
		
		DateInterval contractDataBaseInterval = ContractManager.getContractDatabaseDateInterval(contract);
		
		LocalDate officeStartDate = MealTicketDao.getMealTicketStartDate(contract.person.office);
		if(officeStartDate == null)
			return null;
		
		if(officeStartDate.isBefore(contractDataBaseInterval.getBegin()))
			return contractDataBaseInterval;
		
		if(DateUtility.isDateIntoInterval(officeStartDate, contractDataBaseInterval))
			return new DateInterval(officeStartDate, contractDataBaseInterval.getEnd());
		
		return null;
	}
	
	/**
	 * Ritorna i blocchi inerenti la lista di buoni pasto recap.mealTicketsReceivedOrdered,
	 * consegnati nell'intervallo temporale indicato. N.B. la lista di di cui sopra è ordinata
	 * per data di scadenza e per codice blocco in ordine ascendente.
	 * @param recap
	 * @param interval
	 * @return
	 */
	public List<BlockMealTicket> getBlockMealTicketReceivedIntoInterval(
			MealTicketRecap recap, DateInterval interval) {
		
		List<BlockMealTicket> blockList = Lists.newArrayList();
		BlockMealTicket currentBlock = null;
					
		for(MealTicket mealTicket : recap.getMealTicketsReceivedOrdered() ) {
			if( DateUtility.isDateIntoInterval(mealTicket.date, interval) ) {
				
				if(currentBlock == null) {
					currentBlock = new BlockMealTicket(mealTicket.block);
					currentBlock.mealTickets.add(mealTicket);
					continue;
				}
				
				if(currentBlock.codeBlock.equals( mealTicket.block )) {
					currentBlock.mealTickets.add(mealTicket);
					continue;
				}
								
				blockList.add(currentBlock);
				currentBlock = new BlockMealTicket(mealTicket.block);
				currentBlock.mealTickets.add(mealTicket);
			}
		}
		if(currentBlock != null) {
			blockList.add(currentBlock);
		}	
		return blockList;
		
	}
	
	/**
	 * 
	 * @param mealTicket
	 * @return
	 */
	public List<BlockMealTicket> getBlockMealTicketFromMealTicketList(
			List<MealTicket> mealTicketList) {
		
		//FIXME è lo stesso algoritmo del metodo statico
		//getBlockMealTicketReceivedIntoInterval della classe
		//d MealTicketRecap. Renderlo generico.
		
		List<BlockMealTicket> blockList = Lists.newArrayList();
		BlockMealTicket currentBlock = null;

		for(MealTicket mealTicket : mealTicketList) {

			if(currentBlock == null) {
				currentBlock = new BlockMealTicket(mealTicket.block);
				currentBlock.mealTickets.add(mealTicket);
				continue;
			}

			if(currentBlock.codeBlock.equals( mealTicket.block )) {
				currentBlock.mealTickets.add(mealTicket);
				continue;
			}

			blockList.add(currentBlock);
			currentBlock = new BlockMealTicket(mealTicket.block);
			currentBlock.mealTickets.add(mealTicket);

		}
		if(currentBlock != null) {
			blockList.add(currentBlock);
		}	
		return blockList;
		
	}

}
