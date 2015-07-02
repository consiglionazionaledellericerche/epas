package manager.recaps.mealTicket;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import manager.MealTicketManager;
import models.Contract;
import models.MealTicket;
import models.PersonDay;

import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import dao.MealTicketDao;
import dao.PersonDao;

/**
 * 
 * @author alessandro
 *
 */
public class MealTicketRecap {

	private MealTicketManager mealTicketManager;

	private final Contract contract;
	
	private LocalDate dateExpire = null;
	private LocalDate dateRunOut = null;
	
	private List<PersonDay> personDaysMealTickets = Lists.newArrayList();
	private List<MealTicket> mealTicketsReceivedOrdered = Lists.newArrayList();

	private int remaining = 0;

	private DateInterval mealTicketInterval = null;

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
	public MealTicketRecap(MealTicketManager mealTicketManager,PersonDao personDao,
			MealTicketDao mealTicketDao, Contract contract, DateInterval dateInterval) {

		this.mealTicketManager = mealTicketManager;
		this.contract = contract;
		
		this.mealTicketInterval = dateInterval;
		
		this.personDaysMealTickets = personDao.getPersonDayIntoInterval(
				contract.person, this.mealTicketInterval, true);

		this.mealTicketsReceivedOrdered = mealTicketDao
				.getMealTicketAssignedToPersonIntoInterval(contract, this.mealTicketInterval);

		//MAPPING
		//init lazy variable
		for(MealTicket mealTicket : this.mealTicketsReceivedOrdered) {
			mealTicket.used = false;
		}
		//mapping
		for(int i = 0; i < this.personDaysMealTickets.size(); i++)
		{
			PersonDay currentPersonDay = this.personDaysMealTickets.get(i);

			if(this.mealTicketsReceivedOrdered.size() == i )
			{
				this.dateRunOut = currentPersonDay.date;
				continue;
			}

			MealTicket currentMealTicket = this.mealTicketsReceivedOrdered.get(i);

			if(currentPersonDay.date.isAfter(currentMealTicket.expireDate)) 
			{
				this.dateExpire = currentPersonDay.date;
				//continue;
				//return;
			}

			currentPersonDay.mealTicketAssigned = currentMealTicket;
			currentMealTicket.used = true;

		}

		this.remaining = this.mealTicketsReceivedOrdered.size() 
				- this.personDaysMealTickets.size();

		return;
	}

	public Contract getContract() { return this.contract; }

	public DateInterval getMealTicketInterval() { return this.mealTicketInterval; }

	public boolean isMealTicketRunOut() { 
		return this.dateRunOut != null; 
	}
	public boolean isMealTicketExpired() { 
		return this.dateExpire != null; 
	}
	public LocalDate getDateExpire() {
		return this.dateExpire;
	}
	public LocalDate getDateRunOut() {
		return this.dateRunOut;
	}
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

		return mealTicketManager.getBlockMealTicketReceivedIntoInterval(this, yearInterval);
	}

	/**
	 * Ritorna i blocchi di buoni pasto consegnati alla persona nell'intero intervallo del recap,
	 * ordinati per data di scadenza e per codice blocco.
	 * @return
	 */
	public List<BlockMealTicket> getBlockMealTicketReceived() {

		List<BlockMealTicket> blockList = 
				mealTicketManager.getBlockMealTicketReceivedIntoInterval(this, this.mealTicketInterval);
		return blockList;
	}

}