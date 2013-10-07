package it.cnr.iit.epas;

import org.joda.time.LocalDate;

public class DateInterval {
	
	private LocalDate begin;
	private LocalDate end;
	
	public DateInterval(LocalDate date1, LocalDate date2)
	{
		if(date1==null)
			date1 = DateUtility.setInfinity();
		if(date2==null)
			date2 = DateUtility.setInfinity();
		
		if(date1.isAfter(date2))
		{
			this.begin = date2;
			this.end = date1;
		}
		else
		{
			this.begin = date1;
			this.end = date2;
		}
	}
	
	public LocalDate getBegin()
	{
		return begin;
	}
	
	public LocalDate getEnd()
	{
		return end;
	}
	
	
	
	
}
