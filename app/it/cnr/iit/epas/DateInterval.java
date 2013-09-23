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
	
	
	
	/**
	 * 
	 * @param beginInterval
	 * @param endInterval (null rappresenta intervallo infinito)
	 * @param beginAnother 
	 * @param endAnother (null rappresenta intervallo infinito)
	 * @return true se l'intervallo [beginInterval,endInterval] e' contenuto nell'intervallo [beginAnother, endAnother] estremi di Another compresi
	 */
	public static boolean isIntervalIntoAnother(LocalDate beginInterval, LocalDate endInterval, LocalDate beginAnother, LocalDate endAnother)
	{
		if(endInterval==null)
			endInterval = DateUtility.setInfinity();
		if(endAnother==null)
			endAnother = DateUtility.setInfinity();
		
		if(beginInterval.isBefore(beginAnother) || endInterval.isAfter(endAnother))
		{
			return false;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param beginFirst
	 * @param endFirst (null rappresenta intervallo infinito)
	 * @param beginSecond
	 * @param endSecond (null rappresenta intervallo infinito)
	 * @return true se l'intervallo [beginFirst,endFirst] e' consecutivo a [beginSecond,endSecond] (cioè il secondo intervallo inizia 
	 * il giorno successivo della fine del primo intervallo.
	 */
	public static boolean areContigueInterval(LocalDate beginFirst, LocalDate endFirst, LocalDate beginSecond, LocalDate endSecond)
	{
		if(endFirst==null)
			endFirst = DateUtility.setInfinity();

		if( ! isIntervalBeforeAnother(beginFirst,endFirst,beginSecond,endSecond) )
		{
			return false;
		}

		if(beginSecond.isEqual(endFirst.plusDays(1)))
		{
			return true;
		}
		return false;	
	}
	
	
	/**
	 * 
	 * @param beginInterval
	 * @param endInterval (null rappresenta intervallo infinito)
	 * @param beginAnother
	 * @param endAnother (null rappresenta intervallo infinito)
	 * @return true se l'intervallo [beginInterval,endInterval] e' precedente all'intervallo [beginAnother,endAnother] con intersezione vuota
	 */
	public static boolean isIntervalBeforeAnother(LocalDate beginInterval, LocalDate endInterval, LocalDate beginAnother, LocalDate endAnother)
	{
		if(endInterval==null)
			endInterval = DateUtility.setInfinity();
		if(endInterval.isAfter(beginAnother))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param beginFirst
	 * @param endFirst (null rappresenta intervallo infinito)
	 * @param beginSecond
	 * @param endSecond (null rappresenta intervallo infinito)
	 * @return true se l'intersezione fra i due intervalli [beginFirst,endFirst] e [beginSecond,endSecond] non e' vuota. (richiede che beginFirst < beginSecond)
	 * l'intersezione se esiste e' l'intervallo [beginSecond,endFirst]
	 */
	public static boolean areIntervalIntersected(LocalDate beginFirst, LocalDate endFirst, LocalDate beginSecond, LocalDate endSecond)
	{
		if(endFirst==null)
			endFirst = DateUtility.setInfinity();
		if(endSecond==null)
			endSecond = DateUtility.setInfinity();

		if(isIntervalBeforeAnother(beginFirst,endFirst,beginSecond,endSecond))
			return false;
		
		if(areContigueInterval(beginFirst,endFirst,beginSecond,endSecond))
			return false;
		
		return true;
	}
	
	/**
	 * 
	 * @param begin
	 * @param end (null rappresenta intervallo infinito)
	 * @return true se l'intervallo [begin,end] e' ben formato
	 */
	public static boolean isInterval(LocalDate begin, LocalDate end)
	{
		if(end==null)
			end = DateUtility.setInfinity();
		
		if(begin.isAfter(end))
			return false;
		
		return true;
	}
	
	
	
	
}
