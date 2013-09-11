package controllers.rendering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.joda.time.LocalDate;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.TreeBasedTable;

import play.Logger;
import play.data.validation.Valid;
import models.Absence;
import models.AbsenceType;
import models.Person;
import models.PersonDay;

public class YearlyAbsencesPerPerson {

	//variabili necessarie al rendering
	public Person person;
	public short year;
	Table<Integer, Integer, String> absenceTable;
	public Map<AbsenceType,Integer> absenceSummary = new HashMap<AbsenceType,Integer>();
	public int totalAbsence = 0;

	//costruttore
	public YearlyAbsencesPerPerson(Person person, short year)
	{
		this.person = person;
		this.year = year;
		
		
		//Select lista di assenze nell'anno per la persona, ordinate per data
		List<Absence> yearlyAbsence = 
				Absence.find( "SELECT abs "
							+ "FROM Absence abs "
							+ "WHERE abs.personDay.person = ? AND abs.personDay.date BETWEEN ? AND ? "
							+ "ORDER BY abs.personDay.date", 
								person, 
								new LocalDate(year,1,1), 
								new LocalDate(year,12,31)
							).fetch(); 
		
		
		this.totalAbsence = yearlyAbsence.size();
		this.absenceTable = buildYearlyAbsenceTable(yearlyAbsence);
		this.absenceSummary = buildYearlyAbsenceSummary(yearlyAbsence);		

	}
	
	//metodi richiamati nel template
	/**
	 * 
	 * @param monthNumber
	 * @return il nome del mese con valore monthNumber
	 * 			null in caso di argomento non valido 
	 */
	public String fromIntToStringMonth(Integer monthNumber)
	{
		LocalDate date = new LocalDate().withMonthOfYear(monthNumber);
		return date.monthOfYear().getAsText();
	}
	
	
	
	// metodi di supporto
	/**
	 * 
	 * @param yearlyAbsenceList
	 * @return la tabella contenente in ogni cella i codici delle assenze effettuate in quel giorno  
	 */
	private static Table<Integer, Integer, String> buildYearlyAbsenceTable(List<Absence> yearlyAbsenceList)
	{
		Table<Integer, Integer, String> table = TreeBasedTable.create();
	
		//dimensionamento tabella 12 righe e 31 colonne
		for(int month=1; month<=12; month++)
		{
			for(int day=1; day<=31; day++)
			{
				table.put(month, day, "");
			}
		}
		
		//inserimento valori
		for(Absence abs: yearlyAbsenceList)
		{
			int absMonth = abs.personDay.date.getMonthOfYear();
			int absDay =  abs.personDay.date.getDayOfMonth();
		
			String value = table.get(absMonth, absDay);
			if(value=="")
			{
				table.put(absMonth, absDay, abs.absenceType.code);
			}
			else
			{
				//se e' gia' presente un valore ritorno a capo
				table.put(absMonth, absDay, value + '\n' + abs.absenceType.code);
			}
		}
		
		return table;

	}

	/**
	 * 
	 * @param yearlyAbsence
	 * @return la mappa contenente i tipi di assenza effettuate nell'anno con il relativo numero di occorrenze
	 */
	private static Map<AbsenceType,Integer> buildYearlyAbsenceSummary(List<Absence> yearlyAbsence){
		
		Map<AbsenceType,Integer> mappa = new HashMap<AbsenceType,Integer>();			//mappa che conterra' le entry (tipo assenza, numero occorrenze)
	
		Integer i = 0;
		for(Absence abs : yearlyAbsence){
			boolean stato = mappa.containsKey(abs.absenceType);
			if(stato==false){
				i=1;
				mappa.put(abs.absenceType, i);
			}
			else{
				i = mappa.get(abs.absenceType);
				mappa.remove(abs.absenceType);
				mappa.put(abs.absenceType, i+1);
			}
		}
		return mappa;
	}

	
	
	
}
