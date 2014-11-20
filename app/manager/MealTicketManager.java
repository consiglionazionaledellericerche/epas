package manager;

import it.cnr.iit.epas.DateInterval;

import java.util.List;





import org.joda.time.LocalDate;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Contract;
import models.MealTicket;
import models.PersonDay;
import play.Logger;

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
	 * @return la lista di MealTicket appartenenti al blocco.
	 */
	public static List<MealTicket> buildBlockMealTicket(Integer codeBlock, Integer dimBlock) {

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
	
	
	/**
	 * Verifica che nel contratto precedente a contract siano avanzati dei buoni
	 * pasto assegnati. In tal caso per quei buoni pasto viene modificata la relazione
	 * col contratto successivo e cambiata la data di attribuzione in modo che ricada 
	 * all'inizio del nuovo contratto
	 * @param contract
	 */
	public static void mealTicketsLegacy(Contract contract) {
		
		Contract previousContract = PersonDao.getPreviousPersonContract(contract);
		if(previousContract == null)
			return;
		
		DateInterval previousContractInterval = previousContract.getContractDateInterval();
		
		//Data inizio utilizzo mealticket
		PersonResidualYearRecap c = 
				PersonResidualYearRecap
				.factory(previousContract, previousContractInterval.getEnd().getYear(), null);
		PersonResidualMonthRecap monthRecap = c.getMese(previousContractInterval.getEnd().getMonthOfYear());
		
		if(monthRecap == null)
			return;
		
		if(monthRecap.buoniPastoResidui == 0)
			return;
		
		List<MealTicket> contractMealTicketsDesc = MealTicketDao.getOrderedMealTicketInContract(previousContract);
		for(int i = 0; i<monthRecap.buoniPastoResidui; i++) {

			MealTicket ticketToChange = contractMealTicketsDesc.get(i);
			ticketToChange.contract = contract;
			ticketToChange.date = contract.beginContract;
			ticketToChange.save();
		}

		return;
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
		private int remaining = 0;
		
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
			
			LocalDate startMealTicket = MealTicketDao.getMealTicketStartDate(contract.person.office);
			if(startMealTicket == null)
				return null;

			DateInterval contractDatabaseInterval = contract.getContractDatabaseDateInterval();
			if(contractDatabaseInterval.getBegin().isBefore(startMealTicket)) 
			{
				contractDatabaseInterval = new DateInterval(
						startMealTicket, contractDatabaseInterval.getEnd());
			}

			List<PersonDay> orderedPersonDayWithMealTicketIntoInterval = 
					PersonDao.getPersonDayIntoInterval(contract.person, contractDatabaseInterval, true);

			List<MealTicket> orderedMealTicketAvailableIntoInterval =
					MealTicketDao.getMealTicketAssignedToPersonIntoInterval(contract, contractDatabaseInterval);

			//MAPPING
			for(int i = 0; i < orderedPersonDayWithMealTicketIntoInterval.size(); i++)
			{
				PersonDay currentPersonDay = orderedPersonDayWithMealTicketIntoInterval.get(i);

				if(orderedMealTicketAvailableIntoInterval.size() == i )
				{
					recap.dateErrore = currentPersonDay.date;
					recap.error = MESSAGE_MEAL_TICKET_RUN_OUT;
					return recap;
				}

				MealTicket currentMealTicket = orderedMealTicketAvailableIntoInterval.get(i);
				
				if(currentPersonDay.date.isAfter(currentMealTicket.expireDate)) 
				{
					recap.dateErrore = currentPersonDay.date;
					recap.error = MESSAGE_MEAL_TICKET_EXPIRED;
					return recap;
				}
				
				currentPersonDay.mealTicketAssigned = currentMealTicket;

			}

			recap.remaining = orderedMealTicketAvailableIntoInterval.size() 
					- orderedPersonDayWithMealTicketIntoInterval.size();
			
			return recap;
		}
		
		public Contract getContract() { return this.contract; }
		
		public boolean isMealTicketRunOut() { 
			return this.error == MealTicketRecap.MESSAGE_MEAL_TICKET_RUN_OUT; 
		}
		public boolean isMealTicketExpired() { 
			return this.error == MealTicketRecap.MESSAGE_MEAL_TICKET_EXPIRED; 
		}
		public LocalDate getDateError() { return this.dateErrore; }
		
		public List<PersonDay> getPersonDayMapped() { return this.personDaysMealTickets; }
		public int getRemainig() { return this.remaining; }
	
	}

}
