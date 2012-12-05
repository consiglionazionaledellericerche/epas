package models.exports;

import models.Person;
import models.ShiftTimeTable;
import models.ShiftType;

import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative
 * ai turni delle persone
 * 
 * @author dario
 *
 */
public class ShiftPeriod {

	public final Person person;
	public final LocalDate dateStart;
	public LocalDate dateEnd;
	public final ShiftType shiftType;
	public final ShiftTimeTable shiftTimeTable;
	
	public ShiftPeriod(Person person, LocalDate dateStart, LocalDate dateEnd, ShiftType shiftType, ShiftTimeTable shiftTimeTable){
		this.person = person;
		this.dateStart = dateStart;
		this.dateEnd = dateEnd;
		this.shiftType = shiftType;
		this.shiftTimeTable = shiftTimeTable;
	}
	
	public ShiftPeriod(Person person, LocalDate dateStart, ShiftType shiftType, ShiftTimeTable shiftTimeTable){
		this.person = person;
		this.dateStart = dateStart;
		this.shiftType = shiftType;
		this.shiftTimeTable = shiftTimeTable;
	}
	
}
