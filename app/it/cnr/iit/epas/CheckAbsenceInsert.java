package it.cnr.iit.epas;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class CheckAbsenceInsert {

	public int totalAbsenceInsert;
	public List<LocalDate> dateInTrouble = new ArrayList<LocalDate>();
	public String message;
	public boolean insertInShiftOrReperibility;
	public int howManyAbsenceInReperibilityOrShift;
	
	public CheckAbsenceInsert(int quantity, String message, boolean insertInShiftOrReperibility, int howManyAbsenceInReperibilityOrShift){
		this.totalAbsenceInsert = quantity;
		this.message = message;
		this.insertInShiftOrReperibility = insertInShiftOrReperibility;
		this.howManyAbsenceInReperibilityOrShift = howManyAbsenceInReperibilityOrShift;
	}
}
