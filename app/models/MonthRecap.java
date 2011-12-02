package models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 *
 * Per adesso la classe Month recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista sia nella struttura che più banalmente nei nomi dei campi
 * 
 */
@Entity
public class MonthRecap extends Model {
	
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Column
	public short month;
	@Column
	public short year;
	@Column
	public short workingDays;
	@Column
	public short daysWorked;
	@Column
	public short giorniLavorativiLav;
	@Column
	public int workTime;
	@Column
	public int remaining;
	@Column
	public short justifiedAbsence;
	@Column
	public short vacationAp;
	@Column
	public short vacationAc;
	@Column
	public short holidaySop;
	@Column
	public int recoveries;
	@Column
	public short recoveriesG;
	@Column
	public short recoveriesAp;
	@Column
	public short recoveriesGap;
	@Column
	public int overtime;
	@Column
	public Timestamp lastModified;
	@Column
	public int residualApUsed;
	@Column
	public int extraTimeAdmin;
	@Column
	public int additionalHours;
	@Column
	public byte nadditionalHours;
	@Column
	public int residualFine;
	@Column
	public byte endWork;
	@Column
	public byte beginWork;
	@Column
	public int timeHourVisit;
	@Column
	public short endRecoveries;
	@Column
	public int negative;
	@Column
	public int endNegative;
	@Column
	public String progressive;
}
