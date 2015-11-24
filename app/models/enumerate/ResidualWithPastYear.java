package models.enumerate;

/**
 * 
 * @author dario
 *
 */
public enum ResidualWithPastYear {
	
	atMonth("al mese"),
	atDay("al giorno"),
	atMonthInWhichCanUse("nel mese in cui posso usarla");
	
	public String description;
	
	private ResidualWithPastYear(String description){
		this.description = description;
	}

}
