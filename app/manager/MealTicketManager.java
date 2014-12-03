package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import org.joda.time.LocalDate;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Contract;
import models.MealTicket;
import models.Person;
import models.PersonDay;

import com.google.common.collect.Lists;

import dao.MealTicketDao;
import dao.PersonDao;

/**
 * Manager per MealTicket
 * @author alessandro
 *
 */
public class MealTicketManager {
	
	
	/**
	 * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock
	 * @param codeBlock il codice del blocco di meal ticket
	 * @param dimBlock la dimensione del blocco di meal ticket
	 * @param expireDate la data di scadenza dei buoni nel blocco
	 * @return la lista di MealTicket appartenenti al blocco.
	 */
	public static List<MealTicket> buildBlockMealTicket(
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

		return  mealTicketList;
	}
	
	
	/**
	 * Verifica che nel contratto precedente a contract siano avanzati dei buoni
	 * pasto assegnati. In tal caso per quei buoni pasto viene modificata la relazione
	 * col contratto successivo e cambiata la data di attribuzione in modo che ricada 
	 * all'inizio del nuovo contratto
	 * @param contract
	 */
	
	/**
	 * Verifica che nel contratto precedente a contract siano avanzati dei buoni
	 * pasto assegnati. In tal caso per quei buoni pasto viene modificata la relazione
	 * col contratto successivo e cambiata la data di attribuzione in modo che ricada 
	 * all'inizio del nuovo contratto.
	 * Ritorna il numero di buoni pasto trasferiti fra un contratto e l'altro.
	 * @param contract
	 * @return
	 */
	public static int mealTicketsLegacy(Contract contract) {
		
		Contract previousContract = PersonDao.getPreviousPersonContract(contract);
		if(previousContract == null)
			return 0;
		
		DateInterval previousContractInterval = previousContract.getContractDateInterval();
		
		//Data inizio utilizzo mealticket
		PersonResidualYearRecap c = 
				PersonResidualYearRecap
				.factory(previousContract, previousContractInterval.getEnd().getYear(), null);
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
	 * 
	 * @author alessandro
	 *
	 */
	public static final class MealTicketRecap {
		
		public static final int MESSAGE_MEAL_TICKET_EXPIRED = -1;
		public static final int MESSAGE_MEAL_TICKET_RUN_OUT = -2;
		
		private final Contract contract;
		private int error = 0;
		private LocalDate dateErrore = null;
		private List<PersonDay> personDaysMealTickets = Lists.newArrayList();
		private List<MealTicket> mealTicketsReceivedOrdered = Lists.newArrayList();
				
		private int remaining = 0;
		
		private DateInterval mealTicketInterval = null;
		
		private MealTicketRecap(Contract contract) {
			this.contract = contract;
		}
		
		/**
		 * Effettua il mapping ottimo fra i giorni in cui la persona ha maturato il diritto al buono pasto
		 * ed il buono pasto, selezionando quello con scadenza più imminente rimasto in quella data.
		 * In questo modo è possibile intercettare i seguenti malfunzionalmenti.
		 * 1) MEAL_TICKET_RUN_OUT in error finisce la data in cui sono esauriti i buoni pasto
		 * 2) MEAL_TICKET_EXPIRED in error finisce la data in cui si inizia a consumare buoni pasto scaduti
		 * Se non ci sono errori viene salvato il numero di buoni pasto rimanenti
		 * @param contract
		 * @return 
		 */
		public static MealTicketRecap build(Contract contract) {
			
			MealTicketRecap recap =  new MealTicketRecap(contract);
			
			recap.mealTicketInterval = contract.getContractMealTicketDateInterval();
			
			if(recap.mealTicketInterval == null)
				return null;
			
			recap.personDaysMealTickets = 
					PersonDao.getPersonDayIntoInterval(contract.person, recap.mealTicketInterval, true);

			recap.mealTicketsReceivedOrdered =
					MealTicketDao.getMealTicketAssignedToPersonIntoInterval(contract, recap.mealTicketInterval);

			//MAPPING
			//init lazy variable
			for(MealTicket mealTicket : recap.mealTicketsReceivedOrdered) {
				mealTicket.used = false;
			}
			//mapping
			for(int i = 0; i < recap.personDaysMealTickets.size(); i++)
			{
				PersonDay currentPersonDay = recap.personDaysMealTickets.get(i);

				if(recap.mealTicketsReceivedOrdered.size() == i )
				{
					recap.dateErrore = currentPersonDay.date;
					recap.error = MESSAGE_MEAL_TICKET_RUN_OUT;
					return recap;
				}

				MealTicket currentMealTicket = recap.mealTicketsReceivedOrdered.get(i);
				
				if(currentPersonDay.date.isAfter(currentMealTicket.expireDate)) 
				{
					recap.dateErrore = currentPersonDay.date;
					recap.error = MESSAGE_MEAL_TICKET_EXPIRED;
					return recap;
				}
				
				currentPersonDay.mealTicketAssigned = currentMealTicket;
				currentMealTicket.used = true;

			}

			recap.remaining =recap.mealTicketsReceivedOrdered.size() 
					- recap.personDaysMealTickets.size();
			
			return recap;
		}
		
		public Contract getContract() { return this.contract; }
		
		public DateInterval getMealTicketInterval() { return this.mealTicketInterval; }
		
		public boolean isMealTicketRunOut() { 
			return this.error == MealTicketRecap.MESSAGE_MEAL_TICKET_RUN_OUT; 
		}
		public boolean isMealTicketExpired() { 
			return this.error == MealTicketRecap.MESSAGE_MEAL_TICKET_EXPIRED; 
		}
		public LocalDate getDateError() { return this.dateErrore; }
		
		public List<PersonDay> getPersonDayMapped() { 
			return this.personDaysMealTickets; 
		}
		public List<MealTicket> getMealTicketsReceivedOrdered() { 
			return this.mealTicketsReceivedOrdered; 
		}
		public int getRemaining() { return this.remaining; }
		
		/**
		 * Ritorna i blocchi di buoni pasto consegnati alla persona nell anno year 
		 * ordinati per data di scadenza e per codice blocco.
		 * @param year
		 * @return
		 */
		public List<BlockMealTicket> getBlockMealTicketReceivedInYear(Integer year) {
			
			DateInterval yearInterval = new DateInterval( new LocalDate(year,1,1), new LocalDate(year,12,31));

			return MealTicketRecap.getBlockMealTicketReceivedIntoInterval(this, yearInterval);
		}
		
		/**
		 * Ritorna i blocchi di buoni pasto consegnati alla persona nell anno year 
		 * ordinati per data di scadenza e per codice blocco.
		 * @param year
		 * @return
		 */
		public List<BlockMealTicket> getBlockMealTicketReceived() {
			
			List<BlockMealTicket> blockList = MealTicketRecap.getBlockMealTicketReceivedIntoInterval(this, this.mealTicketInterval);
			return blockList;
		}

		
		/**
		 * Ritorna i blocchi inerenti la lista di buoni pasto recap.mealTicketsReceivedOrdered,
		 * consegnati nell'intervallo temporale indicato. N.B. la lista di di cui sopra è ordinata
		 * per data di scadenza e per codice blocco in ordine ascendente.
		 * @param recap
		 * @param interval
		 * @return
		 */
		private static List<BlockMealTicket> getBlockMealTicketReceivedIntoInterval(
				MealTicketRecap recap, DateInterval interval) {
			
			List<BlockMealTicket> blockList = Lists.newArrayList();
			BlockMealTicket currentBlock = null;
						
			for(MealTicket mealTicket : recap.mealTicketsReceivedOrdered) {
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
	}
	
	public static final class BlockMealTicket {
		
		public Integer codeBlock;
		public List<MealTicket> mealTickets;
		
		public BlockMealTicket(Integer codeBlock) {
			
			this.codeBlock = codeBlock;
			this.mealTickets = Lists.newArrayList();
		}
		
		/**
		 * La dimensione del blocchetto.
		 * @return
		 */
		public Integer getDimBlock() {
			return this.mealTickets.size();
		}
		
		/**
		 * Il numero di buoni rimanenti all'interno del blocchetto.
		 * N.B. Il metodo ritorna un valore valido solo se la variabile 
		 * lazy used è valorizzata per tutti i buoni pasto del blocchetto.
		 * (Per valorizzarla occorre passare dalla MealTicketRecap.build())
		 * @return
		 */
		public Integer getRemaining() {
			Integer consumed = this.getConsumed();
			if(consumed == null)
				return null;
			return this.getDimBlock() - this.getConsumed();
		}
		
		/**
		 * Il numero di buoni consumati all'interno del blocchetto.
		 * N.B. Il metodo ritorna un valore valido solo se la variabile 
		 * lazy used è valorizzata per tutti i buoni pasto del blocchetto.
		 * (Per valorizzarla occorre passare dalla MealTicketRecap.build())
		 * @return
		 */
		public Integer getConsumed() {
			Integer count = 0;
			for(MealTicket mealTicket : this.mealTickets) {
				if(mealTicket.used == null)
					return null;
				if(mealTicket.used)
					count++;
			}
			return count;
		}
		
		/**
		 * Il giorno di scadenza dei buoni appartenenti al blocchetto.
		 * @return
		 */
		public LocalDate getExpireDate() {
			if(this.getDimBlock()>0)
				return this.mealTickets.get(0).expireDate;
			return null;
		}
		
		/**
		 * L'amministratore assegnatario del blocchetto.
		 * @return
		 */
		public Person getAdmin() {
			if(this.getDimBlock()>0)
				return this.mealTickets.get(0).admin;
			return null;
		}
		
		/**
		 * La data di consegna (inserimento ePAS) del blocchetto.
		 * @return
		 */
		public LocalDate getReceivedDate() {
			if(this.getDimBlock()>0)
				return this.mealTickets.get(0).date;
			return null;
		}
		
		/**
		 * 
		 * @param mealTicket
		 * @return
		 */
		public static List<BlockMealTicket> getBlockMealTicketFromMealTicketList(
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

}
