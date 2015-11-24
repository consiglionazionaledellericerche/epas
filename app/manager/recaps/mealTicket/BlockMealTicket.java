package manager.recaps.mealTicket;

import com.google.common.collect.Lists;
import models.MealTicket;
import models.Person;
import org.joda.time.LocalDate;

import java.util.List;

public class BlockMealTicket {
	
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

}