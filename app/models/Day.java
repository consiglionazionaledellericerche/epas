package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Day {

	private boolean mealTicket;
	private List<Calendar> date;
	private String absenceCode;
	
	private String workingTimeType;
		
	
	public List<Day> dayList(long id){
		List<Day> listaGiorni = new ArrayList<Day>();
		return listaGiorni;
		
	}
	
	public int mealTicketToUse(){
		return 0;
		
	}
	
	public int mealTicketToReturn(){
		return 0;
	}
	
	public int officeWorkingDay(){
		return 0;
	}
}
