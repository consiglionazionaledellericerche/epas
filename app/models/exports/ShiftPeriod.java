package models.exports;

import models.Person;
import models.ShiftTimeTable;
import models.ShiftType;

import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative
 * ai turni delle persone
 * 
 * @author dario, arianna
 *
 */
public class ShiftPeriod {

	public Person person;
	public final LocalDate start;
	public LocalDate end;
	public final ShiftType shiftType;
	public final boolean cancelled;
	public ShiftTimeTable shiftTimeTable;
	
	public ShiftPeriod(Person person, LocalDate start, LocalDate end, ShiftType shiftType, boolean cancelled, ShiftTimeTable shiftTimeTable){
		this.person = person;
		this.start = start;
		this.end = end;
		this.cancelled = cancelled;
		this.shiftType = shiftType;
		this.shiftTimeTable = shiftTimeTable;
	}
	
	public ShiftPeriod(Person person, LocalDate start, ShiftType shiftType, boolean cancelled, ShiftTimeTable shiftTimeTable){
		this.person = person;
		this.start = start;
		this.shiftType = shiftType;
		this.cancelled = cancelled;
		this.shiftTimeTable = shiftTimeTable;
	}
	
	// for cancelled shift
	public ShiftPeriod(LocalDate start, LocalDate end, ShiftType shiftType, boolean cancelled){
		this.start = start;
		this.end = end;
		this.shiftType = shiftType;
		this.cancelled = cancelled;
	}
}
