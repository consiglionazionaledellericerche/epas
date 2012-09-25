/**
 * 
 */
package models.enumerate;

/**
 * Tipologie di tempo di lavoro giustificato
 * Utilizzate nelle AbsenceType per modellare le diverse
 * tipologie. 
 * 
 * @author cristian
 * @author dario
 */
public enum JustifiedTimeAtWork {
	AllDay("Tutto il giorno", null),
	HalfDay("Mezza giornata", null),
	OneHour("1 ora", 60),
	TwoHours("2 ore", 120),
	ThreeHours("3 ore", 180),
	FourHours("4 ore", 240),
	FiveHours("5 ore", 300),
	SixHours("6 ore", 360),	
	SevenHours("7 ore", 420),
	EightHours("8 ore", 480),
	Nothing("Niente", 0),
	TimeToComplete("Quello che manca", null),
	ReduceWorkingTimeOfTwoHours("Riduce orario di lavoro di 2 ore", null);
	
	public String description;
	public Integer minutesJustified;
	
	private JustifiedTimeAtWork(String description, Integer minutesJustified) {
		this.description = description;
		this.minutesJustified = minutesJustified;
	}
	
	public boolean isFixedJustifiedTime() {
		return minutesJustified != null;
	}

	
}
