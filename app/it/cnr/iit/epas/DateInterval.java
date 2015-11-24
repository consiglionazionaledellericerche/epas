package it.cnr.iit.epas;

import org.joda.time.LocalDate;

public class DateInterval {

	private LocalDate begin;
	private LocalDate end;

	public DateInterval(LocalDate date1, LocalDate date2){

		if(date1==null)
			date1 = DateUtility.setInfinity();
		if(date2==null)
			date2 = DateUtility.setInfinity();

		//Non applico il riferimento ma costruisco nuovi oggetti
		LocalDate date1Copy = new LocalDate(date1);
		LocalDate date2Copy = new LocalDate(date2);

		if(date1.isAfter(date2)){
			this.begin = date2Copy;
			this.end = date1Copy;
		}
		else{
			this.begin = date1Copy;
			this.end = date2Copy;
		}
	}

	public LocalDate getBegin(){
		return begin;
	}

	public LocalDate getEnd(){
		return end;
	}

	public boolean isClosed(){

		if(DateUtility.isInfinity(this.end))
			return false;
		return true;

	}

	@Override
	public String toString() {
		return "[" + this.begin.toString() + "," + this.getEnd().toString() + "]";
	}




}
