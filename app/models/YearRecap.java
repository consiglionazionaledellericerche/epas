package models;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.joda.time.LocalDate;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;

import play.Logger;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 * 
 * Per adesso la classe Year recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista.
 */
@Entity
@Table(name = "year_recaps")
public class YearRecap extends Model{
	
	private static final long serialVersionUID = -5721503493068567394L;

	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Column
	public short year;
	@Column
	public int remaining;
	@Column
	public int remainingAp;
	@Column
	public int recg;
	@Column
	public int recgap;
	@Column
	public int overtime;
	@Column
	public int overtimeAp;
	@Column
	public int recguap;
	@Column
	public int recm;
	@Column
	public Timestamp lastModified;
	@Transient
	private boolean persistent = false;
	@Transient
	private List<String> months = null;
		
	
	protected YearRecap(){
		
		
	}
	/**
	 * Construttore di default con i parametri obbligatori
	 * 
	 * @param person la persona associata al riepilogo annuale
	 * @param year l'anno di riferimento
	 * 
	 */
	public YearRecap(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		this.person = person;
		this.year = year;		
		
	}
	
	/**
	 * Preleva dallo storage le il YearRecap relativo ai dati passati.
	 * Se il yearRecap non è presente sul db ritorna un'istanza vuota
	 * ma con associati i dati passati.
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @return il riepilogo annuale, se non è presente nello storage viene 
	 * 	restituito un riepilogo annuale vuoto
	 */
	public static YearRecap byPersonAndYear(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		if (person == null) {
			throw new IllegalArgumentException("Person mandatory");
		}
		YearRecap yearRecap = YearRecap.find("byPersonAndYear", person, year).first();
		if (yearRecap == null) {
			return new YearRecap(person, year);
		}
		yearRecap.persistent  = true;
		return yearRecap;
	}
	
	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<String> getMonths() {

		if (months != null) {
			return months;
		}
		months = new ArrayList<String>();
		LocalDate firstMonthOfYear = new LocalDate(year, 1,1);
		
		for(int month = 1; month <= firstMonthOfYear.getMonthOfYear(); month++){
			String mese = firstMonthOfYear.monthOfYear().getAsText();
			months.add(mese);
			firstMonthOfYear=firstMonthOfYear.plusMonths(1);
		}
		return months;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return massimo numero di giorni in un mese per coadiuvare nella realizzazione della tabella per le assenze annuali
	 */
	public int maxNumberOfDays(int year, String month){
		int max = 0;
		
		if(month.equalsIgnoreCase("aprile") || month.equalsIgnoreCase("giugno") || month.equalsIgnoreCase("settembre")
				|| month.equalsIgnoreCase("novembre"))
			max=30;
		if(month.equalsIgnoreCase("gennaio") || month.equalsIgnoreCase("marzo") || month.equalsIgnoreCase("maggio")
				|| month.equalsIgnoreCase("luglio") || month.equalsIgnoreCase("agosto") || month.equalsIgnoreCase("ottobre")
				|| month.equalsIgnoreCase("dicembre"))
			max=31;
		if(month.equalsIgnoreCase("febbraio") && (year==2008 || year==2012 || year==2016 || year==2020))
			max=29;
		else
			max=28;
		return max;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return la totalità delle assenze per quella persona in un anno
	 */
	public List<Absence> getAbsenceInYear(int year, String month, int day){
		LocalDate date = null;
		List<Absence> absences = null;
		if(absences==null){				
			
			if(month.equalsIgnoreCase("gennaio")){
				date = new LocalDate(year,1,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("febbraio")){
				if(day<30){
					date = new LocalDate(year,2,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();
				}
			}
			if(month.equalsIgnoreCase("marzo")){
				date = new LocalDate(year,3,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("aprile")){
				if(day<31){
					date = new LocalDate(year,4,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("maggio")){
				date = new LocalDate(year,5,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("giugno")){
				if(day<31){
					date = new LocalDate(year,6,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
				}
					
			}
			if(month.equalsIgnoreCase("luglio")){
				date = new LocalDate(year,7,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("agosto")){
				date = new LocalDate(year,8,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("settembre")){
				if(day<31){
					date = new LocalDate(year,9,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("ottobre")){
				date = new LocalDate(year,10,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("novembre")){
				if(day<31){
					date = new LocalDate(year,11,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("dicembre")){
				date = new LocalDate(year,12,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
		}
		
		return absences;
	}

}
