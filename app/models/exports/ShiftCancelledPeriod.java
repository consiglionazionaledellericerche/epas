package models.exports;

import models.ShiftType;
import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative
 * ai turni delle persone: Turni cancellati
 * 
 * @author  arianna
 *
 */
public class ShiftCancelledPeriod {

	public final LocalDate start;
	public LocalDate end;
	public final ShiftType shiftType;
	
	public ShiftCancelledPeriod(LocalDate start, LocalDate end, ShiftType shiftType){
		this.start = start;
		this.end = end;
		this.shiftType = shiftType;
	}
	
	public ShiftCancelledPeriod(LocalDate start, ShiftType shiftType){
		this.start = start;
		this.shiftType = shiftType;
	}
	
}
