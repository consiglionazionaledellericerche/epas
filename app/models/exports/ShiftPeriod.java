package models.exports;

import models.Person;
import models.ShiftTimeTable;
import models.ShiftType;
import models.enumerate.ShiftSlot;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

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
	//public ShiftTimeTable shiftTimeTable;
	public ShiftSlot shiftSlot;
	public LocalTime startShift;
	public LocalTime endShift;
	
	public ShiftPeriod(Person person, LocalDate start, LocalDate end, ShiftType shiftType, boolean cancelled, ShiftSlot shiftSlot, LocalTime startShift, LocalTime endShift) {
		this.person = person;
		this.start = start;
		this.end = end;
		this.cancelled = cancelled;
		this.shiftType = shiftType;
		this.shiftSlot = shiftSlot;
		this.startShift = startShift;
		this.endShift = endShift;
	}
	
	public ShiftPeriod(Person person, LocalDate start, ShiftType shiftType, boolean cancelled, ShiftSlot shiftSlot, LocalTime startShift, LocalTime endShift){
		this.person = person;
		this.start = start;
		this.shiftType = shiftType;
		this.cancelled = cancelled;
		this.shiftSlot = shiftSlot;
		this.startShift = startShift;
		this.endShift = endShift;
	}
	
	// for cancelled shift
	public ShiftPeriod(LocalDate start, LocalDate end, ShiftType shiftType, boolean cancelled){
		this.start = start;
		this.end = end;
		this.shiftType = shiftType;
		this.cancelled = cancelled;
	}
}
