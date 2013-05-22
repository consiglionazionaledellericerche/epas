package models.enumerate;

/**
 * 
 * @author dario
 *
 */
public enum CapacityCompensatoryRestOneThree {
	
	onDayResidual("residuo del giorno"),
	onEndOfMonthResidual("residuo a fine mese"),
	onEndPastMonthResidual("residuo a fine mese precedente"),
	onEndPastQuarterResidual("residuo a trimestre precedente");
	
	public String description;
	
	private CapacityCompensatoryRestOneThree(String description){
		this.description = description;
	}
}
