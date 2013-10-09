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

	
	public static JustifiedTimeAtWork getByDescription(String description){
		if(description.equals("Tutto il giorno"))
			return JustifiedTimeAtWork.AllDay;
		if(description.equals("Mezza giornata"))
			return JustifiedTimeAtWork.HalfDay;
		if(description.equals("1 ora"))
			return JustifiedTimeAtWork.OneHour;
		if(description.equals("2 ore"))
			return JustifiedTimeAtWork.TwoHours;
		if(description.equals("3 ore"))
			return JustifiedTimeAtWork.ThreeHours;
		if(description.equals("4 ore"))
			return JustifiedTimeAtWork.FourHours;
		if(description.equals("5 ore"))
			return JustifiedTimeAtWork.FiveHours;
		if(description.equals("6 ore"))
			return JustifiedTimeAtWork.SixHours;
		if(description.equals("7 ore"))
			return JustifiedTimeAtWork.SevenHours;
		if(description.equals("8 ore"))
			return JustifiedTimeAtWork.EightHours;
		if(description.equals("Niente"))
			return JustifiedTimeAtWork.Nothing;
		if(description.equals("Quello che manca"))
			return JustifiedTimeAtWork.TimeToComplete;
		if(description.equals("Riduce orario di lavoro di 2 ore"))
			return JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours;
		return null;
		
	}
	
}
