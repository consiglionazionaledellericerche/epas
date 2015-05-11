package models.rendering;

import it.cnr.iit.epas.DateUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;

/**
 * 
 * @author alessandro
 * Classe da utilizzare per il rendering delle assenze annuali effettuate da una persona in un anno
 */
public class YearlyAbsencesRecap {

	@Inject
	private AbsenceDao absenceDao;

	public Person person;
	public short year;
	Table<Integer, Integer, String> absenceTable;
	public Map<AbsenceType,Integer> absenceSummary = new HashMap<AbsenceType,Integer>();
	public int totalAbsence = 0;

	public YearlyAbsencesRecap(Person person, short year)
	{
		this.person = person;
		this.year = year;

		List<Absence> yearlyAbsence = getYearlyAbsence(this.person,this.year);

		this.totalAbsence = yearlyAbsence.size();
		this.absenceTable = buildYearlyAbsenceTable(yearlyAbsence);
		this.absenceSummary = buildYearlyAbsenceSummary(yearlyAbsence);		

	}

	/**
	 * 
	 * @param person
	 * @param year
	 * @return la lista delle assenze effettuate dalla persona nell'anno
	 */
	public List<Absence> getYearlyAbsence(Person person, int year){

		List<Absence> yearlyAbsence = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), Optional.<String>absent(), 
				new LocalDate(year,1,1), new LocalDate(year,12,31), Optional.<JustifiedTimeAtWork>absent(), false, true);

		return yearlyAbsence;
	}

	/**
	 * 
	 * @param monthNumber
	 * @return il nome del mese con valore monthNumber
	 * 			null in caso di argomento non valido 
	 */
	public String fromIntToStringMonth(Integer monthNumber)
	{
		return DateUtility.fromIntToStringMonth(monthNumber);


	}

	/**
	 * 
	 * @param yearlyAbsenceList
	 * @return la tabella contenente in ogni cella i codici delle assenze effettuate in quel giorno  
	 */
	private Table<Integer, Integer, String> buildYearlyAbsenceTable(List<Absence> yearlyAbsenceList)
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
			if(value.equals(""))
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
	private Map<AbsenceType,Integer> buildYearlyAbsenceSummary(List<Absence> yearlyAbsence){

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
