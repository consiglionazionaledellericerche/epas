package models;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import net.sf.oval.guard.Guarded;

import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Questa classe modella un riepilogo mensile dei giorni lavorativi e non di una persona.
 * La classe contiene una serie di metodi di utilità per estrarre valore calcolati in funzione
 * delle timbrature ed assenze mensili della persona indicata. 
 * 
 * @author cristian
 * @author dario
 *
 * Per adesso la classe Month recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista sia nella struttura che più banalmente nei nomi dei campi
 * 
 */
@Guarded
@Entity
@Table(name = "month_recaps")
public class MonthRecap extends Model {
	
	private static final long serialVersionUID = -448436166612631217L;

	@Required
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Required
	public int month;

	@Required
	public int year;

	public short workingDays;

	public short daysWorked;

	public short giorniLavorativiLav;

	public int workTime;

	public int remaining;

	public short justifiedAbsence;

	public short vacationAp;

	public short vacationAc;

	public short holidaySop;

	public int recoveries;

	public short recoveriesG;

	public short recoveriesAp;

	public short recoveriesGap;

	public int overtime;

	public Timestamp lastModified;

	public int residualApUsed;

	public int extraTimeAdmin;

	public int additionalHours;

	public boolean nadditionalHours;

	public int residualFine;

	public short endWork;

	public short beginWork;

	public int timeHourVisit;

	public short endRecoveries;

	public int negative;

	public int endNegative;

	public String progressive;
	
	protected boolean persistent = false;
	
	@Transient
	public List<PersonDay> days = null;
	
	/**
	 * Construttore di default con i parametri obbligatori
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @param month il mese di riferimento
	 */
	public MonthRecap(
			@NotNull Person person, 
			@Min(1970) int year, 
			@Range(min=1, max=12) int month) {
		this.person = person;
		this.year = year;
		this.month = month;
	}
	
	/**
	 * Preleva dallo storage le il MonthRecap relativo ai dati passati.
	 * Se il monthRecap non è presente sul db ritorna un'istanza vuota
	 * ma con associati i dati passati.
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @param month il mese di riferimento
	 * @return il riepilogo mensile, se non è presente nello storage viene 
	 * 	restituito un riepilogo mensile vuoto
	 */
	public static MonthRecap byPersonAndYearAndMonth(
			@NotNull Person person, 
			@Min(1970) int year, 
			@Range(min=1, max=12) int month) {

		MonthRecap monthRecap = MonthRecap.find("byPersonAndYearAndMonth", person, year, month).first();
		if (monthRecap == null) {
			return new MonthRecap(person, year, month);
		}
		monthRecap.persistent = true;
		return monthRecap;
	}
	
	
	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<PersonDay> getDays() {
		if (days != null) {
			return days;
		}
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for (int day = 1; day <= firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {
			days.add(new PersonDay(person, new LocalDate(year, month, day)));
		}
		return days;
	}
	
	
}
